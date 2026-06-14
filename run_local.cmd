@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "LOG_DIR=%ROOT%logs"
set "PID_DIR=%ROOT%.run"
set "JAVA_EXE=java"

if /I "%~1"=="stop" goto :stop
if /I "%~1"=="status" goto :status
if /I "%~1"=="clean" goto :clean
if /I "%~1"=="help" goto :help
if /I "%~1"=="--help" goto :help
if /I "%~1"=="/?" goto :help

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%PID_DIR%" mkdir "%PID_DIR%"

echo Building Spring eShop...
pushd "%ROOT%"
call mvn -DskipTests package
if errorlevel 1 (
  popd
  echo Build failed. Fix the Maven errors above and run this script again.
  exit /b 1
)
popd

echo.
echo Starting services...
call :start_module identity-api 5105
call :start_module catalog-api 5101
call :start_module basket-api 5103
call :start_module ordering-api 5102
call :start_module ordering-backgroundtasks 5111
call :start_module ordering-notifications 5112
call :start_module marketing-api 5110
call :start_module locations-api 5109
call :start_module payment-api 5108
call :start_module webhooks-api 5113
call :start_module shopping-aggregator 5121
call :start_module webmvc 5100
call :start_module webspa 5104
call :start_module webstatus 5107
call :start_module webhook-client 5114

echo.
echo Started local Spring eShop services.
echo Logs: %LOG_DIR%
echo PID files: %PID_DIR%
echo.
echo Useful URLs:
echo   Web MVC:        http://localhost:5100/
echo   Web SPA:        http://localhost:5104/
echo   Web Status:     http://localhost:5107/
echo   Identity:       http://localhost:5105/
echo   Catalog API:    http://localhost:5101/api/v1/catalog/items
echo   Basket API:     http://localhost:5103/api/v1/basket/demo
echo   Ordering API:   http://localhost:5102/api/v1/orders
echo   Marketing API:  http://localhost:5110/api/v1/campaigns
echo   Locations API:  http://localhost:5109/api/v1/locations
echo   Webhooks API:   http://localhost:5113/api/v1/webhooks
echo.
echo To stop services: run_local.cmd stop
exit /b 0

:start_module
set "MODULE=%~1"
set "PORT=%~2"
set "JAR=%ROOT%%MODULE%\target\%MODULE%-0.1.0-SNAPSHOT.jar"
set "OUT_LOG=%LOG_DIR%\%MODULE%.out.log"
set "ERR_LOG=%LOG_DIR%\%MODULE%.err.log"
set "PID_FILE=%PID_DIR%\%MODULE%.pid"

if not exist "%JAR%" (
  echo   [missing] %MODULE% jar not found: %JAR%
  exit /b 1
)

if exist "%PID_FILE%" (
  set /p EXISTING_PID=<"%PID_FILE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-Process -Id !EXISTING_PID! -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>nul
  if not errorlevel 1 (
    echo   [running] %MODULE% already has PID !EXISTING_PID!
    exit /b 0
  )
  del "%PID_FILE%" >nul 2>nul
)

echo   [start] %MODULE% on port %PORT%
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$jar = '%JAR%';" ^
  "$out = '%OUT_LOG%';" ^
  "$err = '%ERR_LOG%';" ^
  "$pidFile = '%PID_FILE%';" ^
  "$port = '%PORT%';" ^
  "$p = Start-Process -FilePath '%JAVA_EXE%' -ArgumentList @('-DPORT=' + $port, '-jar', $jar) -WorkingDirectory '%ROOT%' -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden -PassThru;" ^
  "$p.Id | Set-Content -Encoding ascii $pidFile"
if errorlevel 1 (
  echo   [failed] %MODULE% could not be started. See %ERR_LOG%
  exit /b 1
)
exit /b 0

:stop
if not exist "%PID_DIR%" (
  echo No PID directory found. Nothing to stop.
  exit /b 0
)
echo Stopping local Spring eShop services...
for %%F in ("%PID_DIR%\*.pid") do (
  set "PID="
  set /p PID=<"%%F"
  if defined PID (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Get-Process -Id !PID! -ErrorAction SilentlyContinue; if ($p) { Stop-Process -Id !PID! -Force }" >nul 2>nul
    echo   [stop] %%~nF PID !PID!
  )
  del "%%F" >nul 2>nul
)
exit /b 0

:status
if not exist "%PID_DIR%" (
  echo No PID directory found. Services are not running from this script.
  exit /b 0
)
echo Local Spring eShop service status:
for %%F in ("%PID_DIR%\*.pid") do (
  set "PID="
  set /p PID=<"%%F"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-Process -Id !PID! -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>nul
  if errorlevel 1 (
    echo   [stopped] %%~nF PID !PID!
  ) else (
    echo   [running] %%~nF PID !PID!
  )
)
exit /b 0

:clean
call "%~f0" stop
if exist "%LOG_DIR%" rmdir /s /q "%LOG_DIR%"
if exist "%PID_DIR%" rmdir /s /q "%PID_DIR%"
echo Removed local run logs and PID files.
exit /b 0

:help
echo Usage:
echo   run_local.cmd          Build and start all local Spring Boot services
echo   run_local.cmd status   Show service process status
echo   run_local.cmd stop     Stop services started by this script
echo   run_local.cmd clean    Stop services and remove local logs/PID files
exit /b 0
