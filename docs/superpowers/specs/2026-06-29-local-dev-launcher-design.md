# Local Development Launcher Design

## Goal

Provide a Windows PowerShell entrypoint that starts the complete local development environment with one command while keeping backend, admin-web, and miniapp logs visible in separate terminal windows.

## Commands

- `start-dev.ps1`: validate prerequisites, start infrastructure, and open development processes.
- `stop-dev.ps1`: stop only processes recorded by the launcher, then stop local infrastructure containers.
- `status-dev.ps1`: report container, process, port, and backend health status.

All commands run from the repository root.

## Startup Flow

1. Resolve the repository root from the script location.
2. Verify `docker`, `mvn`, `java`, `node`, and `npm` are available.
3. Verify the Docker engine is reachable.
4. Start the `mysql` and `redis` services from `deploy/docker-compose.yml`.
5. Check ports before starting application processes:
   - Backend: `8080`
   - Admin web: `5173`
6. Open visible PowerShell windows for missing processes:
   - Backend: `mvn spring-boot:run` in `server`.
   - Admin web: `npm run dev` in `admin-web`.
   - Miniapp compiler: `npm run dev:mp-weixin` in `miniapp`.
7. Wait for `http://localhost:8080/api/health` with a bounded timeout.
8. Print the admin URL, health URL, and WeChat Developer Tools import directory.

An occupied backend or admin port is treated as an already-running service and is not started again. The launcher records only processes it creates.

## Process Tracking And Shutdown

Launcher-owned process IDs are stored in an ignored `.dev-runtime/processes.json` file. Shutdown reads this file and stops those process trees only. Existing processes discovered by port checks are never recorded or terminated.

`stop-dev.ps1` also runs Docker Compose stop for `mysql` and `redis`. Persistent MySQL data remains in the named Docker volume.

## Miniapp API Configuration

The miniapp API base URL is read from `VITE_API_BASE_URL`. The launcher sets it to `http://127.0.0.1:8080/api` in the miniapp compiler window. The existing production placeholder remains the fallback until the real HTTPS domain is supplied.

The development output imported into WeChat Developer Tools is `miniapp/dist/dev/mp-weixin`. Local simulator requests require the developer-tool domain validation option to be disabled. Real-device testing requires a reachable LAN or HTTPS address instead of loopback.

## Error Handling

- Missing commands stop startup before any application windows are opened.
- Docker startup failure stops the workflow with the failing command visible.
- A process that exits early remains visible in its terminal for diagnosis.
- Backend health timeout is reported as a warning and does not close other running services.
- Re-running startup is idempotent for infrastructure and port-detected services.

## Verification

- Static PowerShell parsing for all scripts.
- A dry-run mode verifies command selection, paths, and port decisions without opening windows or changing containers.
- Docker Compose configuration validation.
- Real startup verification for MySQL, Redis, backend health, admin HTTP response, and miniapp development output.
- Real shutdown verification that launcher-owned processes stop while pre-existing processes remain untouched.

## Documentation

The root README will document the one-command start, status, stop, service URLs, miniapp import directory, and the local-versus-real-device API address distinction.
