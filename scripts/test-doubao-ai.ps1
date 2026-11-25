$ErrorActionPreference = "Stop"

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
    param([string]$PromptText)

    $SecureValue = Read-Host $PromptText -AsSecureString
    $PlainPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($PlainPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($PlainPointer)
    }
}

Write-Host ""
Write-Host "=== Doubao Ark Chat Completions Test ==="
Write-Host "This script only tests connectivity and request parameters. It does not save your API key."
Write-Host ""

$ApiUrl = Read-Value "Doubao API URL" "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
$Model = Read-Value "Doubao Model ID or Endpoint ID" ""
$ApiKey = Read-SecretValue "Doubao API key"
$DisableThinking = Read-Value "Disable thinking parameter? input Y for DeepSeek V4 endpoint" "Y"

if ([string]::IsNullOrWhiteSpace($Model)) {
    throw "Model ID or Endpoint ID is empty."
}
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "API key is empty."
}

$PayloadObject = @{
    model = $Model
    messages = @(
        @{
            role = "user"
            content = "请只回复一个合法 JSON：{""ok"":true,""message"":""连接成功""}"
        }
    )
    temperature = 0.1
    max_tokens = 200
}

if ([string]::IsNullOrWhiteSpace($DisableThinking) -or $DisableThinking.Trim().ToUpperInvariant() -eq "Y") {
    $PayloadObject.thinking = @{ type = "disabled" }
}

$Payload = $PayloadObject | ConvertTo-Json -Depth 10

try {
    $Response = Invoke-WebRequest `
        -Uri $ApiUrl `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Headers @{ Authorization = "Bearer $ApiKey"; Accept = "application/json" } `
        -Body $Payload `
        -TimeoutSec 60

    Write-Host ""
    Write-Host "Request succeeded. Status: $($Response.StatusCode)"
    Write-Host "Response:"
    Write-Host $Response.Content
} catch {
    Write-Host ""
    Write-Host "Request failed."
    if ($_.Exception.Response) {
        Write-Host "Status: $([int]$_.Exception.Response.StatusCode)"
        try {
            $Reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $Body = $Reader.ReadToEnd()
            $Reader.Close()
            Write-Host "Response body:"
            Write-Host $Body
        } catch {
            Write-Host "Response body cannot be read."
        }
    } else {
        Write-Host $_.Exception.Message
    }
    throw
}
