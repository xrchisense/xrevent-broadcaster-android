package com.xrchisense.xrevent.broadcasterplugin;

public interface UnityInterface {

        public void updateURL(String url);
        public void updateProgress(int progress );
        public void CanGoBack(boolean able);
        public void CanGoForward(boolean able);
        public void OnPageVisited(String url, String lastUrl);
        public void ChangeKeyboardVisiblity(boolean show);
        public void RestartInput();
        public void OnSessionCrash();
        public void OnRuntimeShutdown();
        public void OnFullScreenRequestChange(boolean fullScreen);

}
