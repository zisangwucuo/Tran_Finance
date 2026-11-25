$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Runtime = Join-Path $ProjectRoot ".runtime\psi"
$Entry = Join-Path $ProjectRoot "psi-service\serve.py"
foreach ($Bank in @("bank_a", "bank_b", "bank_c")) {
    $RecordPath = Join-Path $Runtime "$Bank.process.json"
    if (-not (Test-Path $RecordPath)) { continue }
    $Record = Get-Content -Raw $RecordPath | ConvertFrom-Json
    $Process = Get-Process -Id $Record.id -ErrorAction SilentlyContinue
    if (-not $Process) { continue }
    $Command = (Get-CimInstance Win32_Process -Filter "ProcessId=$($Record.id)").CommandLine
    # PID reuse must not cause an unrelated program to be terminated.
    if ($Process.StartTime.ToUniversalTime().ToString("o") -ne $Record.started -or -not $Command.Contains($Entry) -or -not $Command.Contains($Bank)) {
        Write-Warning "$Bank process identity changed; skipped."
        continue
    }
    Stop-Process -Id $Record.id
    Write-Host "$Bank stopped."
}
