@echo off
setlocal
chcp 65001 >nul
title Gym Management System
set "ROOT=%~dp0"
cd /d "%ROOT%"

rem --- already running? ---
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 goto OPEN

echo Starting Gym Management System ...
start "" javaw -Dfile.encoding=UTF-8 -jar "%ROOT%gym-management\target\gym-management.jar" --server.port=8080

set /a tries=0
:WAIT
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 goto OPEN
set /a tries+=1
if %tries% GEQ 45 goto TIMEOUT
timeout /t 2 /nobreak >nul
goto WAIT

:OPEN
start "" http://127.0.0.1:8080/
echo Opened: http://127.0.0.1:8080/
goto END

:TIMEOUT
echo Startup timeout. Check tools\\server-run.log or run the jar manually.
pause

:END
endlocal
