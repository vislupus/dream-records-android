@echo off
REM Build + install + launch on the connected phone or running emulator (Windows).
cd /d %~dp0
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if exist "%LocalAppData%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LocalAppData%\Programs\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist local.properties (
  if not defined ANDROID_HOME if exist "%LocalAppData%\Android\Sdk\platform-tools" set "ANDROID_HOME=%LocalAppData%\Android\Sdk"
  if defined ANDROID_HOME (
    set "SDK_ESCAPED=%ANDROID_HOME:\=\\%"
    echo sdk.dir=%SDK_ESCAPED%> local.properties
  )
)
if exist "%LocalAppData%\Android\Sdk\platform-tools\adb.exe" set "PATH=%LocalAppData%\Android\Sdk\platform-tools;%PATH%"
call gradlew.bat --quiet installDebug || exit /b 1
adb shell am start -n com.vislupus.dream_records/.MainActivity
echo Dream Records installed and started
