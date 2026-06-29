param(
    [switch]$AsJson
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
. (Join-Path $projectRoot 'scripts\dev-common.ps1')

$composeFile = Join-Path $projectRoot 'deploy\docker-compose.yml'
$runtimePath = Get-DevRuntimePath -ProjectRoot $projectRoot
$runtimeState = Read-DevRuntime -Path $runtimePath
$miniappOutputPath = Join-Path $projectRoot 'miniapp\dist\dev\mp-weixin'

function Get-ProcessStatus {
    param(
        [string]$Name,
        [Nullable[int]]$Port
    )

    $entry = Get-DevRuntimeEntry -State $runtimeState -Name $Name
    $ownedAlive = Test-DevRuntimeEntryAlive -Entry $entry
    $running = if ($null -ne $Port) {
        Test-DevTcpPort -Port $Port.Value -TimeoutMilliseconds 1000
    }
    else {
        $ownedAlive
    }

    return [ordered]@{
        name = $Name
        running = $running
        launcherOwned = $ownedAlive
        pid = if ($ownedAlive) { [int]$entry.pid } else { $null }
    }
}

$status = [ordered]@{
    mysql = [ordered]@{
        name = 'mysql'
        running = Test-DevDockerServiceRunning -ComposeFile $composeFile -ServiceName 'mysql'
    }
    redis = [ordered]@{
        name = 'redis'
        running = Test-DevDockerServiceRunning -ComposeFile $composeFile -ServiceName 'redis'
    }
    backend = Get-ProcessStatus -Name 'backend' -Port 8080
    admin = Get-ProcessStatus -Name 'admin' -Port 5173
    miniapp = Get-ProcessStatus -Name 'miniapp' -Port $null
    backendHealth = [ordered]@{
        name = 'backendHealth'
        healthy = Test-DevHttp -Uri 'http://localhost:8080/api/health' -TimeoutSeconds 2
        url = 'http://localhost:8080/api/health'
    }
    miniappOutput = [ordered]@{
        name = 'miniappOutput'
        exists = Test-Path -LiteralPath $miniappOutputPath
        path = $miniappOutputPath
    }
}

if ($AsJson) {
    $status | ConvertTo-Json -Depth 5
    exit 0
}

Write-Host ('{0,-16} {1}' -f 'mysql', $(if ($status.mysql.running) { 'running' } else { 'stopped' }))
Write-Host ('{0,-16} {1}' -f 'redis', $(if ($status.redis.running) { 'running' } else { 'stopped' }))
foreach ($name in @('backend', 'admin', 'miniapp')) {
    $service = $status[$name]
    $state = if ($service.running) { 'running' } else { 'stopped' }
    $ownership = if ($service.launcherOwned) { "launcher-owned PID $($service.pid)" } else { 'not launcher-owned' }
    Write-Host ('{0,-16} {1} ({2})' -f $name, $state, $ownership)
}
Write-Host ('{0,-16} {1} ({2})' -f 'backend health', $(if ($status.backendHealth.healthy) { 'healthy' } else { 'unavailable' }), $status.backendHealth.url)
Write-Host ('{0,-16} {1} ({2})' -f 'miniapp output', $(if ($status.miniappOutput.exists) { 'present' } else { 'missing' }), $status.miniappOutput.path)
