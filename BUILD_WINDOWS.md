# Building Dream Records on Windows

## Requirements

Install Android Studio with the Android SDK and JDK, then download Android SDK Platform 36 from the SDK Manager. An internet connection may be needed for the initial Gradle dependency download.

## Build an APK

1. Clone or extract the complete project to a local folder.
2. Double-click `build.bat` or `build-release.bat` to create an optimized release-variant APK for evaluation.
3. Read the build result in the terminal window. On success, `DreamRecords-release.apk` is copied into the project root; the Gradle output remains in `app/build/outputs/apk/release/`.
4. To build the debug variant, use `build-debug.bat` instead.

The release variant enables R8 code shrinking and resource shrinking, but it **currently uses the Android debug signing certificate**. This is suitable for personal testing, not a production signing strategy.

For command-line builds:

```bat
gradlew.bat assembleDebug
gradlew.bat assembleRelease
```

## Install and run

Use `run.bat` with a connected Android device and USB debugging enabled. Alternatively, open the project in Android Studio, choose an Android 10+ device or emulator, and select **Run**.

If the Android SDK is not detected, configure `ANDROID_HOME` or open the project in Android Studio and allow it to generate `local.properties`. This file is machine-specific and should not be committed.

## Before sharing a public release

Use a securely managed dedicated release signing key. Back up important recordings before switching signing keys, because Android cannot install a differently signed APK as an update to an existing installation.
