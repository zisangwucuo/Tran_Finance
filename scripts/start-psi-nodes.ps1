param([switch]$PrepareOnly)
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Runtime = Join-Path $ProjectRoot ".runtime\psi"
$Python = Join-Path $ProjectRoot "ml-service\.venv\Scripts\python.exe"
$Engine = Join-Path $ProjectRoot "psi-service\java\target\psi-engine.jar"
if (-not (Test-Path $Python)) { throw "Install the existing ML service Python environment first." }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Java 17 is required." }
& $Python -c "import fastapi, uvicorn, pymysql, dotenv, cryptography"
if ($LASTEXITCODE -ne 0) {
    throw "Missing PSI dependencies. Run ml-service\.venv\Scripts\python.exe -m pip install -r psi-service\requirements.txt"
}
if (-not (Test-Path $Engine)) {
    & (Join-Path $PSScriptRoot "build-psi-engine.ps1")
    if (-not (Test-Path $Engine)) { throw "PSI engine build failed." }
}
New-Item -ItemType Directory -Force -Path $Runtime | Out-Null
# Credentials inherit access only for this Windows user and SYSTEM.
$Sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $Runtime /inheritance:r /grant:r "*$($Sid):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Cannot protect PSI runtime credentials." }
& $Python (Join-Path $ProjectRoot "psi-service\provision.py")
if ($LASTEXITCODE -ne 0) { throw "PSI database provisioning failed." }
if ($PrepareOnly) { return }
$Entry = Join-Path $ProjectRoot "psi-service\serve.py"
$Started = @()
try {
    foreach ($Bank in @("bank_a", "bank_b", "bank_c")) {
        $Config = Get-Content -Raw -Encoding UTF8 (Join-Path $Runtime "$Bank.json") | ConvertFrom-Json
        $Port = $Config.port
        $Listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($Listener) {
            # Never kill an unrelated process or assume an occupied port belongs to us.
            try {
                $Health = Invoke-RestMethod "http://127.0.0.1:$Port/health" -Headers @{Authorization = "Bearer " + $Config.adminToken} -TimeoutSec 4
                if ($Health.institutionId -eq $Bank -and $Health.status -eq "READY" -and $Health.protocol -eq "MPC4J-1.1.5-HFH99-ECC-FIELDS-v2") {
                    Write-Host "$Bank already ready on $Port"
                    continue
                }
            } catch { }
            throw "Port $Port is occupied by an unverified service. Stop it manually."
        }
        $Process = Start-Process -FilePath $Python -ArgumentList @('"' + $Entry + '"', $Bank) -WorkingDirectory (Split-Path $Entry) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $Runtime "$Bank.out.log") -RedirectStandardError (Join-Path $Runtime "$Bank.err.log")
        $Started += $Process
        @{id = $Process.Id; started = $Process.StartTime.ToUniversalTime().ToString("o"); bank = $Bank} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $Runtime "$Bank.process.json")
        $Ready = $false
        for ($Attempt = 0; $Attempt -lt 20; $Attempt++) {
            if ($Process.HasExited) { throw "$Bank exited. Check its runtime error log." }
            try {
                $Health = Invoke-RestMethod "http://127.0.0.1:$Port/health" -Headers @{Authorization = "Bearer " + $Config.adminToken} -TimeoutSec 2
                if ($Health.institutionId -eq $Bank -and $Health.status -eq "READY") { $Ready = $true; break }
            } catch { }
            Start-Sleep -Milliseconds 300
        }
        if (-not $Ready) { throw "$Bank did not become ready." }
        Write-Host "$Bank ready on 127.0.0.1:$Port"
    }
} catch {
    # Roll back only processes started by this invocation, not existing nodes.
    foreach ($Process in $Started) { if (-not $Process.HasExited) { Stop-Process -Id $Process.Id -ErrorAction SilentlyContinue } }
    throw
}
Write-Host "Three Windows PSI nodes ready. Re-login to use the PSI page."
