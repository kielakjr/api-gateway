# API Gateway

A high-performance, non-blocking API gateway built from scratch with **Netty** and **Java 21** — no frameworks, no Spring, no magic. Every component (routing, rate limiting, circuit breaking, retries, auth, CORS, metrics) is hand-implemented to demonstrate production-grade infrastructure engineering.

## Architecture

```
                          ┌─────────────────────────────────────────────┐
                          │            Netty Server (NIO)               │
                          │         Non-blocking Event Loop             │
                          └──────────────────┬──────────────────────────┘
                                             │
                         ┌───────────────────┼───────────────────┐
                         │                   │                   │
                         ▼                   ▼                   ▼
                  ┌─────────────┐   ┌──────────────┐   ┌──────────────┐
                  │ /_gateway/  │   │ /_gateway/   │   │  /_gateway/  │
                  │   health    │   │   metrics    │   │    routes    │
                  └─────────────┘   └──────────────┘   └──────────────┘
                  (Admin Endpoints — bypass filter pipeline)
                                             │
               ┌─────────────────────────────┼─────────────────────────────┐
               │          REQUEST FILTER PIPELINE (sequential)             │
               │                                                           │
               │  ┌──────────┐  ┌────────┐  ┌────────┐  ┌──────────────┐   │
               │  │ Logging  │─▶│  CORS  │─▶│  Auth  │─▶│ Rate Limiter │   │
               │  │          │  │        │  │ (JWT)  │  │(Token Bucket)│   │
               │  └──────────┘  └────────┘  └────────┘  └──────┬───────┘   │
               │                                               │           │
               │                                    ┌──────────▼─────────┐ │
               │                                    │ Request Transform  │ │
               │                                    │ +X-Request-ID      │ │
               │                                    │ +X-Client-IP       │ │
               │                                    │ -Authorization     │ │
               │                                    └───────────┬────────┘ │
               └────────────────────────────────────────────────┼──────────┘
                                                                │
                                                                ▼
                                                     ┌────────────────────┐
                              ┌──── hot reload ──────│      Router        │
                              │   (WatchService)     │  (path-prefix)     │
                              │                      └─────────┬──────────┘
                     ┌────────┴──┐                             │
                     │config.yaml│                             ▼
                     └───────────┘                  ┌────────────────────┐
                                                    │   Load Balancer    │
                                                    │ round-robin /      │
                                                    │ weighted-random    │
                                                    └──────────┬─────────┘
                                                               │
                              ┌────────────────────────────────┼────────────────────┐
                              │         RESILIENCE LAYER       │                    │
                              │                                ▼                    │
                              │              ┌──────────────────────────┐           │
                              │              │     Circuit Breaker      │           │
                              │              │  CLOSED ─▶ OPEN ─▶ HALF │            │
                              │              └────────────┬─────────────┘           │
                              │                           │                         │
                              │              ┌────────────▼─────────────┐           │
                              │              │      Retry Policy        │           │
                              │              │  Exponential backoff     │           │
                              │              │  (idempotent only)       │           │
                              │              └────────────┬─────────────┘           │
                              └───────────────────────────┼─────────────────────────┘
                                                          │
                                         ┌────────────────┼────────────────┐
                                         ▼                                 ▼
                                ┌──────────────────┐              ┌──────────────────┐
                                │   Upstream A     │              │   Upstream B     │
                                │ localhost:9001   │              │ localhost:9002   │
                                └────────┬─────────┘              └────────┬─────────┘
                                         │                                 │
                                         └────────────────┬────────────────┘
                                                          ▼
                                              ┌───────────────────────┐
                                              │  Response Transform   │
                                              │  +X-Frame-Options     │
                                              │  +X-Content-Type-Opts │
                                              │  +Strict-Transport    │
                                              └───────────┬───────────┘
                                                          │
                                                          ▼
                                                       Client
```

## Features

| Feature | Description |
|---------|-------------|
| **Path-based routing** | Matches request paths to upstream targets via prefix rules defined in config |
| **Load balancing** | Round-robin and weighted-random strategies distribute traffic across upstreams |
| **Token bucket rate limiting** | Per-client-IP rate limiter with configurable requests-per-minute and automatic idle cleanup |
| **Three-state circuit breaker** | Per-upstream CLOSED/OPEN/HALF_OPEN state machine that fails fast when backends are down |
| **Retry with exponential backoff** | Automatically retries failed requests to idempotent methods (GET, PUT, DELETE, HEAD, OPTIONS, TRACE) |
| **JWT authentication** | Validates HMAC-SHA signed Bearer tokens on every request using JJWT |
| **CORS handling** | Full preflight (OPTIONS) support with configurable origins, methods, headers, and max-age |
| **Request transforms** | Injects X-Request-ID, X-Client-IP, X-Gateway-Version headers; strips Authorization before forwarding |
| **Response transforms** | Adds X-Content-Type-Options, X-Frame-Options: DENY, and Strict-Transport-Security headers |
| **Hot config reload** | Java WatchService monitors config.yaml and live-reloads routes and load balancer without restart |
| **Metrics collection** | Tracks total requests, 4xx/5xx counts, and latency percentiles (p50, p95, p99) |
| **Admin endpoints** | `/_gateway/health`, `/_gateway/metrics`, and `/_gateway/routes` for observability |
| **Structured logging** | SLF4J + Logback with per-request IDs for traceability |

## Configuration Reference

All configuration lives in `config.yaml` at the project root. A `.env` file holds secrets.

```yaml
# ── Server ────────────────────────────────────────────────────
server:
  port: 8080                        # Port the gateway listens on

# ── Routing ───────────────────────────────────────────────────
routes:                             # List of route definitions
  - path: /api/users                # Path prefix to match
    upstreams:                      # Target backend URLs
      - http://localhost:9001
      - http://localhost:9002

# ── Load Balancing ────────────────────────────────────────────
loadBalancerStrategy: round-robin   # "round-robin" or "weighted-random"

# ── Rate Limiting ─────────────────────────────────────────────
rateLimitPerMinute: 60              # Max requests per client IP per minute

# ── Connection Pool ───────────────────────────────────────────
connectionPool:
  connectTimeoutSeconds: 5          # TCP connect timeout
  requestTimeoutSeconds: 10         # HTTP request timeout

# ── Circuit Breaker ───────────────────────────────────────────
circuitBreaker:
  failureThreshold: 5               # Consecutive failures before opening
  recoveryTimeMs: 30000             # Milliseconds in OPEN before trying HALF_OPEN

# ── Retry Policy ──────────────────────────────────────────────
retryPolicy:
  maxRetries: 3                     # Max retry attempts (idempotent methods only)
  initialDelayMs: 1000              # Delay before first retry
  backoffMultiplier: 2.0            # Multiplier per attempt (1s → 2s → 4s)

# ── Timeouts ──────────────────────────────────────────────────
timeouts:
  readSeconds: 30                   # Netty ReadTimeoutHandler
  writeSeconds: 30                  # Netty WriteTimeoutHandler

# ── CORS ──────────────────────────────────────────────────────
cors:
  allowedOrigins:                   # Origins permitted for cross-origin requests
    - "*"
  allowedMethods:                   # HTTP methods permitted
    - GET
    - POST
    - PUT
    - DELETE
  allowedHeaders:                   # Headers clients may send
    - Content-Type
    - Authorization
  maxAgeSeconds: 3600               # Preflight response cache duration
```

**Environment variables** (`.env` file):

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Base64-encoded HMAC-SHA key for JWT token validation |

## How to Run

### Prerequisites

- Java 21+
- Maven 3.6+

### Local

```bash
# 1. Clone and enter the project
git clone <repo-url> && cd api-gateway

# 2. Create a .env file with your JWT secret
echo "JWT_SECRET=<your-base64-encoded-secret>" > .env

# 3. Configure routes in config.yaml (see reference above)

# 4. Build
mvn clean install

# 5. Run
mvn exec:java -Dexec.mainClass="kielakjr.api_gateway.App"
```

The gateway starts on the port defined in `config.yaml` (default `8080`). Edit `config.yaml` while running — routes and load balancer strategy hot-reload automatically.

### Docker

```bash
# 1. Build the image (multi-stage: builds fat JAR, then copies to slim JRE image)
docker build -t api-gateway .

# 2. Run with config and secrets mounted
docker run --name api-gateway -p 8080:8080 \
  -v $(pwd)/config.yaml:/app/config.yaml \
  --env-file .env \
  api-gateway
```

### Verify it's running

```bash
curl http://localhost:8080/_gateway/health
# → {"status":"UP"}

curl http://localhost:8080/_gateway/metrics
# → {"totalRequests":0,"serverErrors":0,"clientErrors":0,"latency":{...}}
```

## Design Decisions

### Why Netty over a framework like Spring or Vert.x?

An API gateway is essentially a high-throughput byte-shuffler — it reads an inbound request, runs a few fast checks, and proxies it upstream. Spring Boot's dependency injection, annotation scanning, auto-configuration, and servlet abstraction layers add latency and memory overhead that buy nothing here. Netty gives direct control over the NIO event loop, the channel pipeline, and buffer management. Every handler in the pipeline is explicit and visible. The result is a gateway where every microsecond of request processing is accounted for, with no hidden magic between the socket and the business logic.

### Why no framework at all?

The point of this project is to demonstrate understanding of what a gateway actually does at the network level. A framework would hide the interesting parts — connection lifecycle, pipeline composition, backpressure, timeout handling. Building from Netty primitives means every component (filter chain, router, proxy client, circuit breaker) is a visible, testable, replaceable piece of code rather than a configuration flag in someone else's abstraction.

### Why token bucket for rate limiting?

Token bucket naturally handles bursty traffic, which is the real-world pattern for APIs. A client that was idle can spend saved-up tokens in a quick burst, while a client hammering the endpoint continuously hits the limit. Fixed-window counters have the "boundary burst" problem (double the rate at window edges). Sliding-window log is memory-expensive. Token bucket is O(1) per request, requires only two values per client (token count and last refill time), and the refill logic is lazy — computed on each request rather than requiring a background timer thread.

### Why a three-state circuit breaker instead of a simple on/off switch?

A two-state breaker (open/closed) forces a binary choice: either send all traffic or send none. When a backend recovers, you'd have to guess when to flip back. The HALF_OPEN state solves this by letting exactly one probe request through after the recovery timeout. If it succeeds, the circuit closes and normal traffic resumes. If it fails, the circuit re-opens and waits again. This prevents a thundering herd from crashing a recovering backend and gives a controlled, automatic recovery path without manual intervention.

### Why only retry idempotent methods?

Retrying a POST that charges a credit card or creates a record could cause duplicate side effects. GET, PUT, DELETE, HEAD, OPTIONS, and TRACE are safe to retry because repeating them produces the same result. Non-idempotent methods (POST, PATCH) fail immediately on the first error. This is a deliberate safety boundary — the gateway should never cause data corruption by being "helpful" with retries.

### Why per-upstream circuit breakers?

A single global circuit breaker would take down the entire gateway if one backend fails. Instead, each upstream URL gets its own circuit breaker instance (tracked in a ConcurrentHashMap). If `upstream-A` goes down, its circuit opens and traffic shifts to `upstream-B` through the load balancer. The healthy backend keeps serving while the broken one recovers independently.

## Project Structure

```
src/main/java/kielakjr/api_gateway/
├── App.java                        # Entry point
├── config/                         # YAML config loading + hot reload
├── context/                        # Per-request state (ID, IP, timestamps)
├── filter/                         # Request pipeline filters
├── handler/                        # Netty channel handlers
├── loadbalancer/                   # Round-robin + weighted-random strategies
├── metrics/                        # Counters, histograms, percentile tracking
├── proxy/                          # HTTP forwarding to upstreams
├── resilience/                     # Circuit breaker + retry policy
├── router/                         # Path-based route resolution
└── server/                         # Netty server bootstrap
```
