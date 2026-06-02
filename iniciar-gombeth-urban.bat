@echo off
title Gombeth Urban

echo =====================================
echo   INICIANDO GOMBETH URBAN
echo =====================================
echo.

echo Arrancando BACKEND...
start "Gombeth Backend" powershell -NoExit -Command "Set-Location 'W:\proyectos\gombeth-urban-v2\backend'; .\mvnw.cmd spring-boot:run"

timeout /t 10 > nul

echo Arrancando FRONTEND...
start "Gombeth Frontend" powershell -NoExit -Command "Set-Location 'W:\proyectos\gombeth-urban-v2\frontend'; ng serve"

timeout /t 15 > nul

echo Abriendo navegador...
start http://localhost:4200/comunidades

echo.
echo =====================================
echo Gombeth Urban iniciado
echo =====================================
pause