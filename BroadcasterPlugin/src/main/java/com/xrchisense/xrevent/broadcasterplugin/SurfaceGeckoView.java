package com.xrchisense.xrevent.broadcasterplugin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.gecko.util.ActivityUtils;
import org.mozilla.geckoview.BasicSelectionActionDelegate;
import org.mozilla.geckoview.GeckoDisplay;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;


public class SurfaceGeckoView extends GeckoView {
    // The replacement Display of GeckoView. The original is protected
    // This is why it is implemented here.
    protected final Display mDisplay = new Display();

    private GeckoSession.SelectionActionDelegate mSelectionActionDelegate;
    private Surface mSurface;
    private SurfaceView mSurfaceView;
    private SurfaceView pubSurfaceView;
    private int mWidth;
    private int mHeight;

    boolean IS_APPLICATION;

    public SurfaceGeckoView(Context context) {
        super(context);
        init();
    }

    private void init(int... intPointer) {
        CheckIfWeAreApplication(); // <<-- May not be needed anymore.
        setFocusable(false);
        setFocusableInTouchMode(false);
        setWillNotCacheDrawing(false);

        if (IS_APPLICATION)
            SetupApplicationSurfaceView();


        final Activity activity = ActivityUtils.getActivityFromContext(getContext());
        if (activity != null) {
            boolean usingFloatingToolbar = true;
            Log.d("[ xrevent ]", "[ SurfaceGeckoView ] Activity is not null, setting basic selection delegate and using floating bar: " + usingFloatingToolbar );
            mSelectionActionDelegate = new BasicSelectionActionDelegate(activity, usingFloatingToolbar);
        }

        // We can't call OnSurfaceTextureAvailable from the GL thread
        // Instead, initGLTextures() will set our surface texture at some point,
        // poll until it happens
        if (!IS_APPLICATION) {
            final Handler handler = new Handler();
            final int delay = 100; //milliseconds
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (mSurface != null) {
                        mDisplay.DisplaySurfaceAvailable(mSurface, mWidth, mHeight);

                    } else {
                        handler.postDelayed(this, delay);
                    }
                }
            }, delay);
        }
    }

    // TODO: Requires we comment out BasicActivity to work
    private void CheckIfWeAreApplication(){
        try {
            Class.forName( "com.xrchisense.xrevent.broadcasterplugin.BasicActivity" );
            Log.d("[ xrevent ]", "[ SurfaceGeckoView ] BasicActivity found! We are an application");
            IS_APPLICATION = true;
        } catch( ClassNotFoundException e ) {
            Log.d("[ xrevent ]", "[ SurfaceGeckoView ] BasicActivity not found! We are a library and not an application");
            IS_APPLICATION = false;
        }
    }

    private void SetupApplicationSurfaceView(){
        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        // We are adding descendants to this LayerView, but we don't want the
        // descendants to affect the way LayerView retains its focus.
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        // This will stop PropertyAnimator from creating a drawing cache (i.e. a
        // bitmap) from a SurfaceView, which is just not possible (the bitmap will be
        // transparent).
        setWillNotCacheDrawing(false);

        mSurfaceView = new SurfaceView(getContext());

        mSurfaceView.setBackgroundColor(Color.WHITE);
        addView(mSurfaceView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        mSurfaceView.getHolder().addCallback(mDisplay);
        pubSurfaceView = mSurfaceView;
    }


    public void passSurfaceTexture(SurfaceTexture surfaceTexture, int width, int height){
        Log.d("[ xrevent ]", "[ SurfaceGeckoView ] Creating new Surface from Texture2D.");
        mSurface = new Surface(surfaceTexture);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Attach a session to this view. The session should be opened before
     * attaching or a runtime needs to be provided for automatic opening.
     *
     * @param session The session to be attached.
     * @param runtime The runtime to be used for opening the session.
     */
    public void setSession(@NonNull final GeckoSession session,
                           @Nullable final GeckoRuntime runtime) {
        if (mSession != null && mSession.isOpen()) {
            throw new IllegalStateException("Current session is open");
        }
        Log.d("[ xrevent ]", "[ SurfaceGeckoView ] setSession.");
        releaseSession();

        mSession = session;
//        mRuntime = runtime;

//        if (session.isOpen()) {
//            if (runtime != null && runtime != session.getRuntime()) {
//                throw new IllegalArgumentException("Session was opened with non-matching runtime");
//            }
//            mRuntime = session.getRuntime();
//        } else if (runtime == null) {
//            throw new IllegalArgumentException("Session must be open before attaching");
//        }
        if  ( !session.isOpen() && runtime == null) {
            throw new IllegalArgumentException("Session must be open before attaching");
        }

        // session creates a geckodisplay
        mDisplay.acquire( session.acquireDisplay());

        final Context context = getContext();
        session.getOverscrollEdgeEffect().setTheme(context);
        session.getOverscrollEdgeEffect().setInvalidationCallback(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 16) {
                    SurfaceGeckoView.this.postInvalidateOnAnimation();
                } else {
                    SurfaceGeckoView.this.postInvalidateDelayed(10);
                }
            }
        });

        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight,
                outValue, true)) {
            session.getPanZoomController().setScrollFactor(outValue.getDimension(metrics));
        } else {
            session.getPanZoomController().setScrollFactor(0.075f * metrics.densityDpi);
        }

        session.getCompositorController().setFirstPaintCallback(new Runnable() {
            @Override
            public void run() {
                coverUntilFirstPaint(Color.TRANSPARENT);
            }
        });

        if (session.getTextInput().getView() == null) {
            session.getTextInput().setView(this);
        }

        if (session.getAccessibility().getView() == null) {
            session.getAccessibility().setView(this);
        }

        if (session.getSelectionActionDelegate() == null && mSelectionActionDelegate != null) {
            session.setSelectionActionDelegate(mSelectionActionDelegate);
        }

        if (isFocused()) {
            session.setFocused(true);
        }
    }


    public class Display  implements SurfaceHolder.Callback {
        private final int[] mOrigin = new int[2];

        private GeckoDisplay mDisplay;
        private boolean mValid;

        public void acquire(final GeckoDisplay display) {
            Log.d("[ xrevent ]", "[ SurfaceGeckoView ] Acquiring display.");
            mDisplay = display;

            if (!mValid) {
                return;
            }

            // Tell display there is already a surface.
            onGlobalLayout();
            if (SurfaceGeckoView.this.mSurface != null) {
//                final SurfaceHolder holder = SurfaceTextureGeckoView.this.mTextureView.getHolder();
//                final Rect frame = holder.getSurfaceFrame();
                mDisplay.surfaceChanged(mSurface, mWidth, mHeight); // <-- new Surface(mSurfaceTexture) from BCP
                SurfaceGeckoView.this.setActive(true);
            }
        }

        public GeckoDisplay release() {
            if (mValid) {
                mDisplay.surfaceDestroyed();
                SurfaceGeckoView.this.setActive(false);
            }

            final GeckoDisplay display = mDisplay;
            mDisplay = null;
            return display;
        }

        public void onGlobalLayout() {
            if (mDisplay == null) {
                return;
            }
            if (SurfaceGeckoView.this.mSurface != null) {
                mOrigin[0] = 0;
                mOrigin[1] = mHeight;
//                SurfaceTextureGeckoView.this.mTextureView.getLocationOnScreen(mOrigin);
                Log.d("[ xrevent ]", "[ SurfaceGeckoView ] onGlobalLayout(): " + mOrigin[0]+" "+ mOrigin[1] );
                mDisplay.screenOriginChanged(mOrigin[0], mOrigin[1]);
            }
        }

        public boolean shouldPinOnScreen() {
            return mDisplay != null ? mDisplay.shouldPinOnScreen() : false;
        }

        public void ChangeScreenOrigin(int left, int top){
            mDisplay.screenOriginChanged(left, top);

        }

        // We can create a TextureView here for debugging purposes, though now unnecessary
        // We can also set out display surface for debugging + making sure we're drawing to it
        public void DisplaySurfaceAvailable(Surface surface, int width, int height) {
            mSurface = surface;
            Log.d("[ xrevent ]", "[ SurfaceGeckoView ] DisplaySurfaceAvailable: " + width + " " + height);

            mWidth = width;
            mHeight = height;
            if (mDisplay != null) {

                mDisplay.surfaceChanged(mSurface, width, height);
                if (!mValid) {
                    SurfaceGeckoView.this.setActive(true);
                }
                mDisplay.shouldPinOnScreen();
            }
            mValid = true;
        }

        /**
         * Old interface for when GeckoView used a SurfaceHolder to handle its surface
         */
        @Override // SurfaceHolder.Callback
        public void surfaceCreated(final SurfaceHolder holder) {
        }

        @Override // SurfaceHolder.Callback
        public void surfaceChanged(final SurfaceHolder holder, final int format,
                                   final int width, final int height) {
            if (mDisplay != null) {
                mDisplay.surfaceChanged(holder.getSurface(), width, height);
                if (!mValid) {
                    SurfaceGeckoView.this.setActive(true);
                }
            }
            mValid = true;
        }

        @Override // SurfaceHolder.Callback
        public void surfaceDestroyed(final SurfaceHolder holder) {
            if (mDisplay != null) {
                mDisplay.surfaceDestroyed();
                SurfaceGeckoView.this.setActive(false);
            }
            mValid = false;
        }
    }


    /**
     * The following methods are GECKOVIEW required methods
     * b/c they were protected or we need to supply our surface
     */
    private void setActive(final boolean active) {
        if (mSession != null) {
            mSession.setActive(active);
        }
    }

}
