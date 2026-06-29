param(
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$commonScript = Join-Path $projectRoot 'scripts\dev-common.ps1'
$runnerScript = Join-Path $projectRoot 'scripts\run-dev-service.ps1'
$composeFile = Join-Path $projectRoot 'deploy\docker-compose.yml'
$healthUrl = 'http://localhost:8080/api/health'
$adminUrl = 'http://localhost:5173'
$miniappOutput = Join-Path $projectRoot 'miniapp\dist\dev\mp-weixin'

. $commonScript

$definitions = @(Get-DevServiceDefinitions -ProjectRoot $projectRoot)
if ($DryRun) {
    Write-Host "[DryRun] docker compose -f `"$composeFile`" up -d mysql redis"
    foreach ($definition in $definitions) {
        if ($definition.Name -eq 'miniapp') {
            Write-Host "[DryRun] Start miniapp if no launcher-owned live process exists: $(Format-DevServiceCommand -Definition $definition)"
        }
        else {
            Write-Host "[DryRun] Start $($definition.Name) if port $($definition.Port) is closed: $(Format-DevServiceCommand -Definition $definition)"
        }
    }
    Write-Host "[DryRun] Runtime state would be written to $(Get-DevRuntimePath -ProjectRoot $projectRoot)"
    exit 0
}

$missingCommands = @('java', 'mvn', 'node', 'npm', 'docker') |
    Where-Object { -not (Test-DevCommand -Name $_) }
if ($missingCommands.Count -gt 0) {
    throw "Missing required commands: $($missingCommands -join ', ')"
}
if (-not (Test-DevDockerReachable)) {
    throw 'Docker is installed, but the Docker engine is not reachable.'
}

& docker compose -f $composeFile up -d mysql redis
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to start MySQL and Redis with Docker Compose.'
}

$runtimePath = Get-DevRuntimePath -ProjectRoot $projectRoot
$previousState = Read-DevRuntime -Path $runtimePath
$runtimeState = New-DevRuntimeState

foreach ($definition in $definitions) {
    $existingEntry = Get-DevRuntimeEntry -State $previousState -Name $definition.Name
    $hasLiveOwnedProcess = Test-DevRuntimeEntryAlive -Entry $existingEntry
    if ($hasLiveOwnedProcess) {
        $runtimeState.$($definition.Name) = $existingEntry
    }

    $shouldStart = $false
    if ($definition.Name -eq 'miniapp') {
        $shouldStart = -not $hasLiveOwnedProcess
    }
    else {
        $portOpen = Test-DevTcpPort -Port $definition.Port -TimeoutMilliseconds 1000
        $shouldStart = -not $portOpen -and -not $hasLiveOwnedProcess
        if ($portOpen) {
            Write-Host "$($definition.Name) already listens on port $($definition.Port); leaving it untouched."
        }
        elseif ($hasLiveOwnedProcess) {
            Write-Host "$($definition.Name) launcher process is already running; waiting for it to become ready."
        }
    }

    if ($shouldStart) {
        $arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$runnerScript`" -ServiceName $($definition.Name) -ProjectRoot `"$projectRoot`""
        $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -WorkingDirectory $projectRoot -PassThru
        $runtimeState.$($definition.Name) = New-DevProcessRecord -Process $process
        Write-Host "Started $($definition.Name) in PowerShell (PID $($process.Id))."
        Write-DevRuntime -Path $runtimePath -State $runtimeState
    }
}

Write-DevRuntime -Path $runtimePath -State $runtimeState

if (-not (Wait-DevHttp -Uri $healthUrl -TimeoutSeconds 60)) {
    Write-Warning "Backend health did not become ready within 60 seconds: $healthUrl"
}

Write-Host "Backend health: $healthUrl"
Write-Host "Admin web:     $adminUrl"
Write-Host "Miniapp import: $miniappOutput"
