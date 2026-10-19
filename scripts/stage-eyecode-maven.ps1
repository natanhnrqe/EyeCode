# Stages the pinned EyeCode Desktop Maven into target/eyecode-desktop/runtime/maven.
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-maven.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-maven.ps1 -ArchivePath path\to\apache-maven-3.9.16-bin.zip
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-maven.ps1 -MavenHome C:\path\to\extracted-maven
param(
    [string]$Destination,
    [string]$ArchivePath,
    [string]$MavenHome,
    [string]$CacheDir
)

$ErrorActionPreference = "Stop"

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$RuntimeDir = [System.IO.Path]::Combine($RepoRoot, "target", "eyecode-desktop", "runtime")
if (-not $Destination) { $Destination = Join-Path $RuntimeDir "maven" }
if (-not $CacheDir) { $CacheDir = Join-Path $RuntimeDir "maven-cache" }

$Vendor = "Apache Software Foundation"
$Name = "Apache Maven"
$Version = "3.9.16"
$ArchiveName = "apache-maven-3.9.16-bin.zip"
$DownloadUrl = "https://downloads.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip"
$ExpectedSha512 = "ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3"

function Get-FileSha512([string]$Path) {
    (Get-FileHash -Algorithm SHA512 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Test-IsWindows {
    [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [System.Runtime.InteropServices.OSPlatform]::Windows)
}

function Assert-MavenHome([string]$HomePath) {
    if (-not (Test-Path -LiteralPath $HomePath -PathType Container)) {
        throw "Maven home does not exist or is not a directory: $HomePath"
    }
    $binDir = Join-Path $HomePath "bin"
    $mvnCmd = Join-Path $binDir "mvn.cmd"
    $mvnSh = Join-Path $binDir "mvn"
    if (-not (Test-Path -LiteralPath $mvnCmd -PathType Leaf)) {
        throw "Maven home is missing bin\mvn.cmd: $HomePath"
    }
    if (-not (Test-Path -LiteralPath $mvnSh -PathType Leaf)) {
        throw "Maven home is missing bin\mvn: $HomePath"
    }
    $settings = Join-Path (Join-Path $HomePath "conf") "settings.xml"
    if (-not (Test-Path -LiteralPath $settings -PathType Leaf)) {
        throw "Maven home is missing conf\settings.xml: $HomePath"
    }
    $license = Join-Path $HomePath "LICENSE"
    if (-not (Test-Path -LiteralPath $license -PathType Leaf)) {
        throw "Maven home is missing LICENSE (Apache-2.0): $HomePath"
    }
    $previousEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $probeOutput = ((& $mvnCmd -version 2>&1) | ForEach-Object { $_.ToString() }) -join "`n"
    } finally {
        $ErrorActionPreference = $previousEap
    }
    if ($probeOutput -notmatch [regex]::Escape("Apache Maven $Version")) {
        throw "mvn -version did not report Apache Maven ${Version}: $probeOutput"
    }
}

function Write-MavenManifest([string]$HomePath, [string]$Source, [string]$ArchiveSha512) {
    $parent = Split-Path -Parent $HomePath
    $manifestPath = Join-Path $parent "maven-manifest.json"
    $manifest = [ordered]@{
        vendor = $Vendor
        name = $Name
        version = $Version
        mvnCommand = (Join-Path (Join-Path $HomePath "bin") "mvn.cmd")
        home = $HomePath
        source = $Source
        archiveSha512 = $ArchiveSha512
        stagedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        downloadUrl = $DownloadUrl
        licenseNotice = "Apache Maven is licensed under the Apache License, Version 2.0. See LICENSE inside the Maven home and third-party-notices/THIRD-PARTY-NOTICES.md."
    }
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Write-Host "Manifest written: $manifestPath"
}

$destinationFull = [System.IO.Path]::GetFullPath($Destination)
$parentDir = Split-Path -Parent $destinationFull

if ($MavenHome) {
    Write-Host "Validating configured Maven home: $MavenHome"
    Assert-MavenHome $MavenHome
    if (Test-Path -LiteralPath $destinationFull) {
        Remove-Item -LiteralPath $destinationFull -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
    Copy-Item -LiteralPath $MavenHome -Destination $destinationFull -Recurse -Force
    Assert-MavenHome $destinationFull
    Write-MavenManifest $destinationFull "configured-maven-home" ""
    Write-Host "Staged Maven (from -MavenHome): $destinationFull"
    exit 0
}

$resolvedArchive = $null
if ($ArchivePath) {
    $resolvedArchive = [System.IO.Path]::GetFullPath($ArchivePath)
    if (-not (Test-Path -LiteralPath $resolvedArchive -PathType Leaf)) {
        throw "Archive not found: $resolvedArchive"
    }
    $actual = Get-FileSha512 $resolvedArchive
    if ($actual -ne $ExpectedSha512.ToLowerInvariant()) {
        throw "Archive SHA-512 mismatch for $resolvedArchive`n  expected: $ExpectedSha512`n  actual:   $actual"
    }
    Write-Host "Archive checksum verified: $resolvedArchive"
} else {
    New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null
    $cached = Join-Path $CacheDir $ArchiveName
    if (Test-Path -LiteralPath $cached -PathType Leaf) {
        $actual = Get-FileSha512 $cached
        if ($actual -eq $ExpectedSha512.ToLowerInvariant()) {
            $resolvedArchive = $cached
            Write-Host "Using cached archive with verified checksum: $cached"
        } else {
            Write-Host "Cached archive checksum mismatch; re-downloading."
            Remove-Item -LiteralPath $cached -Force
        }
    }
    if (-not $resolvedArchive) {
        Write-Host "Downloading pinned Maven from official source..."
        Write-Host "  $DownloadUrl"
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $cached -UseBasicParsing
        $actual = Get-FileSha512 $cached
        if ($actual -ne $ExpectedSha512.ToLowerInvariant()) {
            Remove-Item -LiteralPath $cached -Force
            throw "Downloaded archive SHA-512 mismatch`n  expected: $ExpectedSha512`n  actual:   $actual"
        }
        Write-Host "Download checksum verified."
        $resolvedArchive = $cached
    }
}

$extractRoot = Join-Path $CacheDir "extract"
if (Test-Path -LiteralPath $extractRoot) {
    Remove-Item -LiteralPath $extractRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
Write-Host "Extracting archive..."
Expand-Archive -LiteralPath $resolvedArchive -DestinationPath $extractRoot -Force

$extractedHome = Get-ChildItem -LiteralPath $extractRoot -Directory | Select-Object -First 1
if (-not $extractedHome) {
    throw "Archive extraction produced no Maven directory."
}
Assert-MavenHome $extractedHome.FullName

if (Test-Path -LiteralPath $destinationFull) {
    Remove-Item -LiteralPath $destinationFull -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
Move-Item -LiteralPath $extractedHome.FullName -Destination $destinationFull
Assert-MavenHome $destinationFull
Write-MavenManifest $destinationFull "pinned-download" $ExpectedSha512

Remove-Item -LiteralPath $extractRoot -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "Staged Maven ready: $destinationFull"
Write-Host "Vendor=$Vendor Name=$Name Version=$Version Sha512=$ExpectedSha512"
