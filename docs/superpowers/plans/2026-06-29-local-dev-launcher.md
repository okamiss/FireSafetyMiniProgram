# Local Development Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reliable one-command Windows development startup, status, and shutdown for Docker infrastructure, Spring Boot, Vue admin, and the uni-app compiler.

**Architecture:** Root entry scripts source a focused PowerShell library under `scripts/`. Application processes run in visible PowerShell windows through a service runner, while launcher-owned PIDs are persisted under `.dev-runtime` so shutdown never kills pre-existing processes.

**Tech Stack:** Windows PowerShell 5.1+, Docker Compose v2+, Maven 3.9.x, Java 21, Node.js/npm, Spring Boot 3.5.3, Vite 7, uni-app CLI.

---

### Task 1: Test Harness And Shared Launcher Library

**Files:**
- Create: `tests/dev-launcher.Tests.ps1`
- Create: `scripts/dev-common.ps1`
- Modify: `.gitignore`

- [ ] **Step 1: Write failing shared-library tests**

Create a dependency-free PowerShell test harness that dot-sources `scripts/dev-common.ps1` and asserts:

```powershell
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $repoRoot 'scripts\dev-common.ps1')

function Assert-Equal($Expected, $Actual, [string]$Message) {
    if ($Expected -ne $Actual) { throw "$Message Expected=$Expected Actual=$Actual" }
}

$definitions = Get-DevServiceDefinitions -ProjectRoot $repoRoot
Assert-Equal 3 $definitions.Count 'Three application services must be defined.'
Assert-Equal 8080 ($definitions | Where-Object Name -eq 'backend').Port 'Backend port mismatch.'
Assert-Equal 5173 ($definitions | Where-Object Name -eq 'admin').Port 'Admin port mismatch.'
Assert-Equal 'http://127.0.0.1:8080/api' ($definitions | Where-Object Name -eq 'miniapp').Environment.VITE_API_BASE_URL 'Miniapp API URL mismatch.'

$runtimePath = Get-DevRuntimePath -ProjectRoot $repoRoot
Assert-Equal (Join-Path $repoRoot '.dev-runtime\processes.json') $runtimePath 'Runtime path mismatch.'
Write-Output 'PASS dev launcher shared library'
```

- [ ] **Step 2: Run the test and verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: FAIL because `scripts/dev-common.ps1` or `Get-DevServiceDefinitions` does not exist.

- [ ] **Step 3: Implement the shared library**

Implement these functions in `scripts/dev-common.ps1`:

```powershell
Set-StrictMode -Version Latest

function Get-DevRuntimePath([string]$ProjectRoot) {
    Join-Path $ProjectRoot '.dev-runtime\processes.json'
}

function Get-DevServiceDefinitions([string]$ProjectRoot) {
    @(
        [pscustomobject]@{ Name='backend'; Title='Fire Safety Backend'; WorkingDirectory=(Join-Path $ProjectRoot 'server'); Executable='mvn'; Arguments=@('spring-boot:run'); Port=8080; Environment=@{} },
        [pscustomobject]@{ Name='admin'; Title='Fire Safety Admin'; WorkingDirectory=(Join-Path $ProjectRoot 'admin-web'); Executable='npm.cmd'; Arguments=@('run','dev'); Port=5173; Environment=@{} },
        [pscustomobject]@{ Name='miniapp'; Title='Fire Safety Miniapp'; WorkingDirectory=(Join-Path $ProjectRoot 'miniapp'); Executable='npm.cmd'; Arguments=@('run','dev:mp-weixin'); Port=$null; Environment=@{ VITE_API_BASE_URL='http://127.0.0.1:8080/api' } }
    )
}
```

Also add command validation, Docker availability, bounded TCP checks, URL health checks, runtime JSON read/write, and process-alive helpers. Add `.dev-runtime/` to `.gitignore`.

- [ ] **Step 4: Run the test and verify GREEN**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: `PASS dev launcher shared library`.

- [ ] **Step 5: Commit**

```powershell
git add .gitignore scripts/dev-common.ps1 tests/dev-launcher.Tests.ps1
git commit -m "add dev launcher shared library"
```

### Task 2: Service Window Runner And Start Command

**Files:**
- Create: `scripts/run-dev-service.ps1`
- Create: `start-dev.ps1`
- Modify: `tests/dev-launcher.Tests.ps1`

- [ ] **Step 1: Add failing dry-run tests**

Extend `tests/dev-launcher.Tests.ps1` to invoke:

```powershell
$output = & (Join-Path $repoRoot 'start-dev.ps1') -DryRun 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) { throw "Dry run failed: $output" }
foreach ($expected in @('docker compose', 'mvn spring-boot:run', 'npm.cmd run dev', 'npm.cmd run dev:mp-weixin')) {
    if ($output -notmatch [regex]::Escape($expected)) { throw "Dry run missing: $expected" }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: FAIL because `start-dev.ps1` does not exist.

- [ ] **Step 3: Implement the runner and startup orchestration**

`scripts/run-dev-service.ps1` accepts a validated service name and project root, applies the service environment variables, sets the window title, changes directory, and executes the configured command:

```powershell
param(
    [Parameter(Mandatory)][ValidateSet('backend','admin','miniapp')][string]$Service,
    [Parameter(Mandatory)][string]$ProjectRoot
)
. (Join-Path $ProjectRoot 'scripts\dev-common.ps1')
$definition = Get-DevServiceDefinitions -ProjectRoot $ProjectRoot | Where-Object Name -eq $Service
$definition.Environment.GetEnumerator() | ForEach-Object { Set-Item "Env:$($_.Key)" $_.Value }
$Host.UI.RawUI.WindowTitle = $definition.Title
Set-Location -LiteralPath $definition.WorkingDirectory
& $definition.Executable @($definition.Arguments)
if ($LASTEXITCODE -ne 0) { throw "$Service exited with code $LASTEXITCODE" }
```

`start-dev.ps1` supports `-DryRun`, validates tools, runs `docker compose -f deploy/docker-compose.yml up -d mysql redis`, skips services whose fixed ports are open, opens visible PowerShell windows for missing services, writes launcher-owned PIDs, waits up to 60 seconds for backend health, and prints URLs/import path. Dry-run prints all selected commands without opening windows or changing Docker.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: both shared-library and startup dry-run checks pass.

- [ ] **Step 5: Commit**

```powershell
git add scripts/run-dev-service.ps1 start-dev.ps1 tests/dev-launcher.Tests.ps1
git commit -m "add one-command development startup"
```

### Task 3: Status And Safe Shutdown Commands

**Files:**
- Create: `status-dev.ps1`
- Create: `stop-dev.ps1`
- Modify: `tests/dev-launcher.Tests.ps1`

- [ ] **Step 1: Add failing status and shutdown dry-run tests**

Add assertions that `status-dev.ps1 -AsJson` returns objects named `mysql`, `redis`, `backend`, `admin`, and `miniapp`, and that `stop-dev.ps1 -DryRun` prints only recorded launcher PIDs plus `docker compose ... stop mysql redis`.

- [ ] **Step 2: Run the test and verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: FAIL because the status and stop scripts do not exist.

- [ ] **Step 3: Implement status and safe shutdown**

`status-dev.ps1` reports container state from Docker Compose, fixed-port availability, launcher PID state, backend health, and miniapp output presence. `-AsJson` emits machine-readable JSON for tests.

`stop-dev.ps1` reads `.dev-runtime/processes.json`, verifies each recorded PID still exists, stops only those process trees, clears the runtime file, and runs:

```powershell
docker compose -f deploy/docker-compose.yml stop mysql redis
```

`-DryRun` prints actions without stopping processes or containers.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: all launcher tests pass.

- [ ] **Step 5: Commit**

```powershell
git add status-dev.ps1 stop-dev.ps1 tests/dev-launcher.Tests.ps1
git commit -m "add dev environment status and shutdown"
```

### Task 4: Miniapp Environment Configuration And Documentation

**Files:**
- Modify: `miniapp/src/api/http.ts`
- Modify: `miniapp/src/vite-env.d.ts`
- Modify: `README.md`
- Modify: `tests/dev-launcher.Tests.ps1`

- [ ] **Step 1: Add failing source assertions**

Assert `miniapp/src/api/http.ts` references `import.meta.env.VITE_API_BASE_URL` and the README documents `start-dev.ps1`, `status-dev.ps1`, `stop-dev.ps1`, `http://localhost:5173`, and `miniapp/dist/dev/mp-weixin`.

- [ ] **Step 2: Run the test and verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1`

Expected: FAIL because the API URL is still hard-coded and README lacks unified commands.

- [ ] **Step 3: Implement environment-aware API configuration and docs**

Use:

```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://example.com/api'
```

Declare `VITE_API_BASE_URL` in `miniapp/src/vite-env.d.ts`. Replace the local-development README section with one-command start/status/stop instructions, service URLs, WeChat import directory, simulator domain-check note, and real-device LAN/HTTPS requirement.

- [ ] **Step 4: Run focused and project checks**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\dev-launcher.Tests.ps1
cd miniapp
npm run build:mp-weixin
cd ..\admin-web
npm run build
cd ..
mvn -f server/pom.xml test
docker compose -f deploy/docker-compose.yml config --quiet
```

Expected: all commands exit `0`.

- [ ] **Step 5: Commit**

```powershell
git add miniapp/src/api/http.ts miniapp/src/vite-env.d.ts README.md tests/dev-launcher.Tests.ps1
git commit -m "document unified local development workflow"
```

### Task 5: Real End-To-End Startup And Shutdown Verification

**Files:**
- Modify only if verification reveals a tested defect in launcher-owned files.

- [ ] **Step 1: Record pre-existing process ownership**

Run `status-dev.ps1 -AsJson` and record any services already running. Do not stop processes that were not started by the launcher.

- [ ] **Step 2: Start missing services**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\start-dev.ps1`

Expected: MySQL and Redis are running; visible windows open only for missing backend/admin/miniapp processes.

- [ ] **Step 3: Verify application endpoints and output**

Run:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
(Invoke-WebRequest http://localhost:5173 -UseBasicParsing).StatusCode
Test-Path .\miniapp\dist\dev\mp-weixin\app.json
```

Expected: backend reports `status=ok`, admin returns `200`, miniapp `app.json` exists.

- [ ] **Step 4: Verify safe shutdown**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\stop-dev.ps1`

Expected: launcher-owned windows and MySQL/Redis stop; pre-existing backend/admin processes remain alive.

- [ ] **Step 5: Final verification and commit any tested correction**

Run `git diff --check` and the complete command set from Task 4. If a launcher defect required a fix, add a failing regression assertion first, then commit only launcher-owned files with `git commit -m "fix local dev launcher verification"`.
