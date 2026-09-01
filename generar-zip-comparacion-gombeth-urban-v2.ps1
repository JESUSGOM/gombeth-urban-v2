param(
    [string]$Proyecto = "W:\PROYECTOS\gombeth-urban-v2"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$parent = Split-Path -Parent $Proyecto
$nombreZip = "gombeth-urban-v2-comparacion-$timestamp.zip"
$zipFinal = Join-Path $parent $nombreZip

$tempRoot = Join-Path $env:TEMP "gombeth-urban-v2-comparacion-$timestamp"
$stage = Join-Path $tempRoot "gombeth-urban-v2"

function Copiar-ArchivoRelativo {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Relativo
    )

    $origen = Join-Path $Proyecto $Relativo

    if (-not (Test-Path -LiteralPath $origen -PathType Leaf)) {
        return
    }

    $destino = Join-Path $stage $Relativo
    $dirDestino = Split-Path -Parent $destino

    if (-not (Test-Path -LiteralPath $dirDestino)) {
        New-Item -ItemType Directory -Force -Path $dirDestino | Out-Null
    }

    Copy-Item -LiteralPath $origen -Destination $destino -Force
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " GOMBETH URBAN V2 - ZIP MINIMO PARA COMPARACION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path -LiteralPath $Proyecto)) {
    throw "No existe el proyecto: $Proyecto"
}

Push-Location $Proyecto
try {
    if (-not (Test-Path ".git")) {
        throw "La carpeta no parece ser un repositorio Git."
    }

    $estado = git status --porcelain

    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo consultar el estado Git."
    }

    if ($estado) {
        Write-Host "ATENCION: el repositorio tiene cambios sin confirmar." -ForegroundColor Red
        Write-Host "No se genera el ZIP para evitar comparar una version ambigua." -ForegroundColor Red
        Write-Host ""
        git status --short
        exit 20
    }

    $rama = (git branch --show-current).Trim()
    $commit = (git rev-parse HEAD).Trim()
    $commitCorto = (git rev-parse --short HEAD).Trim()
    $ultimoCommit = (git log -1 --pretty=format:"%h | %ad | %s" --date=iso-strict)

    Write-Host "Repositorio limpio       : OK" -ForegroundColor Green
    Write-Host "Rama                     : $rama"
    Write-Host "Commit                   : $commitCorto"

    Remove-Item $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $stage | Out-Null

    # --------------------------------------------------------
    # Seleccion:
    # - código Java de producción y pruebas
    # - recursos backend, excepto Angular compilado
    # - código fuente Angular completo
    # - ficheros de construcción/configuración reproducible
    # - scripts definitivos de empaquetado
    #
    # Se parte de git ls-files: nunca copia .git, releases,
    # runtimes, EXE ni artefactos no versionados.
    # --------------------------------------------------------

    $versionados = git ls-files

    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo obtener git ls-files."
    }

    $seleccionados = New-Object System.Collections.Generic.List[string]

    foreach ($f in $versionados) {
        $p = $f.Replace("\", "/")

        $incluir = $false

        if ($p -like "backend/src/main/java/*") { $incluir = $true }
        elseif ($p -like "backend/src/test/java/*") { $incluir = $true }
        elseif ($p -like "backend/src/test/resources/*") { $incluir = $true }
        elseif ($p -like "backend/src/main/resources/*") { $incluir = $true }
        elseif ($p -like "frontend/src/*") { $incluir = $true }
        elseif ($p -eq "backend/pom.xml") { $incluir = $true }
        elseif ($p -eq "frontend/package.json") { $incluir = $true }
        elseif ($p -eq "frontend/package-lock.json") { $incluir = $true }
        elseif ($p -eq "frontend/angular.json") { $incluir = $true }
        elseif ($p -like "frontend/tsconfig*.json") { $incluir = $true }
        elseif ($p -eq ".gitignore") { $incluir = $true }
        elseif ($p -eq "README.md") { $incluir = $true }
        elseif ($p -eq "generar-app-image-actual.ps1") { $incluir = $true }
        elseif ($p -eq "generar-gombeth-urban-portable-autoconfig-v3.ps1") { $incluir = $true }

        # Exclusiones explícitas incluso dentro de los árboles incluidos.
        if ($p -like "backend/src/main/resources/static/*") { $incluir = $false }
        if ($p -match "(^|/)application-local\.properties$") { $incluir = $false }
        if ($p -match "(^|/)application-secrets\.properties$") { $incluir = $false }
        if ($p -match "(^|/)\.env(\..*)?$") { $incluir = $false }
        if ($p -match "\.(exe|msi|jar|war|zip|rar|7z|bak|old|log|tmp|pid)$") { $incluir = $false }
        if ($p -match "\.(p12|pfx|pem|key|jks|keystore)$") { $incluir = $false }
        if ($p -match "(^|/)(target|node_modules|dist|\.angular|packaging|portable-build|release-portable|release-portable-current)(/|$)") { $incluir = $false }

        # Dumps SQL fuera del directorio manual controlado: no.
        if ($p -match "\.sql$" -and $p -notlike "backend/src/main/resources/db/manual/*") {
            $incluir = $false
        }

        if ($incluir) {
            $seleccionados.Add($f)
        }
    }

    foreach ($f in $seleccionados) {
        Copiar-ArchivoRelativo -Relativo $f
    }

    # --------------------------------------------------------
    # Métricas para la siguiente guía comparativa
    # --------------------------------------------------------

    $javaProd = @(
        $seleccionados | Where-Object {
            $_ -like "backend/src/main/java/*.java" -or
            $_ -like "backend/src/main/java/*/*.java"
        }
    ).Count

    # Conteo recursivo real ya copiado.
    $javaProd = @(Get-ChildItem (Join-Path $stage "backend\src\main\java") -Recurse -File -Filter *.java -ErrorAction SilentlyContinue).Count
    $javaTest = @(Get-ChildItem (Join-Path $stage "backend\src\test\java") -Recurse -File -Filter *.java -ErrorAction SilentlyContinue).Count
    $tsApp = @(Get-ChildItem (Join-Path $stage "frontend\src") -Recurse -File -Filter *.ts -ErrorAction SilentlyContinue).Count
    $scssApp = @(Get-ChildItem (Join-Path $stage "frontend\src") -Recurse -File -Filter *.scss -ErrorAction SilentlyContinue).Count
    $htmlApp = @(Get-ChildItem (Join-Path $stage "frontend\src") -Recurse -File -Filter *.html -ErrorAction SilentlyContinue).Count

    $pomVersion = ""
    $pom = Join-Path $Proyecto "backend\pom.xml"
    if (Test-Path $pom) {
        try {
            [xml]$pomXml = Get-Content -LiteralPath $pom -Raw
            $pomVersion = [string]$pomXml.project.version
        } catch {}
    }

    $angularVersion = ""
    $packageJson = Join-Path $Proyecto "frontend\package.json"
    if (Test-Path $packageJson) {
        try {
            $pkg = Get-Content -LiteralPath $packageJson -Raw | ConvertFrom-Json
            if ($pkg.dependencies.'@angular/core') {
                $angularVersion = [string]$pkg.dependencies.'@angular/core'
            }
        } catch {}
    }

    $metadata = @"
GOMBETH URBAN V2 - PAQUETE MINIMO DE COMPARACION
Generado: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Proyecto origen: $Proyecto

GIT
Rama: $rama
Commit completo: $commit
Ultimo commit: $ultimoCommit

VERSIONES
Version backend/proyecto: $pomVersion
Angular @angular/core: $angularVersion

METRICAS DEL CODIGO INCLUIDO
Clases Java de produccion: $javaProd
Clases Java de prueba: $javaTest
Ficheros TypeScript en frontend/src: $tsApp
Ficheros SCSS en frontend/src: $scssApp
Ficheros HTML en frontend/src: $htmlApp
Total de ficheros incluidos: $($seleccionados.Count)

CRITERIO DEL PAQUETE
Incluye:
- backend/src/main/java
- backend/src/test/java
- backend/src/test/resources, si existe
- backend/src/main/resources, excepto static compilado y configuracion local/secreta
- frontend/src
- backend/pom.xml
- frontend/package.json y package-lock.json
- frontend/angular.json
- frontend/tsconfig*.json
- .gitignore y README.md, si existe
- scripts definitivos de app-image y portable V3

Excluye:
- .git
- target, node_modules, dist, .angular
- backend/src/main/resources/static (Angular compilado)
- packaging, portable-build, release-portable-current, release-portable
- EXE/MSI/JAR/WAR/ZIP/RAR/7Z generados
- application-local.properties, application-secrets.properties y .env
- certificados, claves y almacenes de claves
- dumps SQL, excepto SQL manual versionado en backend/src/main/resources/db/manual
- logs, temporales y backups
"@

    Set-Content -LiteralPath (Join-Path $stage "METADATOS_COMPARACION.txt") -Value $metadata -Encoding UTF8

    $lista = $seleccionados | Sort-Object
    Set-Content -LiteralPath (Join-Path $stage "LISTADO_ARCHIVOS_COMPARACION.txt") -Value $lista -Encoding UTF8

    # --------------------------------------------------------
    # Comprobación final de nombres sensibles
    # --------------------------------------------------------

    $sospechosos = Get-ChildItem $stage -Recurse -File |
        Where-Object {
            $nombre = $_.Name.ToLowerInvariant()

            $nombre -eq "application-local.properties" -or
            $nombre -eq "application-secrets.properties" -or
            $nombre -eq ".env" -or
            $nombre -like ".env.*" -or
            $nombre -eq "credenciales.json" -or
            $nombre -eq "credentials.json" -or
            $nombre -eq "secrets.json" -or
            $_.Extension -match "^\.(exe|msi|p12|pfx|pem|key|jks|keystore)$"
        }

    if ($sospechosos) {
        Write-Host ""
        Write-Host "ERROR: se detectaron ficheros potencialmente sensibles/no deseados:" -ForegroundColor Red
        $sospechosos.FullName
        throw "ZIP cancelado por comprobacion de seguridad."
    }

    if (Test-Path $zipFinal) {
        Remove-Item $zipFinal -Force
    }

    Write-Host ""
    Write-Host "Comprimiendo paquete..." -ForegroundColor Yellow

    Compress-Archive `
        -Path $stage `
        -DestinationPath $zipFinal `
        -CompressionLevel Optimal

    if (-not (Test-Path $zipFinal)) {
        throw "No se genero el ZIP final."
    }

    $zipInfo = Get-Item $zipFinal
    $hash = Get-FileHash $zipFinal -Algorithm SHA256

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host " ZIP DE COMPARACION GENERADO CORRECTAMENTE" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host ""

    [PSCustomObject]@{
        Archivo          = $zipInfo.FullName
        TamanoMB         = [math]::Round($zipInfo.Length / 1MB, 2)
        Rama             = $rama
        Commit           = $commitCorto
        JavaProduccion   = $javaProd
        JavaPruebas      = $javaTest
        TypeScript       = $tsApp
        SCSS             = $scssApp
        HTML             = $htmlApp
        SHA256           = $hash.Hash
    } | Format-List

    Write-Host ""
    Write-Host "Suba este ZIP a ChatGPT para hacer la comparacion con sepa-1914-parent." -ForegroundColor Cyan
}
finally {
    Pop-Location
    Remove-Item $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
