@echo off
cd /d "%~dp0"
set "DREAM_RECORDS_NO_PAUSE=1"
call "%~dp0build.bat" release > "log.txt" 2>&1
if errorlevel 1 (
  echo Build failed. See log.txt in the project folder.
) else (
  echo Release APK ready. See DreamRecords-release.apk
  echo Detailed log: log.txt
)
pause
