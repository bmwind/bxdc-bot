@echo off
cd /d "%~dp0"

echo Stopping BXDC-bot services ...

for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":18080.*LISTENING"') do taskkill /f /t /pid %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":3000.*LISTENING"')  do taskkill /f /t /pid %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173.*LISTENING"')  do taskkill /f /t /pid %%a 2>nul

echo Done.
timeout /t 2 >nul
