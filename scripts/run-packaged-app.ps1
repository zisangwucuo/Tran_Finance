$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $ProjectRoot "logs"
$MlServiceDir = Join-Path $ProjectRoot "ml-service"
$MlScript = Join-Path $ProjectRoot "scripts\start-ml-service.ps1"
$MlPythonExe = Join-Path $MlServiceDir ".venv\Scripts\python.exe"
$MlUvicornExe = Join-Path $MlServiceDir ".venv\Scripts\uvicorn.exe"
$MlRequirements = Join-Path $MlServiceDir "requirements.txt"

function Read-Value {
    param(
        [string]$PromptText,
        [string]$DefaultValue
    )

    $Value = Read-Host "$PromptText [$DefaultValue]"
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $DefaultValue
    }
    return $Value.Trim()
}

function Read-SecretValue {
    param(
        [string]$PromptText,
        [string]$DefaultValue
    )

    if ([string]::IsNullOrWhiteSpace($DefaultValue)) {
        $SecureValue = Read-Host $PromptText -AsSecureString
        $PlainPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
        try {
            return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($PlainPointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($PlainPointer)
        }
    }

    $Choice = Read-Host "$PromptText use default value? default Y, input N to type manually"
    if ([string]::IsNullOrWhiteSpace($Choice) -or $Choice.Trim().ToUpperInvariant() -ne "N") {
        return $DefaultValue
    }

    $SecureValue = Read-Host $PromptText -AsSecureString
    $PlainPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($PlainPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($PlainPointer)
    }
}

function Require-Command {
    param([string]$CommandName)

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Command not found: $CommandName. Please install it and add it to PATH."
    }
}

function Find-BackendJar {
    $SearchDirs = @(
        (Join-Path $ProjectRoot "backend\target"),
        (Join-Path $ProjectRoot "backend"),
        $ProjectRoot
    )

    foreach ($Dir in $SearchDirs) {
        if (Test-Path $Dir) {
            $Jar = Get-ChildItem -Path $Dir -Filter "trans-finance-backend-*.jar" -File |
                Where-Object { $_.Name -notlike "*.original" } |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
            if ($Jar) {
                return $Jar
            }
        }
    }
    return $null
}

function Test-PortOpen {
    param(
        [string]$HostName,
        [int]$Port
    )

    try {
        $Client = [System.Net.Sockets.TcpClient]::new()
        $AsyncResult = $Client.BeginConnect($HostName, $Port, $null, $null)
        $Connected = $AsyncResult.AsyncWaitHandle.WaitOne(1000, $false)
        if ($Connected) {
            $Client.EndConnect($AsyncResult)
        }
        $Client.Close()
        return $Connected
    } catch {
        return $false
    }
}

function Get-ListeningPortPids {
    param([int]$Port)

    $Pids = @()
    $Lines = netstat -ano | Where-Object { $_ -match "LISTENING" }
    foreach ($Line in $Lines) {
        $Parts = @($Line -split "\s+" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($Parts.Count -lt 5 -or $Parts[0] -ne "TCP") {
            continue
        }

        $LocalAddress = $Parts[1]
        $State = $Parts[3]
        $ProcessId = $Parts[4]
        if ($State -eq "LISTENING" -and $LocalAddress -match ":(\d+)$" -and [int]$Matches[1] -eq $Port) {
            $Pids += [int]$ProcessId
        }
    }

    return $Pids | Sort-Object -Unique
}

function Stop-ListeningPort {
    param([int]$Port)

    $Pids = @(Get-ListeningPortPids -Port $Port)
    if ($Pids.Count -eq 0) {
        Write-Host "Port $Port is free."
        return
    }

    foreach ($ProcessId in $Pids) {
        Write-Host "Releasing port $Port by stopping PID $ProcessId ..."
        & taskkill.exe /PID $ProcessId /F | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to stop PID $ProcessId on port $Port. Please close it manually or run as administrator."
        }
    }

    Start-Sleep -Seconds 1
    $RemainingPids = @(Get-ListeningPortPids -Port $Port)
    if ($RemainingPids.Count -gt 0) {
        throw "Port $Port is still occupied by PID(s): $($RemainingPids -join ', ')."
    }
}

function Ensure-MlEnvironment {
    if (-not (Test-Path $MlServiceDir)) {
        throw "ML service directory not found: $MlServiceDir"
    }

    if (-not (Test-Path $MlPythonExe)) {
        Write-Host "Python virtual environment not found. Creating ml-service\.venv ..."
        python -m venv (Join-Path $MlServiceDir ".venv")
    }

    if (-not (Test-Path $MlUvicornExe)) {
        Write-Host "ML dependencies not found. Installing requirements.txt ..."
        & $MlPythonExe -m pip install -r $MlRequirements
    }
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

Write-Host ""
Write-Host "=== Preflight ==="
Require-Command "java"
Require-Command "python"

$BackendJar = Find-BackendJar
if (-not $BackendJar) {
    throw "Backend jar not found. Run scripts\build-packaged-app.ps1 first, or put the jar in backend\target, backend, or project root."
}
Write-Host "Backend jar: $($BackendJar.FullName)"

Write-Host ""
Write-Host "=== Database ==="
$DbHost = Read-Value "DB host" "127.0.0.1"
$DbPort = Read-Value "DB port" "3306"
$DbName = Read-Value "DB name" "trans_finance"
$DbUsername = Read-Value "DB username" "root"
$DbPassword = Read-SecretValue "DB password" $env:DB_PASSWORD

if (-not (Test-PortOpen $DbHost ([int]$DbPort))) {
    throw "Cannot connect to MySQL at $DbHost`:$DbPort. Start MySQL and import TransformationalFinance.sql first."
}

$env:DB_HOST = $DbHost
$env:DB_PORT = $DbPort
$env:DB_NAME = $DbName
$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword
$env:ML_SERVICE_URL = "http://127.0.0.1:8090"
# COMPANY_DEFAULT_PASSWORD is optional and must be supplied by the operator.

Write-Host ""
Write-Host "=== AI narrative ==="
$EnableAi = Read-Host "Enable Doubao AI narrative? default N, input Y to enable"
if (-not [string]::IsNullOrWhiteSpace($EnableAi) -and $EnableAi.Trim().ToUpperInvariant() -eq "Y") {
    $env:SPRING_PROFILES_ACTIVE = "doubao"
    $env:AI_NARRATIVE_ENABLED = "true"
    $env:AI_NARRATIVE_RESPONSE_FORMAT_JSON = "false"
    $env:AI_NARRATIVE_DEEPSEEK_THINKING_DISABLED = "true"
    $env:AI_NARRATIVE_API_URL = Read-Value "Doubao API URL" "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
    $env:ARK_MODEL = Read-Value "Doubao model" ""
    $env:ARK_API_KEY = Read-SecretValue "Doubao API key" ""
    if ([string]::IsNullOrWhiteSpace($env:ARK_MODEL) -or [string]::IsNullOrWhiteSpace($env:ARK_API_KEY)) {
        throw "Doubao is enabled, but model or API key is empty."
    }
    Write-Host "AI narrative enabled. Make sure the model name is a valid Doubao model identifier."
} else {
    $env:SPRING_PROFILES_ACTIVE = ""
    $env:AI_NARRATIVE_ENABLED = "false"
    Write-Host "AI narrative disabled. Reports will use rule-based text."
}

Write-Host ""
Write-Host "=== ML service ==="
Ensure-MlEnvironment
if (Test-PortOpen "127.0.0.1" 8090) {
    Write-Host "Port 8090 is already in use. Skip starting ML service."
} else {
    Write-Host "Starting ML service. Logs: logs\ml-service.log, logs\ml-service.err"
    Start-Process `
        -FilePath "powershell" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $MlScript, "-SkipEnvFile") `
        -WorkingDirectory $ProjectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir "ml-service.log") `
        -RedirectStandardError (Join-Path $LogDir "ml-service.err")
}

Write-Host ""
Write-Host "=== Backend and web page ==="
Stop-ListeningPort -Port 8080
Write-Host "Starting backend jar. Logs: logs\backend.log, logs\backend.err"
Start-Process `
    -FilePath "java" `
    -ArgumentList @("-jar", $BackendJar.FullName) `
    -WorkingDirectory $BackendJar.DirectoryName `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $LogDir "backend.log") `
    -RedirectStandardError (Join-Path $LogDir "backend.err")

Write-Host "Waiting for services..."
Start-Sleep -Seconds 8

Start-Process "http://localhost:8080/login"
Write-Host ""
Write-Host "Started. Open http://localhost:8080/login"
Write-Host "If there is no data, import TransformationalFinance.sql into MySQL first."
