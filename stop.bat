@echo off
title Warehouse System - Stop
setlocal

set "CATALINA_HOME=D:\1software\javasoft\apache-tomcat-9.0.122"

echo ============================================
echo    Stopping Warehouse System...
echo ============================================
echo.

echo [1/2] Closing client window...
taskkill /FI "WINDOWTITLE eq WarehouseClient*" /T /F >nul 2>&1
echo     Done
echo.

echo [2/2] Stopping Tomcat...
call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>&1
timeout /t 3 /nobreak >nul
taskkill /FI "WINDOWTITLE eq Tomcat-9.0.122*" /T /F >nul 2>&1
echo     Done
echo.
echo ============================================
echo    All stopped
echo ============================================
pause
