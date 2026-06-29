$ErrorActionPreference = 'Stop'

$script:Passed = 0
$script:Failed = 0
$script:Failures = @()
$projectRoot = Split-Path -Parent $PSScriptRoot

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message (expected: '$Expected', actual: '$Actual')"
    }
}

function Invoke-Test {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Body
    )

    try {
        & $Body
        $script:Passed++
        Write-Host "PASS $Name"
    }
    catch {
        $script:Failed++
        $script:Failures += "${Name}: $($_.Exception.Message)"
        Write-Host "FAIL $Name"
    }
}

function Invoke-LauncherScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName,

        [string[]]$Arguments = @()
    )

    $powershell = (Get-Process -Id $PID).Path
    $scriptPath = Join-Path $projectRoot $ScriptName
    $output = & $powershell -NoProfile -ExecutionPolicy Bypass -File $scriptPath @Arguments 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "$ScriptName exited with code $LASTEXITCODE`n$output"
    }

    return $output
}

$commonPath = Join-Path $projectRoot 'scripts\dev-common.ps1'
$commonLoaded = $false
if (Test-Path -LiteralPath $commonPath) {
    . $commonPath
    $commonLoaded = $true
}

Invoke-Test 'shared service definitions describe backend, admin, and miniapp' {
    Assert-True $commonLoaded 'scripts/dev-common.ps1 must exist'
    $definitions = @(Get-DevServiceDefinitions -ProjectRoot $projectRoot)
    Assert-Equal $definitions.Count 3 'three service definitions are required'

    $backend = $definitions | Where-Object Name -eq 'backend'
    $admin = $definitions | Where-Object Name -eq 'admin'
    $miniapp = $definitions | Where-Object Name -eq 'miniapp'

    Assert-Equal $backend.Command 'mvn' 'backend command'
    Assert-Equal ($backend.Arguments -join ' ') 'spring-boot:run' 'backend arguments'
    Assert-Equal $backend.Port 8080 'backend port'
    Assert-Equal $admin.Command 'npm.cmd' 'admin command'
    Assert-Equal ($admin.Arguments -join ' ') 'run dev' 'admin arguments'
    Assert-Equal $admin.Port 5173 'admin port'
    Assert-Equal $miniapp.Command 'npm.cmd' 'miniapp command'
    Assert-Equal ($miniapp.Arguments -join ' ') 'run dev:mp-weixin' 'miniapp arguments'
    Assert-Equal $miniapp.Port $null 'miniapp has no port'
    Assert-Equal $miniapp.Environment.VITE_API_BASE_URL 'http://127.0.0.1:8080/api' 'miniapp API environment'
}

Invoke-Test 'runtime state path is repository local' {
    Assert-True $commonLoaded 'scripts/dev-common.ps1 must exist'
    $expected = Join-Path $projectRoot '.dev-runtime\processes.json'
    Assert-Equal (Get-DevRuntimePath -ProjectRoot $projectRoot) $expected 'runtime path'
}

Invoke-Test 'runtime JSON helpers round-trip launcher-owned processes' {
    Assert-True $commonLoaded 'scripts/dev-common.ps1 must exist'
    $temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("fire-safety-launcher-test-" + [guid]::NewGuid())
    $runtimePath = Join-Path $temporaryDirectory 'processes.json'
    try {
        $state = [pscustomobject]@{
            backend = [pscustomobject]@{ pid = $PID; owned = $true }
            admin = $null
            miniapp = $null
        }
        Write-DevRuntime -Path $runtimePath -State $state
        $loaded = Read-DevRuntime -Path $runtimePath
        Assert-Equal $loaded.backend.pid $PID 'runtime PID'
        Assert-True ([bool]$loaded.backend.owned) 'runtime ownership flag'
    }
    finally {
        if (Test-Path -LiteralPath $temporaryDirectory) {
            Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
        }
    }
}

Invoke-Test 'shared command and process helpers report known live resources' {
    Assert-True $commonLoaded 'scripts/dev-common.ps1 must exist'
    Assert-True (Test-DevCommand -Name 'powershell') 'powershell command should be available'
    Assert-True (Test-DevProcessAlive -ProcessId $PID) 'current process should be alive'
}

Invoke-Test 'launcher ownership requires a matching process start time' {
    Assert-True $commonLoaded 'scripts/dev-common.ps1 must exist'
    $incompleteEntry = [pscustomobject]@{ pid = $PID; owned = $true }
    Assert-True (-not (Test-DevRuntimeEntryAlive -Entry $incompleteEntry)) 'an incomplete runtime record must not own a live PID'
}

Invoke-Test 'start dry-run prints all commands without changing runtime state' {
    $runtimePath = Join-Path $projectRoot '.dev-runtime\processes.json'
    $before = if (Test-Path -LiteralPath $runtimePath) { Get-Content -Raw -LiteralPath $runtimePath } else { $null }
    $output = Invoke-LauncherScript -ScriptName 'start-dev.ps1' -Arguments @('-DryRun')
    $after = if (Test-Path -LiteralPath $runtimePath) { Get-Content -Raw -LiteralPath $runtimePath } else { $null }

    Assert-True ($output -match 'docker compose -f .*deploy[\\/]docker-compose.yml"? up -d mysql redis') 'dry-run must print Docker startup'
    Assert-True ($output -match 'mvn spring-boot:run') 'dry-run must print backend launch'
    Assert-True ($output -match 'npm\.cmd run dev') 'dry-run must print admin launch'
    Assert-True ($output -match 'npm\.cmd run dev:mp-weixin') 'dry-run must print miniapp launch'
    Assert-Equal $after $before 'dry-run must not change runtime state'
}

Invoke-Test 'status JSON exposes stable service names' {
    $output = Invoke-LauncherScript -ScriptName 'status-dev.ps1' -Arguments @('-AsJson')
    $status = $output | ConvertFrom-Json
    $names = @($status.PSObject.Properties.Name)
    foreach ($name in @('mysql', 'redis', 'backend', 'admin', 'miniapp', 'backendHealth', 'miniappOutput')) {
        Assert-True ($names -contains $name) "status JSON must contain $name"
    }
}

Invoke-Test 'stop dry-run prints owned process and Docker actions without changing runtime state' {
    $runtimePath = Join-Path $projectRoot '.dev-runtime\processes.json'
    $before = if (Test-Path -LiteralPath $runtimePath) { Get-Content -Raw -LiteralPath $runtimePath } else { $null }
    $output = Invoke-LauncherScript -ScriptName 'stop-dev.ps1' -Arguments @('-DryRun')
    $after = if (Test-Path -LiteralPath $runtimePath) { Get-Content -Raw -LiteralPath $runtimePath } else { $null }

    Assert-True ($output -match 'launcher-owned') 'stop dry-run must describe owned process cleanup'
    Assert-True ($output -match 'docker compose -f .*deploy[\\/]docker-compose.yml"? stop mysql redis') 'stop dry-run must print Docker stop'
    Assert-Equal $after $before 'stop dry-run must not change runtime state'
}

Invoke-Test 'miniapp API base URL comes from Vite environment with fallback' {
    $httpSource = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'miniapp\src\api\http.ts')
    Assert-True ($httpSource -match 'import\.meta\.env\.VITE_API_BASE_URL') 'API source must read VITE_API_BASE_URL'
    Assert-True ($httpSource -match 'https://example\.com/api') 'API source must retain the placeholder fallback'

    $envSource = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'miniapp\src\vite-env.d.ts')
    Assert-True ($envSource -match 'VITE_API_BASE_URL') 'Vite environment type must declare VITE_API_BASE_URL'
}

Invoke-Test 'README documents launcher commands and local miniapp constraints' {
    $readme = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'README.md') -Encoding UTF8
    $domainValidation = ([char[]]@(0x57DF, 0x540D, 0x6821, 0x9A8C)) -join ''
    $localNetwork = ([char[]]@(0x5C40, 0x57DF, 0x7F51)) -join ''
    foreach ($text in @('.\start-dev.ps1', '.\status-dev.ps1', '.\stop-dev.ps1', 'http://localhost:8080/api/health', 'http://localhost:5173', 'miniapp/dist/dev/mp-weixin', $domainValidation, $localNetwork, 'HTTPS')) {
        Assert-True ($readme.Contains($text)) "README must contain $text"
    }
}

Invoke-Test 'admin build is no-emit and generated files are absent' {
    $package = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'admin-web\package.json') | ConvertFrom-Json
    Assert-Equal $package.scripts.build 'vue-tsc --noEmit && vite build' 'admin build command'

    $tsconfig = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'admin-web\tsconfig.json') | ConvertFrom-Json
    Assert-True ([bool]$tsconfig.compilerOptions.noEmit) 'admin tsconfig noEmit must be true'

    $generatedFiles = @(
        Get-ChildItem -Path (Join-Path $projectRoot 'admin-web\src') -Filter '*.js' -Recurse -File
        Get-Item -LiteralPath (Join-Path $projectRoot 'admin-web\vite.config.js') -ErrorAction SilentlyContinue
        Get-Item -LiteralPath (Join-Path $projectRoot 'admin-web\tsconfig.tsbuildinfo') -ErrorAction SilentlyContinue
    )
    Assert-Equal $generatedFiles.Count 0 'tracked/generated JavaScript and build info files must be removed'
}

Write-Host ""
Write-Host "Passed: $script:Passed  Failed: $script:Failed"
if ($script:Failures.Count -gt 0) {
    $script:Failures | ForEach-Object { Write-Host " - $_" }
    exit 1
}

exit 0
