@echo off
setlocal

title Gombeth Urban

set "ROOT=%~dp0"
set "BACKEND=%ROOT%backend"
set "FRONTEND=%ROOT%frontend"
set "CONFIG=%BACKEND%\config\application-local.properties"

echo =====================================
echo   INICIANDO GOMBETH URBAN
echo =====================================
echo.

if not exist "%CONFIG%" (
    echo ERROR:
    echo No existe la configuracion privada:
    echo.
    echo %CONFIG%
    echo.
    echo Cree el archivo a partir de:
    echo backend\src\main\resources\application-example.properties
    echo.
    pause
    exit /b 1
)

if not exist "%BACKEND%\mvnw.cmd" (
    echo ERROR: no se encuentra backend\mvnw.cmd
    pause
    exit /b 1
)

if not exist "%FRONTEND%\package.json" (
    echo ERROR: no se encuentra frontend\package.json
    pause
    exit /b 1
)

echo Arrancando BACKEND con perfiles prod,local...

start "Gombeth Urban - Backend" powershell -NoExit -NoProfile -Command ^
    "Set-Location '%BACKEND%'; .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=prod,local'"

timeout /t 10 /nobreak > nul

echo Arrancando FRONTEND...

start "Gombeth Urban - Frontend" powershell -NoExit -NoProfile -Command ^
    "Set-Location '%FRONTEND%'; npm start"

timeout /t 15 /nobreak > nul

echo Abriendo navegador...

start http://localhost:4200/

echo.
echo =====================================
echo Gombeth Urban iniciado
echo =====================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:4200
echo Health:   http://localhost:8080/api/health
echo.

endlocal