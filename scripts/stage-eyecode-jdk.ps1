# Stages the pinned EyeCode Desktop JDK into target/eyecode-desktop/runtime/jdk.
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -ArchivePath path\to\OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -JdkHome C:\path\to\extracted-jdk
param(
    [string]$Destination,
    [string]$ArchivePath,
    [string]$JdkHome,
    [string]$CacheDir
)

$ErrorActionPreference = "Stop"

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$RuntimeDir = Join-Path (Join-Path (Join-Path (Join-Path $RepoRoot "target") "eyecode-desktop") "runtime") $null
$RuntimeDir = [System.IO.Path]::Combine($RepoRoot, "target", "eyecode-desktop", "runtime")
if (-not $Destination) { $Destination = Join-Path $RuntimeDir "jdk" }
if (-not $CacheDir) { $CacheDir = Join-Path $RuntimeDir "jdk-cache" }

$Vendor = "Eclipse Temurin"
$Version = "21.0.12.1+1"
$MajorVersion = 21
$Architecture = "x64"
$OsName = "Windows"
$ArchiveName = "OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
$DownloadUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
$ExpectedSha256 = "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"

function Get-FileSha256([string]$Path) {
    (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Test-IsWindows {
    [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [System.Runtime.InteropServices.OSPlatform]::Windows)
}

function Assert-JdkHome([string]$HomePath) {
    if (-not (Test-Path -LiteralPath $HomePath -PathType Container)) {
        throw "JDK home does not exist or is not a directory: $HomePath"
    }
    $javaExe = Join-Path (Join-Path $HomePath "bin") "java.exe"
    $javacExe = Join-Path (Join-Path $HomePath "bin") "javac.exe"
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        throw "JDK home is missing bin\java.exe (not a usable JDK): $HomePath"
    }
    if (-not (Test-Path -LiteralPath $javacExe -PathType Leaf)) {
        throw "JDK home is missing bin\javac.exe (JRE-only is not a usable Java development toolchain): $HomePath"
    }
    $releasePath = Join-Path $HomePath "release"
    if (Test-Path -LiteralPath $releasePath -PathType Leaf) {
        $release = Get-Content -LiteralPath $releasePath -Raw
        if ($release -notmatch 'JAVA_VERSION="21') {
            throw "JDK release metadata does not report JAVA_VERSION 21: $HomePath"
        }
        if ($release -notmatch 'OS_ARCH="x86_64"') {
            throw "JDK release metadata does not report OS_ARCH x86_64: $HomePath"
        }
    }
    $previousEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $javaVersion = (& $javaExe -version 2>&1 | Out-String)
        $javacVersion = (& $javacExe -version 2>&1 | Out-String)
    } finally {
        $ErrorActionPreference = $previousEap
    }
    if ($javaVersion -notmatch 'version "21') {
        throw "java -version did not report major version 21: $javaVersion"
    }
    if ($javacVersion -notmatch 'javac 21') {
        throw "javac -version did not report major version 21: $javacVersion"
    }
}

function Write-ToolchainManifest([string]$HomePath, [string]$Source, [string]$ArchiveSha) {
    $parent = Split-Path -Parent $HomePath
    $manifestPath = Join-Path $parent "jdk-manifest.json"
    $manifest = [ordered]@{
        vendor = $Vendor
        version = $Version
        majorVersion = $MajorVersion
        architecture = $Architecture
        osName = $OsName
        javaExecutable = (Join-Path (Join-Path $HomePath "bin") "java.exe")
        javacExecutable = (Join-Path (Join-Path $HomePath "bin") "javac.exe")
        home = $HomePath
        source = $Source
        archiveSha256 = $ArchiveSha
        stagedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        downloadUrl = $DownloadUrl
        licenseNotice = "Eclipse Temurin is licensed under the GPLv2 with Classpath Exception. See https://adoptium.net/license/ and the JDK legal/ directory."
    }
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Write-Host "Manifest written: $manifestPath"
}

if (-not (Test-IsWindows)) {
    throw "This checkpoint stages a Windows x64 JDK. Run the script on Windows."
}

$destinationFull = [System.IO.Path]::GetFullPath($Destination)
$parentDir = Split-Path -Parent $destinationFull

if ($JdkHome) {
    Write-Host "Validating configured JDK home: $JdkHome"
    Assert-JdkHome $JdkHome
    if (Test-Path -LiteralPath $destinationFull) {
        Remove-Item -LiteralPath $destinationFull -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
    Copy-Item -LiteralPath $JdkHome -Destination $destinationFull -Recurse -Force
    Assert-JdkHome $destinationFull
    Write-ToolchainManifest $destinationFull "configured-jdk-home" ""
    Write-Host "Staged JDK (from -JdkHome): $destinationFull"
    exit 0
}

$resolvedArchive = $null
if ($ArchivePath) {
    $resolvedArchive = [System.IO.Path]::GetFullPath($ArchivePath)
    if (-not (Test-Path -LiteralPath $resolvedArchive -PathType Leaf)) {
        throw "Archive not found: $resolvedArchive"
    }
    $actual = Get-FileSha256 $resolvedArchive
    if ($actual -ne $ExpectedSha256.ToLowerInvariant()) {
        throw "Archive SHA-256 mismatch for $resolvedArchive`n  expected: $ExpectedSha256`n  actual:   $actual"
    }
    Write-Host "Archive checksum verified: $resolvedArchive"
} else {
    New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null
    $cached = Join-Path $CacheDir $ArchiveName
    if (Test-Path -LiteralPath $cached -PathType Leaf) {
        $actual = Get-FileSha256 $cached
        if ($actual -eq $ExpectedSha256.ToLowerInvariant()) {
            $resolvedArchive = $cached
            Write-Host "Using cached archive with verified checksum: $cached"
        } else {
            Write-Host "Cached archive checksum mismatch; re-downloading."
            Remove-Item -LiteralPath $cached -Force
        }
    }
    if (-not $resolvedArchive) {
        Write-Host "Downloading pinned JDK from official source..."
        Write-Host "  $DownloadUrl"
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $cached -UseBasicParsing
        $actual = Get-FileSha256 $cached
        if ($actual -ne $ExpectedSha256.ToLowerInvariant()) {
            Remove-Item -LiteralPath $cached -Force
            throw "Downloaded archive SHA-256 mismatch`n  expected: $ExpectedSha256`n  actual:   $actual"
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
    throw "Archive extraction produced no JDK directory."
}
Assert-JdkHome $extractedHome.FullName

if (Test-Path -LiteralPath $destinationFull) {
    Remove-Item -LiteralPath $destinationFull -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
Move-Item -LiteralPath $extractedHome.FullName -Destination $destinationFull
Assert-JdkHome $destinationFull
Write-ToolchainManifest $destinationFull "pinned-download" $ExpectedSha256

Remove-Item -LiteralPath $extractRoot -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "Staged JDK ready: $destinationFull"
Write-Host "Vendor=$Vendor Version=$Version Arch=$Architecture Sha256=$ExpectedSha256"
