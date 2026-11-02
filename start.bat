@echo off
title Warehouse System - One-Click Start
setlocal

set "CATALINA_HOME=D:\1software\javasoft\apache-tomcat-9.0.122"
rem %~dp0 = the folder this bat file lives in (avoids Chinese chars in this file)
set "CLIENT_JAR=%~dp0warehouse-client\target\warehouse-client.jar"

echo ============================================
echo    Warehouse System - Starting...
echo ============================================
echo.

:: 1) Check / start MySQL service
echo [1/3] Checking MySQL service...
sc query MySQL80 | find "RUNNING" >nul
if errorlevel 1 (
    echo     MySQL not running, starting...
    net start MySQL80
) else (
    echo     MySQL is running [OK]
)
echo.

:: 2) Start Tomcat in a new minimized window
echo [2/3] Starting Tomcat...
start "Tomcat-9.0.122" /MIN "%CATALINA_HOME%\bin\startup.bat"
echo     Waiting 8 seconds for server ready...
timeout /t 8 /nobreak >nul

:: Smoke test
curl -s -X POST http://localhost:8080/warehouse/api/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\"}" >nul
if errorlevel 1 (
    echo     [WARN] Server not responding, check Tomcat window log
) else (
    echo     Server is ready [OK]
)
echo.

:: 3) Start Swing client
echo [3/3] Starting client...
if not exist "%CLIENT_JAR%" (
    echo     [ERROR] Client jar not found: %CLIENT_JAR%
    echo     Run: mvn clean package in warehouse-client folder
    pause
    exit /b 1
)
start "WarehouseClient" javaw -jar "%CLIENT_JAR%"
echo     Client window opened [OK]
echo.
echo ============================================
echo    All started! Login: admin / admin
echo    Close Tomcat window = stop server
echo ============================================
echo.
pause
