# xrevent-broadcaster-android
The plugin source for the Unity XRevent Broadcaster


## Prerequisites
- Android Studio 2021.3.1


## Build the Unity Plugin
1. Clone the Project to your disk: 'git clone https://github.com/xrchisense/xrevent-broadcaster-android.git'
2. Open Project with Android Studio 'File > Open...', select root folder of project.
3. To build the Plugin select the 'BroadcasterPlugin', then 'Build > Make Module'
4. Find the .aar in 'xrevent-broadcaster-android\BroadcasterPlugin\build\outputs\aar'
5. Copy the BroadcasterPlugin-debug.aar to the Unity Project folder 'xrevent-broadcaster-unity\Assets\Plugins\Android'
6. Copy the required dependencies also into this folder.

## Use the Unity Plugin
1. Switch the Android Project Settings to:
- OpenGLES3
- Color Space Linear
- Multithreaded Rendering Un-checked
- Minimum API Level 25
- Scripting Backend IL2CPP
- Api Compatilility Level .NET Standard 2.0


