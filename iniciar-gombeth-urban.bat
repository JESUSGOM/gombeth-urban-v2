@echo off
setlocal

title Gombeth Urban

set "ROOT=%~dp0"
set "BACKEND=%ROOT%backend"
set "CONFIG=%BACKEND%\config\application-local.properties"
set "JAR=%BACKEND%\target\GombethUrban-1.0.0.jar"

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
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo ERROR:
    echo No existe el JAR ejecutable:
    echo.
    echo %JAR%
    echo.
    echo Compile primero Angular y el backend.
    echo.
    pause
    exit /b 1
)

echo Arrancando Gombeth Urban...

start "Gombeth Urban - Backend" powershell -NoExit -NoProfile -Command ^
    "Set-Location '%BACKEND%'; java -jar '.\target\GombethUrban-1.0.0.jar' --spring.profiles.active=prod,local"

timeout /t 15 /nobreak > nul

echo Abriendo navegador...

start http://localhost:8080/

echo.
echo =====================================
echo Gombeth Urban iniciado
echo =====================================
echo.
echo Aplicacion: http://localhost:8080/
echo Health:     http://localhost:8080/api/health
echo.

endlocal