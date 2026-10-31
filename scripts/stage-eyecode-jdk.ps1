# Stages the pinned EyeCode Desktop JDK into target/eyecode-desktop/runtime/jdk.
# Supports cross-staging every desktop target from any host with PowerShell + tar:
#   win32/x64 (zip), linux/x64, darwin/x64, darwin/arm64 (tar.gz).
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -Platform linux -Arch x64
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -Platform darwin -Arch arm64
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -ArchivePath path\to\archive.zip
#   powershell -ExecutionPolicy Bypass -File scripts/stage-eyecode-jdk.ps1 -JdkHome C:\path\to\extracted-jdk
param(
    [ValidateSet("win32", "linux", "darwin")]
    [string]$Platform = "win32",
    [ValidateSet("x64", "arm64")]
    [string]$Arch = "x64",
    [string]$Destination,
    [string]$ArchivePath,
    [string]$JdkHome,
    [string]$CacheDir
)

$ErrorActionPreference = "Stop"

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$RuntimeDir = [System.IO.Path]::Combine($RepoRoot, "target", "eyecode-desktop", "runtime")
if (-not $Destination) { $Destination = Join-Path $RuntimeDir "jdk" }

$Vendor = "Eclipse Temurin"
$Version = "21.0.12.1+1"
$MajorVersion = 21
$ReleaseBase = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1"

$Pins = @{
    "win32/x64" = @{
        ArchiveName = "OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
        Sha256      = "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"
        OsName      = "Windows"
        OsArch      = "x86_64"
    }
    "linux/x64" = @{
        ArchiveName = "OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
        Sha256      = "ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"
        OsName      = "Linux"
        OsArch      = "x86_64"
    }
    "darwin/x64" = @{
        ArchiveName = "OpenJDK21U-jdk_x64_mac_hotspot_21.0.12.1_1.tar.gz"
        Sha256      = "44db0f08196daf19a47f90d13388b0c943b67663cb537f998fe29e836fa842ce"
        OsName      = "Mac"
        OsArch      = "x86_64"
    }
    "darwin/arm64" = @{
        ArchiveName = "OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz"
        Sha256      = "3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998"
        OsName      = "Mac"
        OsArch      = "aarch64"
    }
}

$pinKey = "$Platform/$Arch"
if (-not $Pins.ContainsKey($pinKey)) {
    throw "Unsupported platform/architecture combination: $pinKey. Supported: $($Pins.Keys -join ', ')"
}
$Pin = $Pins[$pinKey]
$ArchiveName = $Pin.ArchiveName
$ExpectedSha256 = $Pin.Sha256
$OsName = $Pin.OsName
$ExpectedOsArch = $Pin.OsArch
$DownloadUrl = "$ReleaseBase/$ArchiveName"
if (-not $CacheDir) { $CacheDir = Join-Path $RuntimeDir "jdk-cache" }

function Get-FileSha256([string]$Path) {
    (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Test-IsWindows {
    [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [System.Runtime.InteropServices.OSPlatform]::Windows)
}

function Get-JdkExecutableSuffix([string]$TargetPlatform) {
    if ($TargetPlatform -eq "win32") { return ".exe" }
    return ""
}

function Assert-JdkHome([string]$HomePath, [string]$TargetPlatform) {
    if (-not (Test-Path -LiteralPath $HomePath -PathType Container)) {
        throw "JDK home does not exist or is not a directory: $HomePath"
    }
    $suffix = Get-JdkExecutableSuffix $TargetPlatform
    $javaExe = Join-Path (Join-Path $HomePath "bin") "java$suffix"
    $javacExe = Join-Path (Join-Path $HomePath "bin") "javac$suffix"
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        throw "JDK home is missing bin/java$suffix (not a usable JDK): $HomePath"
    }
    if (-not (Test-Path -LiteralPath $javacExe -PathType Leaf)) {
        throw "JDK home is missing bin/javac$suffix (JRE-only is not a usable Java development toolchain): $HomePath"
    }
    $releasePath = Join-Path $HomePath "release"
    if (Test-Path -LiteralPath $releasePath -PathType Leaf) {
        $release = Get-Content -LiteralPath $releasePath -Raw
        if ($release -notmatch 'JAVA_VERSION="21') {
            throw "JDK release metadata does not report JAVA_VERSION 21: $HomePath"
        }
        if ($release -notmatch ([regex]::Escape("OS_ARCH=`"$ExpectedOsArch`""))) {
            throw "JDK release metadata does not report OS_ARCH ${ExpectedOsArch}: $HomePath"
        }
    }
    if ((Test-IsWindows) -and $TargetPlatform -eq "win32") {
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
}

function Write-ToolchainManifest([string]$HomePath, [string]$Source, [string]$ArchiveSha) {
    $parent = Split-Path -Parent $HomePath
    $manifestPath = Join-Path $parent "jdk-manifest.json"
    $suffix = Get-JdkExecutableSuffix $Platform
    $manifest = [ordered]@{
        vendor           = $Vendor
        version          = $Version
        majorVersion     = $MajorVersion
        platform         = $Platform
        architecture     = $Arch
        osName           = $OsName
        javaExecutable   = (Join-Path (Join-Path $HomePath "bin") "java$suffix")
        javacExecutable  = (Join-Path (Join-Path $HomePath "bin") "javac$suffix")
        home             = $HomePath
        source           = $Source
        archiveSha256    = $ArchiveSha
        stagedAtUtc      = (Get-Date).ToUniversalTime().ToString("o")
        downloadUrl      = $DownloadUrl
        licenseNotice    = "Eclipse Temurin is licensed under the GPLv2 with Classpath Exception. See https://adoptium.net/license/ and the JDK legal/ directory."
    }
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Write-Host "Manifest written: $manifestPath"
}

$destinationFull = [System.IO.Path]::GetFullPath($Destination)
$parentDir = Split-Path -Parent $destinationFull

if ($JdkHome) {
    Write-Host "Validating configured JDK home: $JdkHome"
    Assert-JdkHome $JdkHome $Platform
    if (Test-Path -LiteralPath $destinationFull) {
        Remove-Item -LiteralPath $destinationFull -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
    Copy-Item -LiteralPath $JdkHome -Destination $destinationFull -Recurse -Force
    Assert-JdkHome $destinationFull $Platform
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
        Write-Host "Downloading pinned JDK for $pinKey from official source..."
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

$extractRoot = Join-Path $CacheDir "extract-$pinKey"
if (Test-Path -LiteralPath $extractRoot) {
    Remove-Item -LiteralPath $extractRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
Write-Host "Extracting archive..."
if ($ArchiveName.EndsWith(".zip")) {
    Expand-Archive -LiteralPath $resolvedArchive -DestinationPath $extractRoot -Force
} else {
    # Tarballs for linux/darwin contain POSIX symlinks; Windows tar cannot create
    # them without developer mode. The python helper materializes links as copies.
    $extractHelper = Join-Path $PSScriptRoot "extract-tarball.py"
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) { $python = Get-Command python3 -ErrorAction SilentlyContinue }
    if ($python) {
        & $python.Source $extractHelper $resolvedArchive $extractRoot
        if ($LASTEXITCODE -ne 0) {
            throw "tarball extraction helper failed with exit code $LASTEXITCODE for $resolvedArchive"
        }
    } else {
        & tar -xzf $resolvedArchive -C $extractRoot
        if ($LASTEXITCODE -ne 0) {
            throw "tar extraction failed with exit code $LASTEXITCODE for $resolvedArchive (install python to materialize POSIX symlinks on Windows)"
        }
    }
}

$topEntries = Get-ChildItem -LiteralPath $extractRoot
$extractedHome = $null
foreach ($entry in $topEntries) {
    if ($entry.PSIsContainer) {
        $contentsHome = Join-Path $entry.FullName "Contents/Home"
        if (Test-Path -LiteralPath $contentsHome -PathType Container) {
            $extractedHome = Get-Item -LiteralPath $contentsHome
            break
        }
        $extractedHome = $entry
        break
    }
}
if (-not $extractedHome) {
    throw "Archive extraction produced no JDK directory."
}
Assert-JdkHome $extractedHome.FullName $Platform

if (Test-Path -LiteralPath $destinationFull) {
    Remove-Item -LiteralPath $destinationFull -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $parentDir | Out-Null
Move-Item -LiteralPath $extractedHome.FullName -Destination $destinationFull
Assert-JdkHome $destinationFull $Platform
Write-ToolchainManifest $destinationFull "pinned-download" $ExpectedSha256

Remove-Item -LiteralPath $extractRoot -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "Staged JDK ready: $destinationFull"
Write-Host "Vendor=$Vendor Version=$Version Target=$pinKey Sha256=$ExpectedSha256"
