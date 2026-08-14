param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Release = Join-Path $Root "release"
$ApplicationFolder = Join-Path $Release "cliente\GombethUrban"

if (-not (Test-Path $ApplicationFolder)) {
    throw "No existe la aplicacion generada: $ApplicationFolder"
}

$ConfigFolder = Join-Path $ApplicationFolder "config"
$LogsFolder = Join-Path $ApplicationFolder "logs"
$DataFolder = Join-Path $ApplicationFolder "datos"

New-Item -Path $ConfigFolder -ItemType Directory -Force | Out-Null
New-Item -Path $LogsFolder -ItemType Directory -Force | Out-Null
New-Item -Path $DataFolder -ItemType Directory -Force | Out-Null

$ConfigText = @(
    "# ============================================================",
    "# GOMBETH URBAN - CLIENT CONFIGURATION",
    "# Do not upload this file to Git.",
    "# ============================================================",
    "",
    "spring.profiles.active=prod",
    "",
    "server.address=127.0.0.1",
    "server.port=8080",
    "",
    "spring.datasource.url=jdbc:mysql://mysql-8001.dinaserver.com:3306/sepa_1914?useSSL=true&serverTimezone=Europe/Madrid",
    "spring.datasource.username=CHANGE_USER",
    "spring.datasource.password=CHANGE_PASSWORD",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "",
    "spring.jpa.hibernate.ddl-auto=none",
    "",
    "gombeth.production.database-name=sepa_1914",
    "gombeth.production.confirmed=true",
    "",
    "logging.file.name=./logs/gombeth-urban.log",
    "logging.level.root=INFO"
) -join [Environment]::NewLine

Set-Content -LiteralPath (Join-Path $ConfigFolder "application.properties") -Value $ConfigText -Encoding ASCII

$StartText = @(
    "@echo off",
    "setlocal",
    "cd /d \"%~dp0\"",
    "",
    "if not exist \"config\application.properties\" (",
    "    echo Missing config\application.properties",
    "    pause",
    "    exit /b 1",
    ")",
    "",
    "start \"Gombeth Urban\" /min \"GombethUrban.exe\" --spring.config.additional-location=file:./config/",
    "",
    "echo Starting Gombeth Urban...",
    "timeout /t 8 /nobreak >nul",
    "start \"\" \"http://127.0.0.1:8080\"",
    "",
    "endlocal"
) -join [Environment]::NewLine

Set-Content -LiteralPath (Join-Path $ApplicationFolder "Iniciar Gombeth Urban.bat") -Value $StartText -Encoding ASCII

$StopText = @(
    "@echo off",
    "setlocal EnableDelayedExpansion",
    "",
    "set FOUND=0",
    "",
    "for /f \"tokens=5\" %%P in ('netstat -ano ^| findstr \":8080\" ^| findstr \"LISTENING\"') do (",
    "    set FOUND=1",
    "    echo Stopping Gombeth Urban. PID %%P",
    "    taskkill /PID %%P /F >nul 2>&1",
    ")",
    "",
    "if \"!FOUND!\"==\"0\" (",
    "    echo Gombeth Urban was not listening on port 8080.",
    ") else (",
    "    echo Gombeth Urban was stopped.",
    ")",
    "",
    "timeout /t 2 /nobreak >nul",
    "endlocal"
) -join [Environment]::NewLine

Set-Content -LiteralPath (Join-Path $ApplicationFolder "Detener Gombeth Urban.bat") -Value $StopText -Encoding ASCII

$ReadmeText = @(
    "GOMBETH URBAN - PORTABLE INSTALLATION",
    "Version $Version",
    "",
    "1. Open config\application.properties.",
    "2. Replace CHANGE_USER and CHANGE_PASSWORD.",
    "3. Do not change the sepa_1914 database name.",
    "4. Run Iniciar Gombeth Urban.bat.",
    "5. The application opens at http://127.0.0.1:8080.",
    "6. Run Detener Gombeth Urban.bat to stop it.",
    "",
    "CLIENT PC REQUIREMENTS",
    "- Windows 10 or Windows 11, 64 bit.",
    "- Internet access.",
    "- Outbound access to the MySQL server on port 3306.",
    "- A modern browser.",
    "- Java installation is not required. The runtime is included."
) -join [Environment]::NewLine

Set-Content -LiteralPath (Join-Path $ApplicationFolder "LEEME_INSTALACION.txt") -Value $ReadmeText -Encoding ASCII

$Zip = Join-Path $Release ("GombethUrban-" + $Version + "-Windows-x64.zip")

if (Test-Path $Zip) {
    Remove-Item $Zip -Force
}

Compress-Archive -Path $ApplicationFolder -DestinationPath $Zip -CompressionLevel Optimal -Force

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "CLIENT RELEASE COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host ("Folder: " + $ApplicationFolder)
Write-Host ("ZIP:    " + $Zip)
Write-Host "============================================================" -ForegroundColor Green