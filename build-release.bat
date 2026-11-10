@echo off
REM Double-click to build release APK.
call "%~dp0build.bat" release
exit /b %ERRORLEVEL%
