param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArgs)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  Write-Host 'Java 21 is missing. Install Temurin JDK 21, open a NEW terminal, and retry.' -ForegroundColor Red
  exit 1
}
$javacVersion = & javac -version 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host 'A full Java JDK (not only a JRE) is required.'; exit 1 }
Write-Host "Using $javacVersion"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) { & mvn @MavenArgs; exit $LASTEXITCODE }
$version = '3.9.11'
$folder = Join-Path $root ".tools\apache-maven-$version"
$executable = Join-Path $folder 'bin\mvn.cmd'
if (-not (Test-Path $executable)) {
  $toolsDir = Join-Path $root '.tools'
  New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
  $archive = Join-Path $toolsDir 'apache-maven.zip'
  $sources = @(
    "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip",
    "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
  )
  $ok=$false
  foreach($url in $sources) {
    try { Write-Host 'Downloading Maven automatically (first run only)...'; Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing; $ok=$true; break }
    catch { Write-Host ('Download source failed: ' + $url) }
  }
  if (-not $ok) { Write-Host 'Could not download Maven. Install Apache Maven, then retry.'; exit 1 }
  # SHA-512 from Apache's Maven 3.9.11 signed release voting announcement.
  $expected='03e2d65d4483a3396980629f260e25cac0d8b6f7f2791e4dc20bc83f9514db8d0f05b0479e699a5f34679250c49c8e52e961262ded468a20de0be254d8207076'
  $actual=(Get-FileHash -Path $archive -Algorithm SHA512).Hash.ToLowerInvariant()
  if ($actual -ne $expected) { Remove-Item $archive -Force; Write-Host 'Maven archive checksum failed.'; exit 1 }
  Expand-Archive -Path $archive -DestinationPath $toolsDir -Force
  Remove-Item $archive -Force
}
& $executable @MavenArgs
exit $LASTEXITCODE
