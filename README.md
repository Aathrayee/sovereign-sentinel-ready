# Watchdog

**Service health and API traffic monitoring with Java 21 and Spring Boot.**

Watchdog is a small backend application that tracks a service heartbeat, changes its state when a heartbeat expires, and enforces a request limit on a public API endpoint. It includes a browser-based dashboard for sending requests and inspecting the results.

This repository is a portfolio project focused on Spring Boot, scheduled jobs, transactional persistence, concurrency, and HTTP API behavior.

## Features

### Heartbeat monitoring
- Stores the latest heartbeat in an H2 database using Spring Data JPA.
- A protected `POST /api/vault/ping` endpoint refreshes the heartbeat and sets the state to `ACTIVE`.
- A scheduled worker checks for expiry every 60 seconds and changes the stored state to `LOCKDOWN` after 24 hours without a new heartbeat.
- A protected status endpoint returns the current state, last heartbeat, deadline, and remaining time.

### Request limiting
- Uses a per-client sliding window of **30 requests per 60 seconds** for `/api/public/stealth`.
- Tracks recent request timestamps using `ConcurrentHashMap` and per-key atomic updates.
- Rejects requests over the limit with **HTTP 429** and a `Retry-After` header.
- Exposes counters for total requests, blocked requests, and tracked clients through a protected endpoint.
- Periodically removes expired client entries.

### Request inspection
- Returns a normal JSON response for ordinary requests to `/api/public/stealth`.
- Returns synthetic randomized data for requests with recognized crawler-related User-Agent strings or an explicit demo header.
- This is a demonstration of response handling, **not reliable bot detection**: User-Agent headers can be spoofed.

### Dashboard
A lightweight HTML, CSS, and JavaScript page is served directly by Spring Boot at `/`. It shows heartbeat status, time remaining, request counters, API responses, and controls for testing request limits. No separate frontend build is needed.

## Technology

- Java 21 and Spring Boot 3.5.7
- Spring Web, Spring Data JPA, Hibernate
- H2 database
- Maven Wrapper
- JUnit / Spring Boot Test
- HTML, CSS, JavaScript

## How it works

```text
Browser dashboard / HTTP client
                |
                v
       Spring Boot REST API
          |           |
          v           v
   HeartbeatService   TrafficRateLimiter
          |           |
          v           v
     Spring Data JPA  In-memory request windows
          |           |  -> HTTP 429 when exceeded
          v
      H2 database
          ^
          |
   Scheduled expiry check (every 60 seconds)
```

## API endpoints

| Method | Endpoint | Purpose | Access |
| --- | --- | --- | --- |
| `POST` | `/api/vault/ping` | Refresh heartbeat | `X-Sentinel-Key` required |
| `GET` | `/api/vault/status` | Read heartbeat state and deadline | `X-Sentinel-Key` required |
| `GET` | `/api/vault/traffic` | Read traffic counters | `X-Sentinel-Key` required |
| `GET` | `/api/public/stealth` | Test normal, decoy, and rate-limited responses | Public |
| `GET` | `/api/public/health` | Basic health endpoint | Public |
| `GET` | `/` | Monitoring dashboard | Public page; private actions require key |

## Run locally

**Requirements:** JDK 21 and an internet connection for the first Maven dependency download. VS Code with the Extension Pack for Java is recommended.

From the repository root, open the application folder:

```powershell
cd sovereign-sentinel-api
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

If your terminal injects a problematic `JAVA_TOOL_OPTIONS` value, the following worked in the development environment used for this project:

```cmd
cmd /c "set JAVA_TOOL_OPTIONS=&& mvnw.cmd spring-boot:run"
```

Open **http://localhost:8080**. For local testing, the default API key is `local-demo-key`. Do not use this key on a public deployment.

### Try the API

```powershell
# Check heartbeat status
curl.exe -H "X-Sentinel-Key: local-demo-key" http://localhost:8080/api/vault/status

# Refresh heartbeat
curl.exe -X POST -H "X-Sentinel-Key: local-demo-key" http://localhost:8080/api/vault/ping

# Read traffic counters
curl.exe -H "X-Sentinel-Key: local-demo-key" http://localhost:8080/api/vault/traffic

# Test a bot-labelled request
curl.exe -H "User-Agent: GPTBot/1.0" http://localhost:8080/api/public/stealth
```

Use **Test 35 requests** in the dashboard to exercise the rate limiter. For a fresh client window, 30 requests are allowed and five receive HTTP 429. Prior requests within the same 60 seconds reduce the number allowed in the burst.

### Build and test

```powershell
.\mvnw.cmd clean package
```

This compiles the project, runs the Maven test suite, and packages the application as `target/sentinel-api.jar`.

## Configuration

Settings are in `sovereign-sentinel-api/src/main/resources/application.properties`.

| Setting | Default | Notes |
| --- | --- | --- |
| `PORT` | `8080` | HTTP server port |
| `SENTINEL_API_KEY` | `local-demo-key` | **Set a strong secret in cloud environments** |
| `sentinel.heartbeat.hours` | `24` | Heartbeat expiry window |
| `sentinel.scheduler.interval-ms` | `60000` | Expiry check interval |
| `sentinel.traffic.max-requests` | `30` | Allowed requests in window |
| `sentinel.traffic.window-seconds` | `60` | Request window duration |

## Deploying to Railway

The application is configured for a Maven/Java deployment without a local Docker installation.

1. Create a Railway project and connect this GitHub repository.
2. In the service's **Settings**, set **Root Directory** to `/sovereign-sentinel-api`. The Java project is nested one folder below the repository root.
3. Configure the start command as `java -jar target/sentinel-api.jar` if Railway does not pick it up from the project configuration.
4. Set the service environment variable `SENTINEL_API_KEY` to a unique, strong secret.
5. Build and deploy, inspect the service logs, and generate a public domain under service networking settings.
6. Confirm that `/api/public/health` works, then use the dashboard with your configured API key.

**Deployment status:** The code is published on GitHub; a public deployment URL will be added after hosting is verified.

## Design limitations

- `LOCKDOWN` is a **stored application state**. It does not shut down the HTTP server or enforce infrastructure-level access controls.
- The scheduled expiry check runs only while the application process is running. It cannot provide an always-on guarantee when the host is stopped or sleeping.
- H2 uses a local file; data may be lost on cloud restarts or redeployments without persistent storage. A managed database is preferable for a durable service.
- The rate limiter is **in-memory and single-instance**. Its counters reset on restart, and separate instances do not share limits.
- Client identification currently uses the direct connecting IP address. Reverse proxies and NAT can cause multiple users to share an apparent address.
- User-Agent matching and synthetic decoy responses do not reliably identify or prevent scraping.

For production use, this design would need persistent/shared storage, proxy-aware client identification, proper authentication and secret management, centralized request limiting, and monitoring outside the application process.

## Project structure

```text
sovereign-sentinel-ready/
├── README.md                     # Repository overview
└── sovereign-sentinel-api/
    ├── pom.xml                   # Maven project
    ├── mvnw.cmd                  # Maven wrapper (Windows)
    ├── src/main/java/            # Controllers, services, JPA, scheduler, limiter
    ├── src/main/resources/
    │   ├── application.properties
    │   └── static/index.html     # Dashboard
    └── src/test/                 # Automated tests
```
