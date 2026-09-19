[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$programRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$frontendRoot = Join-Path $programRoot 'frontend'
$backendRoot = Join-Path $programRoot 'backend'
$templateRoot = Join-Path $programRoot 'release-template'
$releaseRoot = Join-Path $programRoot 'release'
$destination = Join-Path $releaseRoot 'LeagueTicket'
$staging = Join-Path $releaseRoot '.LeagueTicket-staging'
$backup = Join-Path $releaseRoot '.LeagueTicket-previous'

function Assert-Command {
    param([Parameter(Mandatory = $true)][string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command was not found: $Name"
    }
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory
    )
    Push-Location $WorkingDirectory
    try {
        Write-Host "> $FilePath $($Arguments -join ' ')" -ForegroundColor Cyan
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

try {
    Assert-Command 'java.exe'
    Assert-Command 'jar.exe'
    Assert-Command 'node.exe'
    Assert-Command 'npm.cmd'

    $mavenWrapper = Join-Path $backendRoot 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf)) {
        throw "Maven Wrapper was not found: $mavenWrapper"
    }

    Write-Host '[1/8] Installing locked frontend dependencies...' -ForegroundColor Green
    Invoke-Checked 'npm.cmd' @('ci') $frontendRoot

    Write-Host '[2/8] Building the Vue frontend...' -ForegroundColor Green
    Invoke-Checked 'npm.cmd' @('run', 'build') $frontendRoot

    $frontendIndex = Join-Path $frontendRoot 'dist\index.html'
    if (-not (Test-Path -LiteralPath $frontendIndex -PathType Leaf)) {
        throw "Frontend build did not produce: $frontendIndex"
    }

    Write-Host '[3/8] Running backend tests...' -ForegroundColor Green
    Invoke-Checked $mavenWrapper @('clean', 'test') $backendRoot

    Write-Host '[4/8] Packaging the release JAR and runtime dependencies...' -ForegroundColor Green
    Invoke-Checked $mavenWrapper @('package', '-Prelease', '-DskipTests') $backendRoot

    $jarFile = Get-ChildItem -LiteralPath (Join-Path $backendRoot 'target') -Filter '*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $jarFile) {
        throw 'The release build did not produce an executable JAR.'
    }

    $dependencyRoot = Join-Path $backendRoot 'target\release-dependencies'
    $runtimeJars = @(Get-ChildItem -LiteralPath $dependencyRoot -Filter '*.jar' -File -ErrorAction SilentlyContinue)
    if ($runtimeJars.Count -eq 0) {
        throw 'No runtime dependencies were copied to target\release-dependencies.'
    }

    Write-Host '[5/8] Verifying executable JAR contents...' -ForegroundColor Green
    $jarEntries = & jar.exe tf $jarFile.FullName
    if ($LASTEXITCODE -ne 0) {
        throw 'jar.exe could not inspect the packaged JAR.'
    }
    if ($jarEntries -notcontains 'BOOT-INF/classes/static/index.html') {
        throw 'The packaged JAR does not contain static/index.html.'
    }
    if (-not ($jarEntries | Where-Object { $_ -like 'BOOT-INF/classes/static/assets/*' })) {
        throw 'The packaged JAR does not contain frontend assets.'
    }
    if (-not ($jarEntries | Where-Object { $_ -like 'BOOT-INF/lib/*.jar' })) {
        throw 'The packaged JAR does not contain Spring Boot runtime dependencies.'
    }

    Write-Host '[6/8] Assembling a clean staging directory...' -ForegroundColor Green
    New-Item -ItemType Directory -Path $releaseRoot -Force | Out-Null
    if (Test-Path -LiteralPath $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
    New-Item -ItemType Directory -Path $staging | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $staging 'third-party-jars') | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $staging 'uploads') | Out-Null

    Copy-Item -LiteralPath $jarFile.FullName -Destination (Join-Path $staging 'league-ticket.jar')
    Copy-Item -LiteralPath (Join-Path $templateRoot 'start.bat') -Destination $staging
    Copy-Item -LiteralPath (Join-Path $templateRoot 'init-db.bat') -Destination $staging
    Copy-Item -LiteralPath (Join-Path $templateRoot 'config.bat.example') -Destination $staging
    $releaseReadme = Get-ChildItem -LiteralPath $templateRoot -Filter 'README-*.txt' -File |
        Select-Object -First 1
    if (-not $releaseReadme) {
        throw 'Release README template was not found.'
    }
    Copy-Item -LiteralPath $releaseReadme.FullName -Destination $staging
    Copy-Item -LiteralPath (Join-Path $programRoot 'database') -Destination $staging -Recurse
    Copy-Item -LiteralPath $runtimeJars.FullName -Destination (Join-Path $staging 'third-party-jars')

    $requiredFiles = @(
        'league-ticket.jar', 'start.bat', 'init-db.bat', 'config.bat.example',
        'database\schema.sql', 'database\seed.sql', 'database\test-data.sql'
    )
    foreach ($relativePath in $requiredFiles) {
        if (-not (Test-Path -LiteralPath (Join-Path $staging $relativePath) -PathType Leaf)) {
            throw "Release staging is incomplete; missing: $relativePath"
        }
    }
    if (-not (Get-ChildItem -LiteralPath $staging -Filter 'README-*.txt' -File)) {
        throw 'Release staging is incomplete; missing the runtime README.'
    }

    Write-Host '[7/8] Publishing the completed Release...' -ForegroundColor Green
    if (Test-Path -LiteralPath $backup) {
        Remove-Item -LiteralPath $backup -Recurse -Force
    }
    if (Test-Path -LiteralPath $destination) {
        Move-Item -LiteralPath $destination -Destination $backup
    }
    try {
        Move-Item -LiteralPath $staging -Destination $destination
    }
    catch {
        if ((Test-Path -LiteralPath $backup) -and -not (Test-Path -LiteralPath $destination)) {
            Move-Item -LiteralPath $backup -Destination $destination
        }
        throw
    }
    if (Test-Path -LiteralPath $backup) {
        Remove-Item -LiteralPath $backup -Recurse -Force
    }

    Write-Host '[8/8] Release completed successfully.' -ForegroundColor Green
    Write-Host "Output: $destination" -ForegroundColor Green
    Write-Host "Runtime dependency JARs: $($runtimeJars.Count)"
}
catch {
    Write-Error $_
    exit 1
}
