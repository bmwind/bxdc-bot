@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================
echo   BXDC-bot start
echo ========================================

REM ---- stop old processes ----
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":18080.*LISTENING"') do taskkill /f /pid %%a 2^>nul >nul
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":3000.*LISTENING"')  do taskkill /f /pid %%a 2^>nul >nul
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173.*LISTENING"')  do taskkill /f /pid %%a 2^>nul >nul
timeout /t 2 /nobreak >nul

REM ---- find mvn ----
set "MVN="
if exist "backend\skill-gateway\apache-maven-3.9.6\bin\mvn.cmd" set "MVN=backend\skill-gateway\apache-maven-3.9.6\bin\mvn.cmd"
if not defined MVN if exist "backend\apache-maven-3.9.6\bin\mvn.cmd" set "MVN=backend\apache-maven-3.9.6\bin\mvn.cmd"
if not defined MVN for /f "delims=" %%a in ('where mvn.cmd 2^>nul') do if not defined MVN set "MVN=%%a"
if not defined MVN (
    echo [ERROR] mvn.cmd not found
    pause
    exit /b 1
)
echo [INFO] mvn: %MVN%

REM ---- 1. skill-gateway (hidden) ----
echo [1/3] Skill Gateway (port 18080)
powershell -NoProfile -Command "Start-Process -FilePath '%MVN%' -ArgumentList '-s','settings.xml','clean','spring-boot:run','-Dmaven.test.skip=true' -WorkingDirectory 'backend\skill-gateway' -WindowStyle Hidden"

REM ---- 2. agent-core (build dist first, then start hidden) ----
echo [2/3] Agent Core - building dist ...
cd /d "%~dp0backend\agent-core"
if exist "dist" rd /s /q "dist"
call node_modules\.bin\tsc.cmd -p tsconfig.json --module commonjs 2>nul
echo [2/3] Agent Core (port 3000)
powershell -NoProfile -Command "Start-Process -FilePath 'npm.cmd' -ArgumentList 'run','start:dev' -WorkingDirectory '%~dp0backend\agent-core' -WindowStyle Hidden"
cd /d "%~dp0"

REM ---- 3. frontend (hidden) ----
echo [3/3] Frontend (port 5173)
powershell -NoProfile -Command "Start-Process -FilePath 'npm.cmd' -ArgumentList 'run','dev' -WorkingDirectory '%~dp0frontend' -WindowStyle Hidden"

REM ---- wait for ports ----
echo.
echo Waiting for ports (max 120s) ...
set /a waited=0
:waitloop
set "ok=1"
netstat -ano 2>nul | findstr ":18080.*LISTENING" >nul || set "ok=0"
netstat -ano 2>nul | findstr ":3000.*LISTENING"  >nul || set "ok=0"
netstat -ano 2>nul | findstr ":5173.*LISTENING"  >nul || set "ok=0"
if !ok!==1 goto ready
set /a waited+=3
if !waited! geq 120 goto timeout
timeout /t 3 /nobreak >nul
goto waitloop

:ready
echo.
echo ========================================
echo   All services ready
echo   Skill Gateway: http://localhost:18080
echo   Agent Core:    http://localhost:3000
echo   Frontend:      http://localhost:5173
echo   Run stop.bat to stop all services
echo ========================================
echo.
pause
exit /b 0

:timeout
echo.
echo [WARN] some services not ready after 120s
echo Check if skill-gateway maven is still compiling.
pause
exit /b 1
