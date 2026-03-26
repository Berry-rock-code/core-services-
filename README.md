# Integration Hub

A Java 21 + Spring Boot service that synchronizes property addresses across
Salesforce, a Google Sheets Loan Tape, and Buildium so that Berry Rock has
end-to-end searchability across all three platforms.

## The Problem It Solves

Each system holds the same properties but with no shared identifier. A
Salesforce Opportunity, a row in the Loan Tape Google Sheet, and a Buildium
lease all refer to the same physical address but have no way to find each other.
This service normalizes and matches addresses across all three, then writes the
Salesforce Opportunity ID and Buildium IDs back into the sheet so the ops team
has a single lookup point without needing access to Salesforce or Buildium directly.

## How It Works

The pipeline runs in three phases. For a detailed breakdown see [docs/architecture.md](docs/architecture.md).

**Phase 1 — Fetch**

Pulls all Opportunities with a non-null address from Salesforce (via JWT bearer
auth + SOQL), all data rows from the Loan Tape Google Sheet, and all active-lease
addresses from Buildium. Buildium units and leases are fetched separately and
joined in memory by unit ID, keeping total API calls to two regardless of
portfolio size.

**Phase 2 — Normalize and Match**

Addresses are normalized (suffixes abbreviated, punctuation stripped, city aliases
applied for OKC, STL, KC, ZIP+4 truncated) then matched using a three-pass strategy:

- Pass 1 — full composite key (address + city + state + ZIP): result = `SYNCED`
- Pass 2 — address line only, CLEAN SF records: result = `SYNCED_ADDRESS_ONLY`
- Pass 3 — address line only, PARTIAL SF records: result = `SYNCED_PARTIAL`

Salesforce records are pre-classified as `CLEAN`, `PARTIAL`, or `SUSPICIOUS`
before matching. `SUSPICIOUS` records (where the street field contains the city,
state, or ZIP) are excluded entirely.

Buildium matching runs after each SF match using the same two-pass key strategy.
A single Buildium match writes the property and lease IDs. Multiple matches
produce `AMBIGUOUS_BUILDIUM_MATCH`.

**Phase 3 — Write Back**

Matched rows are updated in Google Sheets with the Salesforce Opportunity ID,
Buildium property and lease IDs, the SF-standardized address, quality tier, and
sync status. When `address.pipeline.dry-run=true` no writes are made — the first
10 proposed updates are logged instead.

## Stack

- Java 21
- Spring Boot 3.2.3
- Google Sheets API v4
- Salesforce REST API (JWT bearer token flow)
- Buildium REST API (client ID + secret headers)
- Apache Commons Lang3
- Auth0 Java JWT (for Salesforce auth signing)
- Spring Boot Actuator

## Package Structure

| Package | Role |
|---------|------|
| `config` | `@ConfigurationProperties` bindings and `@Bean` definitions |
| `controller` | HTTP endpoints — routing and request/response only |
| `service` | Core business logic and pipeline orchestration |
| `client` | Thin API wrappers for Buildium, Google Sheets, and Salesforce |
| `model` | Internal domain objects (one per source system plus shared types) |
| `dto` | API request and response envelopes |
| `util` | Stateless address normalization and grouping utilities |
| `audit` | Structured workflow-step and compliance event logging |

## Documentation

| Document | Contents |
|----------|----------|
| [docs/architecture.md](docs/architecture.md) | Full pipeline walkthrough, two-pass matching detail, quality tier table, data flow diagram, planned Buildium-anchored pass |
| [docs/address-normalization.md](docs/address-normalization.md) | Every normalization rule with before/after examples and the reasoning behind each one |
| [docs/configuration-reference.md](docs/configuration-reference.md) | Every `address.pipeline.*` and `integration.vendor.*` property with type, default, required flag, and env var override |
| [docs/runbook.md](docs/runbook.md) | How to run locally, curl examples, reading the summary log output, diagnosing low match rates, health check endpoints |

## Running Locally

### Prerequisites

- JDK 21
- `./mvnw` wrapper (included in the repo)
- A Google service account JSON key with Sheets read/write access
- A Salesforce Connected App configured for JWT bearer flow with a PEM private key
- Buildium API credentials (client ID + secret)

### Credentials

Create `src/main/resources/application-local.yml` (gitignored) with your credentials:

```yaml
integration:
  vendor:
    buildium:
      client-id: your-client-id
      client-secret: your-client-secret
    google-sheets:
      credentials-path: /path/to/service-account.json
    salesforce:
      enabled: true
      client-id: your-sf-connected-app-consumer-key
      username: your-sf-username
      private-key-path: /path/to/private-key.pem

address:
  pipeline:
    dry-run: true
    sheet-id: your-google-spreadsheet-id
    sheet-name: your-sheet-tab-name
```

### Run as a batch job (pipeline fires on startup, then exits)

```bash
./mvnw spring-boot:run
```

`address.pipeline.enabled=true` (the default) causes `AddressPipelineRunner` to execute the full pipeline immediately after startup and then shut down cleanly.

### Run as a persistent server (pipeline triggered via HTTP)

```bash
ADDRESS_PIPELINE_ENABLED=false ./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`. Trigger the pipeline via the REST endpoint below.

### Trigger the pipeline via curl

```bash
curl -X POST http://localhost:8080/api/v1/workflows/address-sync/run \
  -H "Content-Type: application/json" \
  -d '{
    "dryRun": true,
    "syncGoogleSheet": false,
    "enrichBuildium": true,
    "sheetId": "your-spreadsheet-id",
    "sheetName": "your-sheet-tab-name"
  }'
```

See [docs/runbook.md](docs/runbook.md) for the full set of curl examples and request options.

## Health and Status Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/ping` | Liveness check — returns `pong` and exercises DI wiring |
| `GET /api/v1/info` | Service metadata (name, framework, Java version) |
| `GET /api/v1/integrations/health` | Probes vendor connectivity (currently Buildium) |
| `GET /actuator/health` | Spring Boot internal health (JVM, disk) |
| `GET /actuator/metrics` | JVM and HTTP request metrics |

## Running Tests

```bash
./mvnw test
```

All tests use mocks — no live credentials required.

## Next Steps

1. Run a dry-run against the live sheet to validate match rates across all three systems
2. Flip `address.pipeline.dry-run=false` and validate Google Sheet write-back
3. Add API key or OAuth security to the workflow endpoints before exposing beyond localhost
4. Deploy to Google Cloud Run with Cloud Scheduler for scheduled nightly runs
5. Implement the Buildium-anchored pass for Loan Tape rows with no Salesforce record (see [docs/architecture.md](docs/architecture.md))
