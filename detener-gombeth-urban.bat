@echo off

echo Cerrando procesos Java...
taskkill /F /IM java.exe

echo Cerrando procesos Node...
taskkill /F /IM node.exe

echo Aplicacion detenida.
pause