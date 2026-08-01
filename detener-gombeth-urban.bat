@echo off
setlocal

title Gombeth Urban - Detener

echo =====================================
echo   DETENIENDO GOMBETH URBAN
echo =====================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ports=@(8080,4200);$detenidos=@();foreach($port in $ports){$processIds=Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique;foreach($processId in $processIds){try{Stop-Process -Id $processId -Force -ErrorAction Stop;$detenidos+=('Puerto '+$port+' - PID '+$processId)}catch{Write-Host ('ERROR al detener PID '+$processId+': '+$_.Exception.Message);exit 1}}};Start-Sleep -Seconds 2;$restantes=@();foreach($port in $ports){$restantes+=Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue};if($restantes.Count -gt 0){Write-Host 'ERROR: quedan procesos escuchando.';$restantes | Format-Table LocalAddress,LocalPort,OwningProcess -AutoSize;exit 1};if($detenidos.Count -eq 0){Write-Host 'No habia procesos de Gombeth Urban en ejecucion.'}else{Write-Host 'Procesos detenidos correctamente:';$detenidos | ForEach-Object{Write-Host (' - '+$_)}}"

if errorlevel 1 (
    echo.
    echo No se pudo detener completamente Gombeth Urban.
    pause
    exit /b 1
)

echo.
echo Gombeth Urban detenido correctamente.
echo.

timeout /t 3 /nobreak > nul

endlocal