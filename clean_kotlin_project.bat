@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem Run this BAT from the root of a Kotlin / Gradle / Maven / IntelliJ project.
set "ROOT=%~dp0"
cd /d "%ROOT%" || (
    echo [ERROR] Cannot open the project folder.
    pause
    exit /b 1
)

rem Never clean an arbitrary folder without a recognizable project marker.
if not exist "gradlew.bat" if not exist "settings.gradle" if not exist "settings.gradle.kts" if not exist "build.gradle" if not exist "build.gradle.kts" if not exist "pom.xml" if not exist ".idea\" (
    echo [ERROR] No Gradle, Maven or IntelliJ project detected here.
    echo Put this file in the project ROOT and run it again.
    pause
    exit /b 1
)

echo ========================================================
echo  Kotlin project cleaner - generated files ONLY
echo ========================================================
echo Project: "%ROOT%"
echo.
echo Removes: Gradle caches, Kotlin caches, build outputs,
echo          IntelliJ out, Android native build caches,
echo          and Maven target folders.
echo Keeps:   src, .git, .idea, gradle wrapper, project files,
echo          gradle.properties, local.properties, dependencies.
echo.
echo Close Android Studio / IntelliJ and stop builds first.
echo.
choice /C YN /N /M "Start cleanup? [Y/N]: "
if errorlevel 2 (
    echo Cancelled.
    exit /b 0
)

echo.
echo Scanning build modules...
call :SCAN "%ROOT%"

rem Root-level generated directories.
call :DELETE "%ROOT%build"
call :DELETE "%ROOT%.gradle"
call :DELETE "%ROOT%.kotlin"
call :DELETE "%ROOT%out"
call :DELETE "%ROOT%.cxx"
call :DELETE "%ROOT%.externalNativeBuild"

echo.
echo Cleanup finished. Reopen the IDE and sync/build the project.
pause
exit /b 0

:SCAN
setlocal
set "DIR=%~1"

rem Only delete a module's build/target if it contains a build descriptor.
if exist "%DIR%build.gradle"     call :DELETE "%DIR%build"
if exist "%DIR%build.gradle.kts" call :DELETE "%DIR%build"
if exist "%DIR%pom.xml"          call :DELETE "%DIR%target"

rem Skip source control, generated trees, bulky dependency trees and IDE data.
rem Also skip junctions/symlinks (reparse points), which can lead elsewhere.
for /d %%D in ("%DIR%*") do (
    if /I not "%%~nxD"==".git" if /I not "%%~nxD"==".gradle" if /I not "%%~nxD"==".kotlin" if /I not "%%~nxD"==".idea" if /I not "%%~nxD"=="build" if /I not "%%~nxD"=="target" if /I not "%%~nxD"=="out" if /I not "%%~nxD"=="node_modules" if /I not "%%~nxD"==".cxx" if /I not "%%~nxD"==".externalNativeBuild" (
        fsutil reparsepoint query "%%~fD" >nul 2>&1
        if errorlevel 1 call :SCAN "%%~fD\"
    )
)
endlocal
exit /b 0

:DELETE
if exist "%~1\" (
    echo Deleting "%~1"
    rmdir /S /Q "%~1"
    if exist "%~1\" echo [WARN] Could not fully remove "%~1"
)
exit /b 0
