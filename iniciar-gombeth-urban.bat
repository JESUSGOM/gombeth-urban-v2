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
    pause
    exit /b 1
)

powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue){exit 1}else{exit 0}"

if errorlevel 1 (
    echo ERROR:
    echo El puerto 8080 ya esta ocupado.
    echo.
    echo Ejecute primero detener-gombeth-urban.bat
    echo.
    powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess | Format-Table -AutoSize"
    pause
    exit /b 1
)

echo Arrancando el JAR con perfiles prod,local...
echo.

start "Gombeth Urban - Backend" powershell -NoExit -NoProfile -Command "Set-Location '%BACKEND%'; java -jar '.\target\GombethUrban-1.0.0.jar' --spring.profiles.active=prod,local"

echo Esperando a que el backend este disponible...

powershell -NoProfile -ExecutionPolicy Bypass -Command "$limite=(Get-Date).AddSeconds(120);do{Start-Sleep -Seconds 2;try{$respuesta=Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 3;if($respuesta.status -eq 'OK'){exit 0}}catch{}}while((Get-Date)-lt $limite);exit 1"

if errorlevel 1 (
    echo.
    echo ERROR:
    echo El backend no ha respondido correctamente en 120 segundos.
    echo Revise la terminal del backend.
    echo.
    pause
    exit /b 1
)

echo.
echo Backend iniciado correctamente.
echo Abriendo Gombeth Urban...
echo.

start http://localhost:8080/

echo =====================================
echo Gombeth Urban iniciado
echo =====================================
echo.
echo Aplicacion: http://localhost:8080/
echo Health:     http://localhost:8080/api/health
echo.

endlocal