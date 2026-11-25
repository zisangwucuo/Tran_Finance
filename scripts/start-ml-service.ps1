param(
    [switch]$SkipEnvFile
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$MlServiceDir = Join-Path $ProjectRoot "ml-service"
$EnvFile = Join-Path $MlServiceDir ".env.local"
$PythonExe = Join-Path $MlServiceDir ".venv\Scripts\python.exe"
$UvicornExe = Join-Path $MlServiceDir ".venv\Scripts\uvicorn.exe"

function Import-LocalEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    Get-Content $Path | ForEach-Object {
        $Line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($Line) -or $Line.StartsWith("#")) {
            return
        }

        $Parts = $Line.Split("=", 2)
        if ($Parts.Length -ne 2) {
            return
        }

        $Name = $Parts[0].Trim()
        $Value = $Parts[1].Trim()
        if (($Value.StartsWith('"') -and $Value.EndsWith('"')) -or ($Value.StartsWith("'") -and $Value.EndsWith("'"))) {
            $Value = $Value.Substring(1, $Value.Length - 2)
        }

        if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
            Set-Item -Path "Env:$Name" -Value $Value
        }
    }
}

if (-not $SkipEnvFile) {
    Import-LocalEnv $EnvFile
}

$RequiredEnvNames = @("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD")
foreach ($Name in $RequiredEnvNames) {
    if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
        Write-Host "Missing environment variable: $Name"
        Write-Host "Set it in the current terminal or create ml-service\.env.local."
        exit 1
    }
}

if (-not (Test-Path $PythonExe)) {
    Write-Host "Python virtual environment not found: $PythonExe"
    Write-Host "Run this first from project root: python -m venv ml-service\.venv"
    exit 1
}

if (-not (Test-Path $UvicornExe)) {
    Write-Host "uvicorn is not installed. Install ml-service dependencies first:"
    Write-Host ".\ml-service\.venv\Scripts\python.exe -m pip install -r .\ml-service\requirements.txt"
    exit 1
}

Write-Host "Starting Python ML service: http://127.0.0.1:8090"
& $UvicornExe app:app --host 127.0.0.1 --port 8090 --app-dir $MlServiceDir
