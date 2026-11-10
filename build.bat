@echo off
setlocal EnableExtensions
REM Double click build.bat: optimized release APK (default).
REM From terminal: build.bat debug  -- unoptimized development APK.
cd /d "%~dp0"
if errorlevel 1 goto failed

set "MODE=%~1"
if "%MODE%"=="" set "MODE=release"
if /I not "%MODE%"=="release" if /I not "%MODE%"=="debug" (
    echo Usage: build.bat [release^|debug]
    goto failed
)

call :setup_env
if errorlevel 1 goto failed

echo.
if /I "%MODE%"=="release" (
    echo === DreamRecords - optimized RELEASE build ===
    echo R8 code shrinking + optimized resource shrinking enabled.
    call "%~dp0gradlew.bat" --console=plain assembleRelease
    if errorlevel 1 goto failed
    set "APK=app\build\outputs\apk\release\app-release.apk"
    set "OUT=DreamRecords-release.apk"
) else (
    echo === DreamRecords - DEBUG build ===
    call "%~dp0gradlew.bat" --console=plain assembleDebug
    if errorlevel 1 goto failed
    set "APK=app\build\outputs\apk\debug\app-debug.apk"
    set "OUT=DreamRecords-debug.apk"
)

if not exist "%APK%" (
    echo ERROR: Gradle finished, but no APK was found: %APK%
    goto failed
)
copy /Y "%APK%" "%OUT%" >nul
if errorlevel 1 goto failed

echo.
echo ================================================
echo SUCCESS! APK ready:
echo   %CD%\%OUT%
echo Original: %CD%\%APK%
for %%F in ("%OUT%") do echo APK size: %%~zF bytes
if /I "%MODE%"=="release" echo NOTICE: Release is signed with the DEBUG key for local installation only.
echo ================================================
echo.
if not "%DREAM_RECORDS_NO_PAUSE%"=="1" pause
exit /b 0

:failed
echo.
echo ================================================
echo BUILD FAILED! Read the error messages above.
echo ================================================
echo.
if not "%DREAM_RECORDS_NO_PAUSE%"=="1" pause
exit /b 1

:setup_env
REM Android Studio's JBR avoids incompatible system Java versions.
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if exist "%LocalAppData%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LocalAppData%\Programs\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

REM If local.properties exists, Gradle already knows the user's SDK path.
if exist "local.properties" exit /b 0
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME if exist "%LocalAppData%\Android\Sdk\platform-tools" set "ANDROID_HOME=%LocalAppData%\Android\Sdk"
if not defined ANDROID_HOME (
    echo ERROR: Android SDK not found.
    echo Install Android SDK in Android Studio or set ANDROID_HOME.
    exit /b 1
)
set "SDK_ESCAPED=%ANDROID_HOME:\=\\%"
>"local.properties" echo sdk.dir=%SDK_ESCAPED%
echo Android SDK: %ANDROID_HOME%
exit /b 0
