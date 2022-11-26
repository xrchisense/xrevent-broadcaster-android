package com.xrchisense.xrevent.broadcasterplugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.widget.RelativeLayout;


import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoSession.PermissionDelegate;


import com.unity3d.player.UnityPlayer;

public class BroadcasterPlugin extends Fragment implements OnFrameAvailableListener, GeckoRuntime.Delegate, GeckoSession.NavigationDelegate, GeckoSession.ContentDelegate, GeckoSession.HistoryDelegate, GeckoSession.TextInputDelegate, GeckoSession.ProgressDelegate, GeckoSession.PermissionDelegate {
    static String FRAGMENT_TAG = "BroadcasterPluginFragment";
    static BroadcasterPlugin instance;

    private static int width;
    private static int height;
    static int defaultUserAgent = GeckoSessionSettings.USER_AGENT_MODE_MOBILE;

    private static GeckoRuntime mRuntime;
    private GeckoSession mSession;
    private UnityInterface UnityCallback;

    private SurfaceGeckoView mGeckoView;

    // Vars for the Texture Sampler
    private SurfaceTexture mSurfaceTexture; // <<-- This is the Surface on which the gecko image needs to go.
    private FilterFBOTexture mFilterFBOTexture;
    private boolean mIsUpdateFrame;

    // TestFunction for Unity. Should be removed.
    public static String getTextFromPlugin(int num){
        return "Number is " + num;
    }

    /*
	/ Functions called externally from XrEventBroadcaster.cs Unity script.
	/
	*/
    public static BroadcasterPlugin CreateInstance(int width, int height, String defaultUserAgent){
        Log.d("[ xrevent ]", "[ GeckoViewPlugin ] CreateInstance.");

        // Create new fragment
        BroadcasterPlugin pluginFragment = new BroadcasterPlugin();
        pluginFragment.width 			= width;
        pluginFragment.height 			= height;

        //pluginFragment.defaultUserAgent = defaultUserAgent;
        pluginFragment.setRetainInstance(true);

        AddFragment(pluginFragment);

        return pluginFragment;
    }



    public static void DebugMsg(String msg){
        Log.i("[ xrevent ]", "[ GeckoViewPlugin ]: " + msg);
    }

    public void SetUnityBitmapCallback(UnityInterface callback){
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] Setting Unity callback.");
        UnityCallback = callback;
    }

    public void LoadURL(final String url) {
        Log.d("[ xrevent ]", "[ GeckoViewPlugin ] LoadURL.");

        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mGeckoView == null || mGeckoView.getSession()==null) {
                Log.d("[ xrevent ]", "[ GeckoViewPlugin ] Session is null.");
                return;
            }
            mGeckoView.getSession().loadUri(url);

        }});
    }




    /**
     *  This Part is about the Texture sampling.
     *
     */
    public void initTextureSampler(int unityTextureId, int width, int height){
        Log.d("[ xrevent ]", "[ GeckoViewPlugin ] initTextureSampler.");

        int videoTextureId = FBOUtils.createOESTextureID();

        mSurfaceTexture = new SurfaceTexture(videoTextureId);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mFilterFBOTexture = new FilterFBOTexture(width, height, unityTextureId, videoTextureId);

        //Pass the new mSurfaceTexture to the SurfaceGeckoView to have it used for
        // drawing on. mSurface will get swapped for drawing in Display class.
        mGeckoView.passSurfaceTexture(mSurfaceTexture, width, height);
        Log.d("[ xrevent ]", "[ GeckoViewPlugin ] initTextureSampler DONE.");
    }

    /**
     *  Main GeckoView related Functions
     *
     */

    public void ActivateSession(boolean setActive){
        if (mGeckoView == null || mGeckoView.getSession()==null)
            return;

        mGeckoView.getSession().setActive(setActive);
    }


    /**
	 * Functions for creating the fragment and GeckoView
	 *
	 */
    @SuppressLint("ResourceType")
    private static void AddFragment(Fragment fragment){
        Log.d("[ xrevent ]", "[ GeckoViewPlugin ] AddFragment.");

        // Get the first leaf view (i.e. a view without children) of the current activity UnityPlayer.
        ViewGroup rootView = (ViewGroup)UnityPlayer.currentActivity.findViewById(android.R.id.content);
        View topMostView = getLeafView(rootView);

        if(topMostView != null){
            ViewGroup leafParent = (ViewGroup)topMostView.getParent();

            if(leafParent != null){
                //leafParent.setId(0x20348);
                leafParent.setId(0x20348);

                FragmentManager fragmentManager = UnityPlayer.currentActivity.getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.add(leafParent.getId(), fragment, FRAGMENT_TAG);
                fragmentTransaction.commit();

            }

        }

    }

    private static View getLeafView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                View chview = vg.getChildAt(i);
                View result = getLeafView(chview);
                if (result != null)
                    return result;
            }
            return null;
        }
        else {
            return view;
        }
    }

    public void LoadUrlIfUnopened(String firstURL){
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            GeckoSession currentSession = mGeckoView.getSession();
            if (currentSession != null) {
                mGeckoView.releaseSession();
                currentSession.setActive(false);
            }

            GeckoSession newSession = mSession;//ParseSessionForType(sessionType);

            boolean firstOpen = !newSession.isOpen();
            if (firstOpen) {
                newSession.open(mRuntime);
            }

            newSession.setActive(true);
            mGeckoView.setSession(newSession, mRuntime);
            if (firstOpen)
                newSession.loadUri(firstURL);

        });
    }


    // Initialize the actual GeckoView on a relativeLayout resource
    void initViews(View view){
        // useful for adding keys from unity to the webview
       //TOB CharMap = KeyCharacterMap.load(KeyCharacterMap.ALPHA);

        RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.relativeLayout);

        // Create the GeckoView
        mGeckoView = new SurfaceGeckoView(view.getContext());

        // Set the webview parameters to 1x1 so on startup the user doesn't see a big white rectangle
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams( 1,1);//Width/100,  Height/100);
        mGeckoView.setLayoutParams(layoutParams);
        relativeLayout.addView (mGeckoView);
        mGeckoView.setVisibility(View.VISIBLE);

        Log.d("[ xrevent ]","[ GeckoViewPlugin ] Creating Runtime.");
        InitNewRuntime(view);

        /* TOB
        Log.i(LOG_TAG, "CREATING sessions hashmap");
        mSessions = new HashMap<SessionTypes, GeckoSession>() {{
            put(SessionTypes.BROWSER, InitNewSession(mRuntime));
            put(SessionTypes.YOUTUBE, InitNewSession(mRuntime));
        }};
        */
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] Creating New Session.");
        mSession = InitNewSession(mRuntime);
    }

    /*
     *  GeckoView engine related functions
     *  ToDo: Needs androidx updating for higher API levels than 28
     */
    private void InitNewRuntime(View view){
        if(mRuntime != null) return;
        GeckoRuntimeSettings.Builder runtimeSettings = new GeckoRuntimeSettings.Builder();
        runtimeSettings.inputAutoZoomEnabled(false);
        mRuntime = GeckoRuntime.create(view.getContext(), runtimeSettings.build());
        mRuntime.setDelegate(this);
    }

    private GeckoSession InitNewSession(GeckoRuntime runtime){
        GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder();
//        builder.useMultiprocess(false);
        builder.suspendMediaWhenInactive(true);
        builder.userAgentMode(0); // USER_AGENT_MODE_MOBILE
        builder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        builder.displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE);
        GeckoSession session = new GeckoSession(builder.build());
        session.setNavigationDelegate(this);
        session.setContentDelegate(this);
        session.setHistoryDelegate(this);
        session.getTextInput().setDelegate(this);
        session.setProgressDelegate(this);
        session.setPermissionDelegate(this);
        return session;
    }

    /*
     * Fragment creation Callbacks
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] onCreate.");
        // register listener for download complete status
//        getContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        setRetainInstance(true); // Retain between configuration changes (like device rotation)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] onCreateView() Callback of Fragment creation.");
//        WebView.enableSlowWholeDocumentDraw();
//        BitmapWebView.enableSlowWholeDocumentDraw();

        // can use this to not show window
//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.ADJUST_NOTHING|     WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        View mView = inflater.inflate(R.layout.activity_main, parent, false);
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] Inflating with View started.");
        instance = this;
        initViews(mView);
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] Finished onCreateView().");

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        Log.d("[ xrevent ]","[ GeckoViewPlugin ] onViewCreated.");
    }

    /*
     *  For the Texture Sampler
     *
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mIsUpdateFrame = true;
    }

    public void updateTexture() {
        Log.d("[ xrevent ]", "[ BroadcasterPlugin ] updateTexture.");
        mIsUpdateFrame = false;
        mSurfaceTexture.updateTexImage();
        mFilterFBOTexture.draw();
    }

    public boolean isUpdateFrame() {
        //Log.d("[ xrevent ]", "[ BroadcasterPlugin ] isUpdateFrame(): " + mIsUpdateFrame);
        return mIsUpdateFrame;
    }

    /*
     * Gecko Engine Callbacks
     */
    @Override
    public void onShutdown() {
        Log.i("[ xrevent ]","[ GeckoViewPlugin ] onShutdown: Runtime has been shutdown.");
        //UnityCallback.OnRuntimeShutdown();
    }


    @Override
    public void onAndroidPermissionsRequest(final GeckoSession session, final String[] permissions, final Callback callback) {
        if (Build.VERSION.SDK_INT >= 23) {
            // requestPermissions was introduced in API 23.
           // mCallback = callback;
            requestPermissions(permissions, PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE);
        } else {
            callback.grant();
        }
    }

    @Override
    public void onContentPermissionRequest(final GeckoSession session, final String uri, final int type, final Callback callback) {
        final int resId;
        Callback contentPermissionCallback = callback;

        if (PERMISSION_AUTOPLAY_AUDIBLE == type || PERMISSION_AUTOPLAY_INAUDIBLE == type) {
            Log.i("[ xrevent ]","[ GeckoViewPlugin ] Allow autoplay request");
            callback.grant();
            return;
        }

       // https://bugzilla.mozilla.org/show_bug.cgi?id=1614894
       // final String title = getString(resId, Uri.parse(uri).getAuthority());
       // final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)mTabSessionManager.getCurrentSession().getPromptDelegate();
       // prompt.onPermissionPrompt(session, title, contentPermissionCallback);
    }
}
