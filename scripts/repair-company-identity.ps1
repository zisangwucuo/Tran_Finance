param([switch]$Apply)
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Backup = Join-Path $ProjectRoot ".runtime\company-identity"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
$Sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $Backup /inheritance:r /grant:r "*$($Sid):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Cannot protect identity migration backup." }
$Python = Join-Path $ProjectRoot "ml-service\.venv\Scripts\python.exe"
$Script = Join-Path $ProjectRoot "scripts\repair_company_identity.py"
if ($Apply) { & $Python $Script --apply } else { & $Python $Script }
if ($LASTEXITCODE -ne 0) { throw "Identity migration failed; no partial transaction was committed." }
