$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Vendor = Join-Path $ProjectRoot "psi-service\.vendor"
$Commit = "ea3b9aa99389adbe6219b0b909d2cc39cacad2bd"
$Archive = Join-Path $Vendor "mpc4j-1.1.5.zip"
$SourceRoot = Join-Path $Vendor "source"
$Source = Join-Path $SourceRoot "mpc4j-$Commit"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "Building PSI requires Maven and JDK 17." }
New-Item -ItemType Directory -Force -Path $Vendor | Out-Null
if (-not (Test-Path (Join-Path $Source "pom.xml"))) {
    if (-not (Test-Path $Archive)) {
        Invoke-WebRequest "https://codeload.github.com/alibaba-edu/mpc4j/zip/$Commit" -OutFile $Archive
    }
    $Expected = "954F9947F9B1EA9C574063F39DF17BB3C8C0359C46E7C9907BCD93A673427B10"
    if ((Get-FileHash $Archive -Algorithm SHA256).Hash -ne $Expected) { throw "MPC4J source checksum mismatch; do not build." }
    Expand-Archive -LiteralPath $Archive -DestinationPath $SourceRoot -Force
}
# Only build Java modules needed by PSI. No native C/C++ library is compiled.
& mvn -q -f (Join-Path $Source "pom.xml") -pl mpc4j-s2pc-pso -am install '-Dmaven.test.skip=true' '-Dassembly.skipAssembly=true' '-Dmaven.source.skip=true'
if ($LASTEXITCODE -ne 0) { throw "MPC4J build failed." }
& mvn -q -f (Join-Path $ProjectRoot "psi-service\java\pom.xml") package
if ($LASTEXITCODE -ne 0) { throw "PSI engine build failed." }
Write-Host "Windows PSI engine built. No Docker or WSL required."
