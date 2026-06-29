param(
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
. (Join-Path $projectRoot 'scripts\dev-common.ps1')

$composeFile = Join-Path $projectRoot 'deploy\docker-compose.yml'
$runtimePath = Get-DevRuntimePath -ProjectRoot $projectRoot
$runtimeState = Read-DevRuntime -Path $runtimePath

if ($DryRun) {
    Write-Host '[DryRun] Stop only launcher-owned process trees recorded in .dev-runtime/processes.json.'
    foreach ($name in @('backend', 'admin', 'miniapp')) {
        $entry = Get-DevRuntimeEntry -State $runtimeState -Name $name
        if (Test-DevRuntimeEntryAlive -Entry $entry) {
            Write-Host "[DryRun] taskkill.exe /PID $($entry.pid) /T /F ($name)"
        }
    }
    Write-Host "[DryRun] docker compose -f `"$composeFile`" stop mysql redis"
    exit 0
}

foreach ($name in @('backend', 'admin', 'miniapp')) {
    $entry = Get-DevRuntimeEntry -State $runtimeState -Name $name
    if (-not (Test-DevRuntimeEntryAlive -Entry $entry)) {
        continue
    }

    if (Stop-DevProcessTree -ProcessId ([int]$entry.pid)) {
        Write-Host "Stopped launcher-owned $name process tree (PID $($entry.pid))."
    }
    else {
        Write-Warning "Could not stop launcher-owned $name process tree (PID $($entry.pid))."
    }
}

if (Test-Path -LiteralPath $runtimePath) {
    Remove-Item -LiteralPath $runtimePath -Force
}

& docker compose -f $composeFile stop mysql redis
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to stop MySQL and Redis with Docker Compose.'
}

Write-Host 'Development services stopped. The Docker volume was retained.'
