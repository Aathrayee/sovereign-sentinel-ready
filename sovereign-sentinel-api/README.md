# Sovereign Telemetry & Anti-Bot Sentinel API

**A deliberately small Spring Boot portfolio demo** of REST endpoints, data persistence, scheduled workers, bearer-style header checks, and synthetic bot decoys.

## What is included

- Spring Boot 3.5.7, Java 21, Maven, Spring Web, Spring Data JPA, Validation, H2, JUnit
- Private `POST /api/vault/ping` resets the 24-hour clock and sets `ACTIVE`
- Private `GET /api/vault/status` gives status, last ping, deadline, seconds remaining
- `@Scheduled` checks every 60 seconds and moves expired record to `LOCKDOWN`
- `GET /api/public/stealth` returns synthetic random bytes for bot-like User-Agents
- `GET /api/public/health` public uptime probe
- Built-in dashboard at `/` — no npm, no separate frontend hosting, no CORS setup necessary
- 4 automated integration tests, including a time-travel expiry simulation

**LIMITATIONS:** This demo does not stop the server in LOCKDOWN, or reliably prevent scraping. User-Agent headers can be spoofed. H2 is local/ephemeral on cloud hosts, so this is not a real 24/7 dead-man's switch. Never use it to protect valuable data or enforce actual safety policies.

## Quickstart (Windows + VS Code)

1. Install **JDK 21** if missing. Check `java -version` and `javac -version` from a NEW VS Code terminal (both must be version 21). One possible installer: `winget install -e --id EclipseAdoptium.Temurin.21.JDK`.
2. Install VS Code **Extension Pack for Java** and **Spring Boot Extension Pack**. Open the extracted `sovereign-sentinel-api` folder with VS Code.
3. Double-click `run-local.cmd` (or run `./mvnw.cmd spring-boot:run` in VS Code PowerShell).
4. On first launch, the script downloads Maven if Maven isn't installed, and Maven resolves Spring libraries. An internet connection is needed.
5. Wait until logs say `Started SentinelApplication` and open **http://localhost:8080**.
6. Local demo key is **`local-demo-key`**; it is filled in automatically for localhost. Click **Check status**, **Send heartbeat**, and **Normal request**.
7. To trigger the bot decoy: in PowerShell run `curl.exe -H "User-Agent: GPTBot/1.0" http://localhost:8080/api/public/stealth`. Browsers cannot override `User-Agent`, so the dashboard demo button uses the explicit `X-Sentinel-Demo-Bot: true` simulation header; the terminal command tests real User-Agent detection.
8. Automated tests: `./mvnw.cmd test`. A test modifies the last ping to 25 hours ago and verifies `LOCKDOWN` without any waiting. Tests use separate in-memory H2.
9. Stop the server: `Ctrl+C` in the running terminal. Port default is 8080.

If PowerShell prohibits local scripts, use the included `.cmd` wrapper, which invokes PowerShell with a process-only execution bypass.

## Endpoints

| Verb | URL | Security |
|---|---|---|
| POST | `/api/vault/ping` | `X-Sentinel-Key` required |
| GET | `/api/vault/status` | `X-Sentinel-Key` required |
| GET | `/api/public/stealth` | Public; heuristic by request's User-Agent |
| GET | `/api/public/health` | Public |
| GET | `/` | Public dashboard |

**PowerShell:**

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/vault/status' -Headers @{'X-Sentinel-Key'='local-demo-key'}
Invoke-RestMethod -Method POST -Uri 'http://localhost:8080/api/vault/ping' -Headers @{'X-Sentinel-Key'='local-demo-key'}
curl.exe -H "User-Agent: GPTBot/1.0" http://localhost:8080/api/public/stealth
```

**H2 browser console (local only):** Stop the server, run `$env:H2_CONSOLE_ENABLED='true'; ./mvnw.cmd spring-boot:run`, open `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:file:./data/sentinel`, username `sa`, blank password. Stop the server before another tool attempts to open this H2 file. Never enable H2 console on a public host.

## Deploy without local Docker — Railway

Railway documents native GitHub deployment for Java/Spring Boot: https://docs.railway.com/guides/spring-boot

1. In VS Code, click **Source Control** (branch icon) → **Initialize Repository** → `+` (Stage All Changes) → enter `Initial Sentinel API` → **Commit** → **Publish Branch** (sign into GitHub, choose public).
   - Requires Git installed on the computer and a GitHub account. If **Publish Branch** is not present, install Git from https://git-scm.com/ and restart VS Code, or use GitHub Desktop.
2. Go to https://railway.com/new, sign in → **Deploy from GitHub repo** → choose your new repository → **Deploy**.
3. Railway automatically identifies Java. The included `railway.json` specifies the start command and healthcheck endpoint.
4. In the service panel, open **Variables** → **New Variable**: `SENTINEL_API_KEY` with a secret string you invent (e.g., a random password of 32+ characters). Do **not** use `local-demo-key` on the public deployment. Redeploy if Railway doesn't do so automatically after setting variables.
5. Open **Settings → Networking → Generate Domain**. Then visit the generated `https://...up.railway.app/` URL. Enter the same secret in the dashboard.
6. Test `/api/public/health` and private dashboard operations. API status is initialized on first start.

### Railway pricing and durability

Railway may offer a time-limited trial and a low recurring monthly credit, but **continuous service is not guaranteed free**. Check current pricing before adding payment details. H2 database files on Railway's ephemeral filesystem are not durable through deployments/restarts. For real monitoring use PostgreSQL, a persistent volume where supported, and an always-running service. The scheduler only runs while the instance is live.

### Why not Render without Docker?

Render does not officially provide a native JVM runtime. For Spring Boot there you need an image/Docker-based deployment. Railway avoids asking you to install or write Docker locally.

### Optional H2 durability on Railway

When you want to keep the heartbeat across redeployments, attach a persistent volume with **mount path `/app/data`**, matching the project's relative `./data` folder (see https://docs.railway.com/volumes). Availability and charges vary by Railway plan. This still does **not** guarantee always-on monitoring if the service is stopped or runs out of credits.

## Security notes and real-world improvements

- User-Agent is spoofable; the public route creates fake *synthetic* bytes on an opt-in demo path. Do not interpret it as a universal crawler or AI-scraper defense.
- The vault header key is intended as a simple portfolio demonstration. Real systems should use identity, OAuth/JWT/API gateway, secrets rotation, audit logs, quotas, and TLS.
- Do not commit secrets. The local key exists for local testing only; Railway variable must override it.
- CORS is limited to local developer origins for separate websites. The bundled dashboard calls the same origin, which needs no CORS.
- Optimistic locking field and JPA write lock are included to serialize updates to the singleton record inside database transactions.

## CV-ready summary (only after you build it)

"Built and deployed a Java 21/Spring Boot monitoring API with transactional JPA state management, authenticated heartbeat endpoints, a 60-second scheduled expiry worker, heuristic bot-decoy responses, and integration tests."
