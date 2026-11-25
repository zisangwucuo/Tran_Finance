$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $ProjectRoot "frontend"
$BackendDir = Join-Path $ProjectRoot "backend"

Write-Host "Building frontend production assets..."
Push-Location $FrontendDir
try {
    npm run build
} finally {
    Pop-Location
}

Write-Host "Building backend jar with bundled frontend assets..."
Push-Location $BackendDir
try {
    mvn -DskipTests package
} finally {
    Pop-Location
}

Write-Host "Packaged build completed."
Write-Host "Backend jar: backend\target\trans-finance-backend-0.0.1-SNAPSHOT.jar"
