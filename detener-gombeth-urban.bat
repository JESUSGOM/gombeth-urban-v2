@echo off
setlocal

title Gombeth Urban - Detener

echo Deteniendo el backend de Gombeth Urban...

for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "$p = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess; if ($p) { Write-Output $p }"`) do (
    taskkill /PID %%P /T /F > nul 2>&1
)

echo Deteniendo el frontend de desarrollo, si estuviera activo...

for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "$p = Get-NetTCPConnection -LocalPort 4200 -State Listen -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess; if ($p) { Write-Output $p }"`) do (
    taskkill /PID %%P /T /F > nul 2>&1
)

echo.
echo Gombeth Urban detenido.
echo.

endlocal