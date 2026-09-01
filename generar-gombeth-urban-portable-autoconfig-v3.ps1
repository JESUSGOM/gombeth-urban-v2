param(
    [string]$Version = "1.0.1"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = "W:\PROYECTOS\gombeth-urban-v2"
$AppImage = Join-Path $Root "release-portable-current\app-image\Gombeth Urban"
$ConfigBuild = "C:\ProgramData\GombethUrban\config\application-local.properties"
$SevenZip = "C:\GombethUrban-Portable-Tools\7za.exe"
$IExpress = "$env:WINDIR\System32\iexpress.exe"

$BuildRoot = "C:\GombethUrban-Portable-Build-Autoconfig-V3"
$SourceDir = Join-Path $BuildRoot "source"
$OutputLocalDir = Join-Path $BuildRoot "out"
$ReleaseDir = Join-Path $Root "release-portable"

$Payload = Join-Path $SourceDir "payload.7z"
$Bootstrap = Join-Path $SourceDir "bootstrap.ps1"
$LauncherCmd = Join-Path $SourceDir "launcher.cmd"
$SedFile = Join-Path $BuildRoot "GombethUrban-Autoconfig-V3.sed"

$OutputLocal = Join-Path $OutputLocalDir "GombethUrban.exe"
$OutputFinal = Join-Path $ReleaseDir "GombethUrban.exe"

$BuildId = "$Version-" + (Get-Date -Format "yyyyMMdd-HHmmss")

function Leer-Propiedad {
    param([string]$Ruta,[string]$Nombre)
    $prefijo = $Nombre + "="
    $linea = Get-Content -LiteralPath $Ruta |
        ForEach-Object { $_.Trim() } |
        Where-Object {
            $_ -ne "" -and
            -not $_.StartsWith("#") -and
            $_.StartsWith($prefijo,[System.StringComparison]::OrdinalIgnoreCase)
        } |
        Select-Object -First 1
    if ($null -eq $linea) { return $null }
    return $linea.Substring($linea.IndexOf("=") + 1).Trim()
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " GOMBETH URBAN - EXE AUTOCONFIGURABLE V3" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Version : $Version"
Write-Host "BuildId : $BuildId"
Write-Host ""

$LauncherOriginal = Join-Path $AppImage "Gombeth Urban.exe"
$AppFolder = Join-Path $AppImage "app"
$RuntimeFolder = Join-Path $AppImage "runtime"

foreach ($Item in @($LauncherOriginal,$AppFolder,$RuntimeFolder,$SevenZip,$IExpress,$ConfigBuild)) {
    if (-not (Test-Path $Item)) { throw "Falta el requisito: $Item" }
}

$DatasourceUrl = Leer-Propiedad -Ruta $ConfigBuild -Nombre "spring.datasource.url"
$DatasourceDriver = Leer-Propiedad -Ruta $ConfigBuild -Nombre "spring.datasource.driver-class-name"

if ([string]::IsNullOrWhiteSpace($DatasourceUrl)) {
    throw "No se encontro spring.datasource.url."
}
if ([string]::IsNullOrWhiteSpace($DatasourceDriver)) {
    throw "No se encontro spring.datasource.driver-class-name."
}

$DatasourceUrlPs = $DatasourceUrl.Replace("'", "''")
$DatasourceDriverPs = $DatasourceDriver.Replace("'", "''")

Write-Host "App-image actual       : OK" -ForegroundColor Green
Write-Host "Configuracion base     : OK" -ForegroundColor Green
Write-Host "Credenciales en el EXE : NO" -ForegroundColor Green
Write-Host "Cifrado                : SecureString + DPAPI" -ForegroundColor Green
Write-Host "Staging persistente    : SI" -ForegroundColor Green
Write-Host "7za.exe                : OK" -ForegroundColor Green
Write-Host "IExpress               : OK" -ForegroundColor Green

Remove-Item $BuildRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $SourceDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutputLocalDir | Out-Null
New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
Remove-Item $OutputFinal -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "PASO 1/4 - Comprimiendo app-image actual..." -ForegroundColor Yellow

Push-Location $AppImage
try {
    & $SevenZip a -t7z -mx=7 $Payload ".\*" -y
    if ($LASTEXITCODE -ne 0) { throw "7-Zip termino con codigo $LASTEXITCODE." }
}
finally {
    Pop-Location
}

if (-not (Test-Path $Payload)) { throw "No se genero payload.7z." }

Write-Host ("Payload: {0:N2} MB" -f ((Get-Item $Payload).Length / 1MB)) -ForegroundColor Green

Write-Host ""
Write-Host "PASO 2/4 - Generando bootstrap seguro..." -ForegroundColor Yellow

$BootstrapContent = @"
`$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

`$BuildId = '$BuildId'
`$DbUrl = '$DatasourceUrlPs'
`$DbDriver = '$DatasourceDriverPs'

`$Here = Split-Path -Parent `$MyInvocation.MyCommand.Path
`$SevenZip = Join-Path `$Here '7za.exe'
`$Payload = Join-Path `$Here 'payload.7z'

`$Base = Join-Path `$env:LOCALAPPDATA 'GombethUrban'
`$Target = Join-Path `$Base ('portable\' + `$BuildId)
`$App = Join-Path `$Target 'Gombeth Urban.exe'
`$Ready = Join-Path `$Target '.ready'
`$SecureConfigDir = Join-Path `$Base 'secure-config'
`$CredentialFile = Join-Path `$SecureConfigDir 'credenciales.json'
`$LegacyConfig = 'C:\ProgramData\GombethUrban\config\application-local.properties'
`$Log = Join-Path `$Base 'portable-launch.log'

function Log([string]`$Message) {
    New-Item -ItemType Directory -Force -Path `$Base | Out-Null
    Add-Content -LiteralPath `$Log -Value ("[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), `$Message)
}

function Read-LegacyProperty([string]`$Name) {
    if (-not (Test-Path `$LegacyConfig)) { return `$null }
    `$Prefix = `$Name + '='
    `$Line = Get-Content -LiteralPath `$LegacyConfig |
        ForEach-Object { `$_.Trim() } |
        Where-Object {
            `$_ -ne '' -and
            -not `$_.StartsWith('#') -and
            `$_.StartsWith(`$Prefix,[System.StringComparison]::OrdinalIgnoreCase)
        } |
        Select-Object -First 1
    if (`$null -eq `$Line) { return `$null }
    return `$Line.Substring(`$Line.IndexOf('=') + 1).Trim()
}

function Protect-Text([string]`$PlainText) {
    `$Secure = ConvertTo-SecureString `$PlainText -AsPlainText -Force
    return ConvertFrom-SecureString `$Secure
}

function Unprotect-Text([string]`$CipherText) {
    `$Secure = ConvertTo-SecureString `$CipherText
    return (New-Object System.Net.NetworkCredential('',`$Secure)).Password
}

function Save-Credentials([string]`$Username,[string]`$Password) {
    New-Item -ItemType Directory -Force -Path `$SecureConfigDir | Out-Null
    `$Data = [ordered]@{
        version = 3
        username = Protect-Text `$Username
        password = Protect-Text `$Password
    }
    `$Data | ConvertTo-Json | Set-Content -LiteralPath `$CredentialFile -Encoding UTF8
}

function Load-Credentials {
    if (-not (Test-Path `$CredentialFile)) { return `$null }
    try {
        `$Data = Get-Content -LiteralPath `$CredentialFile -Raw | ConvertFrom-Json
        return [PSCustomObject]@{
            Username = Unprotect-Text `$Data.username
            Password = Unprotect-Text `$Data.password
        }
    }
    catch {
        Log ('No se pudieron descifrar las credenciales: ' + `$_.Exception.Message)
        Remove-Item `$CredentialFile -Force -ErrorAction SilentlyContinue
        return `$null
    }
}

function Show-CredentialForm {
    `$Form = New-Object System.Windows.Forms.Form
    `$Form.Text = 'Gombeth Urban - Primera configuracion'
    `$Form.StartPosition = 'CenterScreen'
    `$Form.Size = New-Object System.Drawing.Size(430,275)
    `$Form.FormBorderStyle = 'FixedDialog'
    `$Form.MaximizeBox = `$false
    `$Form.MinimizeBox = `$false
    `$Form.TopMost = `$true

    `$Info = New-Object System.Windows.Forms.Label
    `$Info.Location = New-Object System.Drawing.Point(20,18)
    `$Info.Size = New-Object System.Drawing.Size(380,45)
    `$Info.Text = 'Introduzca las credenciales de conexion. Se guardaran cifradas para este usuario de Windows.'
    `$Form.Controls.Add(`$Info)

    `$UserLabel = New-Object System.Windows.Forms.Label
    `$UserLabel.Location = New-Object System.Drawing.Point(20,78)
    `$UserLabel.Size = New-Object System.Drawing.Size(110,20)
    `$UserLabel.Text = 'Usuario'
    `$Form.Controls.Add(`$UserLabel)

    `$UserBox = New-Object System.Windows.Forms.TextBox
    `$UserBox.Location = New-Object System.Drawing.Point(140,75)
    `$UserBox.Size = New-Object System.Drawing.Size(245,24)
    `$Form.Controls.Add(`$UserBox)

    `$PasswordLabel = New-Object System.Windows.Forms.Label
    `$PasswordLabel.Location = New-Object System.Drawing.Point(20,120)
    `$PasswordLabel.Size = New-Object System.Drawing.Size(110,20)
    `$PasswordLabel.Text = 'Contrasena'
    `$Form.Controls.Add(`$PasswordLabel)

    `$PasswordBox = New-Object System.Windows.Forms.TextBox
    `$PasswordBox.Location = New-Object System.Drawing.Point(140,117)
    `$PasswordBox.Size = New-Object System.Drawing.Size(245,24)
    `$PasswordBox.UseSystemPasswordChar = `$true
    `$Form.Controls.Add(`$PasswordBox)

    `$OkButton = New-Object System.Windows.Forms.Button
    `$OkButton.Location = New-Object System.Drawing.Point(205,175)
    `$OkButton.Size = New-Object System.Drawing.Size(85,30)
    `$OkButton.Text = 'Guardar'
    `$OkButton.DialogResult = [System.Windows.Forms.DialogResult]::OK
    `$Form.AcceptButton = `$OkButton
    `$Form.Controls.Add(`$OkButton)

    `$CancelButton = New-Object System.Windows.Forms.Button
    `$CancelButton.Location = New-Object System.Drawing.Point(300,175)
    `$CancelButton.Size = New-Object System.Drawing.Size(85,30)
    `$CancelButton.Text = 'Cancelar'
    `$CancelButton.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
    `$Form.CancelButton = `$CancelButton
    `$Form.Controls.Add(`$CancelButton)

    `$Result = `$Form.ShowDialog()
    if (`$Result -ne [System.Windows.Forms.DialogResult]::OK) { return `$null }

    `$Username = `$UserBox.Text.Trim()
    `$Password = `$PasswordBox.Text

    if ([string]::IsNullOrWhiteSpace(`$Username) -or [string]::IsNullOrWhiteSpace(`$Password)) {
        [System.Windows.Forms.MessageBox]::Show('Debe indicar usuario y contrasena.','Gombeth Urban') | Out-Null
        return Show-CredentialForm
    }

    return [PSCustomObject]@{ Username=`$Username; Password=`$Password }
}

function Test-Port8080 {
    try {
        `$Client = New-Object System.Net.Sockets.TcpClient
        `$Async = `$Client.BeginConnect('127.0.0.1',8080,`$null,`$null)
        `$Connected = `$Async.AsyncWaitHandle.WaitOne(500)
        if (`$Connected -and `$Client.Connected) {
            `$Client.EndConnect(`$Async)
            `$Client.Close()
            return `$true
        }
        `$Client.Close()
        return `$false
    }
    catch { return `$false }
}

try {
    Log ('Inicio portable V3 ' + `$BuildId)
    Log ('Bootstrap persistente: ' + `$Here)

    if (-not (Test-Path `$SevenZip)) { throw 'No existe 7za.exe en staging.' }
    if (-not (Test-Path `$Payload)) { throw 'No existe payload.7z en staging.' }

    if (Test-Port8080) {
        Log 'Puerto 8080 ya disponible. Abriendo navegador.'
        Start-Process 'http://127.0.0.1:8080/login'
        exit 0
    }

    if (-not (Test-Path `$Ready) -or -not (Test-Path `$App)) {
        Log ('Creando destino: ' + `$Target)

        if (Test-Path `$Target) { Remove-Item `$Target -Recurse -Force }
        New-Item -ItemType Directory -Force -Path `$Target | Out-Null

        Log 'Iniciando extraccion 7-Zip.'
        & `$SevenZip x `$Payload "-o`$Target" -y | Out-Null

        `$Exit7z = `$LASTEXITCODE
        Log ('7-Zip termino con codigo ' + `$Exit7z)

        if (`$Exit7z -ne 0 -or -not (Test-Path `$App)) {
            throw 'No se pudo extraer correctamente Gombeth Urban.'
        }

        Set-Content -LiteralPath `$Ready -Value 'OK' -Encoding ASCII
        Log 'App-image extraida correctamente.'
    }

    `$Credentials = Load-Credentials

    if (`$null -eq `$Credentials -and (Test-Path `$LegacyConfig)) {
        Log 'Configuracion antigua detectada. Iniciando migracion.'

        `$LegacyUser = Read-LegacyProperty 'spring.datasource.username'
        `$LegacyPassword = Read-LegacyProperty 'spring.datasource.password'

        if (-not [string]::IsNullOrWhiteSpace(`$LegacyUser) -and
            -not [string]::IsNullOrWhiteSpace(`$LegacyPassword)) {
            Save-Credentials `$LegacyUser `$LegacyPassword
            `$Credentials = Load-Credentials
            Log 'Credenciales migradas a SecureString/DPAPI.'
        }
    }

    if (`$null -eq `$Credentials) {
        Log 'No hay credenciales. Mostrando primera configuracion.'
        `$Credentials = Show-CredentialForm

        if (`$null -eq `$Credentials) {
            Log 'Configuracion cancelada.'
            exit 0
        }

        Save-Credentials `$Credentials.Username `$Credentials.Password
        `$Credentials = Load-Credentials
        Log 'Credenciales iniciales guardadas con SecureString/DPAPI.'
    }

    `$env:SPRING_PROFILES_ACTIVE = 'prod,local'
    `$env:GOMBETH_PRODUCTION_CONFIRMED = 'true'
    `$env:SPRING_DATASOURCE_URL = `$DbUrl
    `$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = `$DbDriver
    `$env:SPRING_DATASOURCE_USERNAME = `$Credentials.Username
    `$env:SPRING_DATASOURCE_PASSWORD = `$Credentials.Password
    `$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'none'
    `$env:SERVER_ADDRESS = '127.0.0.1'
    `$env:SERVER_PORT = '8080'

    Log ('Arrancando aplicacion: ' + `$App)

    `$Process = Start-Process -FilePath `$App -PassThru
    `$Deadline = (Get-Date).AddSeconds(90)

    while ((Get-Date) -lt `$Deadline) {
        if (Test-Port8080) {
            Log 'Puerto 8080 disponible. Abriendo navegador.'
            Start-Process 'http://127.0.0.1:8080/login'
            exit 0
        }

        if (`$Process.HasExited) {
            Log ('La aplicacion termino antes de abrir 8080. Codigo: ' + `$Process.ExitCode)
            [System.Windows.Forms.MessageBox]::Show(
                'Gombeth Urban no pudo iniciarse. Revise la configuracion de acceso.',
                'Gombeth Urban',
                [System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Error
            ) | Out-Null
            exit 20
        }

        Start-Sleep -Milliseconds 800
    }

    Log 'Tiempo agotado esperando 8080.'
    [System.Windows.Forms.MessageBox]::Show(
        'Gombeth Urban esta tardando demasiado en iniciar.',
        'Gombeth Urban',
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Warning
    ) | Out-Null
    exit 21
}
catch {
    Log ('ERROR: ' + `$_.Exception.Message)
    [System.Windows.Forms.MessageBox]::Show(
        ('No se pudo iniciar Gombeth Urban.' + [Environment]::NewLine + [Environment]::NewLine + `$_.Exception.Message),
        'Gombeth Urban',
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null
    exit 99
}
"@

Set-Content -LiteralPath $Bootstrap -Value $BootstrapContent -Encoding UTF8

Write-Host ""
Write-Host "PASO 3/4 - Generando launcher con staging persistente..." -ForegroundColor Yellow

$LauncherContent = @"
@echo off
setlocal EnableExtensions

set "BUILD_ID=$BuildId"
set "SOURCE=%~dp0"
set "STAGE=%LOCALAPPDATA%\GombethUrban\bootstrap\%BUILD_ID%"
set "LOG=%LOCALAPPDATA%\GombethUrban\portable-launch.log"

if not exist "%LOCALAPPDATA%\GombethUrban" mkdir "%LOCALAPPDATA%\GombethUrban" >nul 2>&1
if not exist "%STAGE%" mkdir "%STAGE%" >nul 2>&1

echo [%date% %time%] Launcher V3. Copiando staging %BUILD_ID%...>>"%LOG%"

copy /Y "%SOURCE%payload.7z" "%STAGE%\payload.7z" >nul
if errorlevel 1 exit /b 31

copy /Y "%SOURCE%7za.exe" "%STAGE%\7za.exe" >nul
if errorlevel 1 exit /b 32

copy /Y "%SOURCE%bootstrap.ps1" "%STAGE%\bootstrap.ps1" >nul
if errorlevel 1 exit /b 33

echo [%date% %time%] Staging completo. Ejecutando bootstrap persistente.>>"%LOG%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%STAGE%\bootstrap.ps1"

set "RC=%ERRORLEVEL%"
echo [%date% %time%] Bootstrap finalizado con codigo %RC%.>>"%LOG%"
exit /b %RC%
"@

Set-Content -LiteralPath $LauncherCmd -Value $LauncherContent -Encoding ASCII
Copy-Item $SevenZip (Join-Path $SourceDir "7za.exe") -Force

$SedContent = @"
[Version]
Class=IEXPRESS
SEDVersion=3

[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=0
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=%TargetName%
FriendlyName=%FriendlyName%
AppLaunched=%AppLaunched%
PostInstallCmd=%PostInstallCmd%
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles

[Strings]
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$OutputLocal
FriendlyName=Gombeth Urban
AppLaunched=launcher.cmd
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
FILE0="payload.7z"
FILE1="7za.exe"
FILE2="bootstrap.ps1"
FILE3="launcher.cmd"

[SourceFiles]
SourceFiles0=$SourceDir\

[SourceFiles0]
%FILE0%=
%FILE1%=
%FILE2%=
%FILE3%=
"@

Set-Content -LiteralPath $SedFile -Value $SedContent -Encoding ASCII

Write-Host ""
Write-Host "PASO 4/4 - Construyendo GombethUrban.exe..." -ForegroundColor Yellow

& $IExpress /N /Q $SedFile

$Deadline = (Get-Date).AddSeconds(180)
$LastSize = -1L
$StableChecks = 0

while ((Get-Date) -lt $Deadline) {
    if (Test-Path $OutputLocal) {
        $CurrentSize = (Get-Item $OutputLocal).Length
        if ($CurrentSize -gt 10MB -and $CurrentSize -eq $LastSize) {
            $StableChecks++
        } else {
            $StableChecks = 0
        }
        $LastSize = $CurrentSize
        if ($StableChecks -ge 3) { break }
    }
    Start-Sleep -Seconds 2
}

if (-not (Test-Path $OutputLocal)) {
    throw "IExpress no genero el EXE en 180 segundos: $OutputLocal"
}
if ((Get-Item $OutputLocal).Length -le 10MB) {
    throw "El EXE generado parece incompleto."
}

Copy-Item $OutputLocal $OutputFinal -Force

$FinalInfo = Get-Item $OutputFinal
$Hash = Get-FileHash -LiteralPath $OutputFinal -Algorithm SHA256

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " EXE AUTOCONFIGURABLE V3 GENERADO CORRECTAMENTE" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

[PSCustomObject]@{
    Archivo         = $FinalInfo.FullName
    TamanoMB        = [math]::Round($FinalInfo.Length / 1MB,2)
    BuildId         = $BuildId
    SHA256          = $Hash.Hash
    CredencialesEXE = "NO"
    CifradoCliente  = "SecureString + DPAPI / CurrentUser"
    Staging         = "LocalAppData persistente antes de bootstrap"
} | Format-List

Write-Host ""
Write-Host "NO ejecute todavia el EXE." -ForegroundColor Yellow
Write-Host "Primero revisaremos juntos esta salida."
