param(
    [string]$Version = "1.0.1"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = "W:\PROYECTOS\gombeth-urban-v2"
$Frontend = Join-Path $Root "frontend"
$Backend = Join-Path $Root "backend"

$ReleaseRoot = Join-Path $Root "release-portable-current"
$JpackageInput = Join-Path $ReleaseRoot "jpackage-input"
$JpackageOutput = Join-Path $ReleaseRoot "app-image"

function Ejecutar {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Programa,

        [Parameter(Mandatory = $true)]
        [string[]]$Argumentos,

        [Parameter(Mandatory = $true)]
        [string]$Directorio
    )

    Push-Location $Directorio
    try {
        Write-Host ""
        Write-Host (">> " + $Programa + " " + ($Argumentos -join " ")) -ForegroundColor Cyan

        & $Programa @Argumentos

        if ($LASTEXITCODE -ne 0) {
            throw "El comando '$Programa' termino con codigo $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " GOMBETH URBAN - APP-IMAGE DESDE CODIGO ACTUAL" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Version de empaquetado: $Version"
Write-Host ""

if (-not (Test-Path $Frontend)) {
    throw "No existe la carpeta frontend: $Frontend"
}

if (-not (Test-Path $Backend)) {
    throw "No existe la carpeta backend: $Backend"
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "No se encuentra npm en PATH."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "No se encuentra Maven en PATH."
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "No se encuentra jpackage. Debe ejecutarse con un JDK 21 completo."
}

# 1. Compilar Angular actual.
Write-Host "PASO 1/4 - Compilando Angular actual..." -ForegroundColor Yellow
Ejecutar `
    -Programa "npm" `
    -Argumentos @("run", "build") `
    -Directorio $Frontend

# 2. Compilar backend y ejecutar pruebas.
Write-Host ""
Write-Host "PASO 2/4 - Compilando Spring Boot y ejecutando pruebas..." -ForegroundColor Yellow
Ejecutar `
    -Programa "mvn" `
    -Argumentos @("clean", "package") `
    -Directorio $Backend

# 3. Localizar el JAR ejecutable.
$Jar = Get-ChildItem `
    -Path (Join-Path $Backend "target") `
    -Filter "*.jar" `
    -File |
    Where-Object {
        $_.Name -notmatch "\.original$" -and
        $_.Name -notmatch "sources" -and
        $_.Name -notmatch "javadoc"
    } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $Jar) {
    throw "No se encontro el JAR ejecutable en backend\target."
}

Write-Host ""
Write-Host "JAR actual localizado:" -ForegroundColor Green
Write-Host $Jar.FullName

# 4. Crear app-image limpia con runtime Java incluido.
Write-Host ""
Write-Host "PASO 3/4 - Preparando jpackage..." -ForegroundColor Yellow

if (Test-Path $ReleaseRoot) {
    Remove-Item $ReleaseRoot -Recurse -Force
}

New-Item -Path $JpackageInput -ItemType Directory -Force | Out-Null
New-Item -Path $JpackageOutput -ItemType Directory -Force | Out-Null

$JarDestination = Join-Path $JpackageInput "gombeth-urban.jar"

Copy-Item `
    -LiteralPath $Jar.FullName `
    -Destination $JarDestination `
    -Force

Write-Host ""
Write-Host "PASO 4/4 - Generando app-image actual con Java incluido..." -ForegroundColor Yellow

$JpackageArguments = @(
    "--type", "app-image",
    "--name", "Gombeth Urban",
    "--input", $JpackageInput,
    "--main-jar", "gombeth-urban.jar",
    "--main-class", "org.springframework.boot.loader.launch.JarLauncher",
    "--dest", $JpackageOutput,
    "--app-version", $Version,
    "--vendor", "Gombeth Urban",
    "--description", "Gestion profesional de comunidades"
)

Ejecutar `
    -Programa "jpackage" `
    -Argumentos $JpackageArguments `
    -Directorio $Root

$ApplicationFolder = Join-Path $JpackageOutput "Gombeth Urban"
$Launcher = Join-Path $ApplicationFolder "Gombeth Urban.exe"
$AppFolder = Join-Path $ApplicationFolder "app"
$RuntimeFolder = Join-Path $ApplicationFolder "runtime"

if (-not (Test-Path $Launcher)) {
    throw "No se creo el lanzador esperado: $Launcher"
}

if (-not (Test-Path $AppFolder)) {
    throw "No se creo la carpeta app: $AppFolder"
}

if (-not (Test-Path $RuntimeFolder)) {
    throw "No se creo la carpeta runtime: $RuntimeFolder"
}

$TotalBytes = (
    Get-ChildItem $ApplicationFolder -Recurse -File |
    Measure-Object Length -Sum
).Sum

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " APP-IMAGE ACTUAL GENERADA CORRECTAMENTE" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

[PSCustomObject]@{
    Carpeta      = $ApplicationFolder
    Ejecutable   = $Launcher
    JarOrigen    = $Jar.FullName
    TamanoMB     = [math]::Round($TotalBytes / 1MB, 2)
    TieneApp     = (Test-Path $AppFolder)
    TieneRuntime = (Test-Path $RuntimeFolder)
} | Format-List

Write-Host ""
Write-Host "NO ejecutes todavia el EXE de esta app-image." -ForegroundColor Yellow
Write-Host "Primero revisaremos juntos esta salida y despues generaremos el EXE unico."
