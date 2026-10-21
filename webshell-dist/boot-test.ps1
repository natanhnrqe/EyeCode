$ErrorActionPreference = 'Stop'
$dist = (Resolve-Path "webshell-dist\target\webshell-dist").Path
$java = "C:\Users\JoyBoy\.jdks\jbrsdk_jcef-21.0.11\bin\java.exe"
$log = "$env:TEMP\eyecode-cp1-boot.log"
$err = "$env:TEMP\eyecode-cp1-boot.err"
Remove-Item $log, $err -ErrorAction SilentlyContinue

# kill leftover java from prior run
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.StartTime -gt (Get-Date).AddMinutes(-10) } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

$proc = Start-Process -FilePath $java `
  -ArgumentList "-cp", "`"$dist\eyecode-webshell.jar;$dist\lib\*`"", "com.eyecode.ui.web.LocalWebShellLauncher" `
  -RedirectStandardOutput $log -RedirectStandardError $err -PassThru -WindowStyle Hidden
Write-Host "PID=$($proc.Id)"

$deadline = (Get-Date).AddSeconds(30)
$url = $null
while ((Get-Date) -lt $deadline) {
  if (Test-Path $log) {
    $line = Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -match '\[EyeCode\] URL:' } | Select-Object -First 1
    if ($line) { $url = ($line -replace '.*URL:\s*', '').Trim(); break }
  }
  Start-Sleep -Milliseconds 400
}
if (-not $url) {
  Write-Host "BOOT_FAIL: no readiness line"
  Write-Host "=== STDOUT ==="; Get-Content $log -ErrorAction SilentlyContinue
  Write-Host "=== STDERR ==="; Get-Content $err -ErrorAction SilentlyContinue
  Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
  exit 1
}
Write-Host "URL=$url"

$base = ([Uri]$url).GetLeftPart([UriPartial].Authority)
$checks = @()
foreach ($path in @('/webshell/index.html', '/monaco/editor/vs/loader.js', '/documentation/catalog.txt')) {
  try {
    $r = Invoke-WebRequest -Uri "$base$path" -UseBasicParsing -TimeoutSec 10
    $checks += [PSCustomObject]@{ Path = $path; Status = $r.StatusCode; Len = $r.RawContentLength }
  } catch {
    $checks += [PSCustomObject]@{ Path = $path; Status = "FAIL"; Len = 0; Error = $_.Exception.Message }
  }
}
$checks | Format-Table -AutoSize

$fail = $checks | Where-Object { $_.Status -ne 200 }
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

if ($fail) { Write-Host "BOOT_FAIL"; exit 1 }
Write-Host "BOOT_OK url=$url"
