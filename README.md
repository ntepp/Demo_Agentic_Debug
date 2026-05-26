# Financial Demo — Agentic Engineering Demo

> 5-minute conference demo: Claude Code + MCP tools automating a complete production debug workflow.

## The Demo

```
/debug TTP-2847
```

Claude reads a Jira ticket, fetches Confluence architecture docs, searches application logs,
traces a `NullPointerException` in `FinancialAmountCalculator.java:142`, applies a null-safe
fix, generates a regression test, formats a commit message, and updates the Jira ticket status.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 25 + Spring Boot 3.4.1 + Maven |
| Database | PostgreSQL 16 |
| Observability | Loki 3 + Grafana 10 + Promtail |
| Ticketing | Jira (MCP) |
| Documentation | Confluence (MCP) |
| AI | Claude Code + custom MCP |

## Quick Start

**Prerequisites:** JDK 25 at `D:\app\jdk-25.0.3`, Maven, Docker Desktop

```bash
# Full stack (Docker)
./scripts/start-demo.sh

# Local only (H2, no Docker)
cd order-service
JAVA_HOME=D:\app\jdk-25.0.3 mvn spring-boot:run
```

## The Bug

`FinancialAmountCalculator.java:142` throws `NullPointerException` when processing
orders from portfolio `EMERGING-MKT-C`. Its `currency` field is `null` — it was
imported from FinCore v1 without currency enrichment (tracked in TTP-2841).

**Orders that trigger the bug:** `ORD-008`, `ORD-009`, `ORD-010`

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/orders` | All orders |
| `GET` | `/api/orders/failed` | Orders that failed batch |
| `GET` | `/api/orders/unconfigured` | Orders with no portfolio currency |
| `POST` | `/api/orders/batch-report` | Manually trigger batch |
| `GET` | `/actuator/health` | Health check |

## Observability

Grafana: http://localhost:3000  
LogQL: `{job="financial-order-service"} |= "NullPointerException"`

## CI

GitHub Actions workflow at `.github/workflows/ci.yml`:
- Builds with Java 25
- Runs all tests
- **Regression gate job** runs `shouldHandleMissingCurrency()` on every PR — red until TTP-2847 is fixed

## Project Layout

```
financial-demo/
├── order-service/          # Spring Boot app (Java 25)
├── docker/                 # Docker Compose
├── monitoring/             # Loki + Grafana + Promtail config
├── scripts/                # Demo helper scripts
├── .claude/
│   ├── commands/debug.md   # /debug slash command
│   └── skills/             # 6 Claude skills
└── .github/workflows/      # CI
```
