function Get-DevServiceDefinitions {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    return @(
        [pscustomobject]@{
            Name = 'backend'
            Command = 'mvn'
            Arguments = @('spring-boot:run')
            Port = 8080
            WorkingDirectory = Join-Path $ProjectRoot 'server'
            Environment = @{
                BOOTSTRAP_ADMIN_USERNAME = 'admin'
                BOOTSTRAP_ADMIN_PASSWORD = 'LocalAdmin123!'
                BOOTSTRAP_ADMIN_DISPLAY_NAME = 'Local Admin'
                WECHAT_MOCK_ENABLED = 'true'
            }
        }
        [pscustomobject]@{
            Name = 'admin'
            Command = 'npm.cmd'
            Arguments = @('run', 'dev')
            Port = 5173
            WorkingDirectory = Join-Path $ProjectRoot 'admin-web'
            Environment = @{}
        }
        [pscustomobject]@{
            Name = 'miniapp'
            Command = 'npm.cmd'
            Arguments = @('run', 'dev:mp-weixin')
            Port = $null
            WorkingDirectory = Join-Path $ProjectRoot 'miniapp'
            Environment = @{
                VITE_API_BASE_URL = 'http://127.0.0.1:8080/api'
                VITE_WECHAT_MOCK_ENABLED = 'true'
            }
        }
    )
}

function Get-DevRuntimePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    return Join-Path $ProjectRoot '.dev-runtime\processes.json'
}

function New-DevRuntimeState {
    return [pscustomobject]@{
        backend = $null
        admin = $null
        miniapp = $null
    }
}

function Test-DevCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-DevDockerReachable {
    if (-not (Test-DevCommand -Name 'docker')) {
        return $false
    }

    & docker info --format '{{.ServerVersion}}' *> $null
    return $LASTEXITCODE -eq 0
}

function Test-DevTcpPort {
    param(
        [string]$ComputerName = '127.0.0.1',

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [int]$TimeoutMilliseconds = 1000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    $result = $null
    try {
        $result = $client.BeginConnect($ComputerName, $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne($TimeoutMilliseconds, $false)) {
            return $false
        }

        $client.EndConnect($result)
        return $true
    }
    catch {
        return $false
    }
    finally {
        if ($null -ne $result) {
            $result.AsyncWaitHandle.Close()
        }
        $client.Close()
    }
}

function Test-DevHttp {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [int]$TimeoutSeconds = 2
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -Method Get -TimeoutSec $TimeoutSeconds -UseBasicParsing
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
    }
    catch {
        return $false
    }
}

function Wait-DevHttp {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [int]$TimeoutSeconds = 60
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($true) {
        $remaining = $deadline - [DateTime]::UtcNow
        if ($remaining -le [TimeSpan]::Zero) {
            return $false
        }

        $requestTimeoutSeconds = [int][Math]::Floor([Math]::Min(2, $remaining.TotalSeconds))
        if ($requestTimeoutSeconds -lt 1) {
            $tailSleepMilliseconds = [int][Math]::Floor($remaining.TotalMilliseconds)
            if ($tailSleepMilliseconds -gt 0) {
                Start-Sleep -Milliseconds $tailSleepMilliseconds
            }
            return $false
        }

        if (Test-DevHttp -Uri $Uri -TimeoutSeconds $requestTimeoutSeconds) {
            return $true
        }

        $remaining = $deadline - [DateTime]::UtcNow
        if ($remaining -le [TimeSpan]::Zero) {
            return $false
        }

        $sleepMilliseconds = [int][Math]::Min(1000, [Math]::Floor($remaining.TotalMilliseconds))
        if ($sleepMilliseconds -gt 0) {
            Start-Sleep -Milliseconds $sleepMilliseconds
        }
    }
}

function Read-DevRuntime {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return New-DevRuntimeState
    }

    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    }
    catch {
        Write-Warning "Ignoring invalid launcher runtime state at $Path"
        return New-DevRuntimeState
    }
}

function Write-DevRuntime {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        $State
    )

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $State | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Get-DevRuntimeEntry {
    param(
        [Parameter(Mandatory = $true)]
        $State,

        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $property = $State.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Test-DevProcessAlive {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId,

        [string]$ExpectedStartTimeUtc
    )

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return $false
    }

    if ([string]::IsNullOrWhiteSpace($ExpectedStartTimeUtc)) {
        return $true
    }

    try {
        $expected = [DateTime]::Parse($ExpectedStartTimeUtc).ToUniversalTime()
        $actual = $process.StartTime.ToUniversalTime()
        return [Math]::Abs(($actual - $expected).TotalSeconds) -lt 1
    }
    catch {
        return $false
    }
}

function Test-DevRuntimeEntryAlive {
    param($Entry)

    if (
        $null -eq $Entry -or
        -not [bool]$Entry.owned -or
        $null -eq $Entry.pid -or
        [string]::IsNullOrWhiteSpace([string]$Entry.processStartTimeUtc)
    ) {
        return $false
    }

    return Test-DevProcessAlive `
        -ProcessId ([int]$Entry.pid) `
        -ExpectedStartTimeUtc ([string]$Entry.processStartTimeUtc)
}

function New-DevProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process
    )

    return [pscustomobject]@{
        pid = $Process.Id
        owned = $true
        processStartTimeUtc = $Process.StartTime.ToUniversalTime().ToString('o')
    }
}

function Test-DevDockerServiceRunning {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ComposeFile,

        [Parameter(Mandatory = $true)]
        [string]$ServiceName
    )

    if (-not (Test-DevDockerReachable)) {
        return $false
    }

    $services = @(& docker compose -f $ComposeFile ps --status running --services $ServiceName 2>$null)
    return $LASTEXITCODE -eq 0 -and $services -contains $ServiceName
}

function Stop-DevProcessTree {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    & taskkill.exe /PID $ProcessId /T /F | Out-Null
    return $LASTEXITCODE -eq 0
}

function Format-DevServiceCommand {
    param(
        [Parameter(Mandatory = $true)]
        $Definition
    )

    return "$($Definition.Command) $($Definition.Arguments -join ' ')"
}
