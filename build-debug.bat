@echo off
REM Double-click to build debug APK.
call "%~dp0build.bat" debug
exit /b %ERRORLEVEL%
