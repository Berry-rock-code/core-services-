# Integration Hub

A Java 21 + Spring Boot service that synchronizes property addresses across
Salesforce, a Google Sheet ops file, and Buildium so that Berry Rock has
end-to-end searchability across all three platforms.

## The Problem It Solves

Each system holds the same properties but with no shared identifier. A
Salesforce opportunity, a row in the ops Google Sheet, and a Buildium lease
all refer to the same physical address but have no way to find each other.
This service normalizes and matches addresses across all three, then writes
the Salesforce Opportunity ID and Buildium Lease ID back into the sheet so
ops staff have a single lookup point.

## How It Works

The pipeline is triggered via HTTP POST and runs in three phases.

**Phase 1 - Fetch**

Pulls all Closed Won opportunities from Salesforce (structured Google Maps
format address fields), all rows from the ops Google Sheet, and all active
lease addresses from Buildium via the rental units endpoint. Buildium leases
are fetched in bulk upfront and joined to units in memory by unit ID, keeping
total API calls to 2-3 regardless of portfolio size.

**Phase 2 - Normalize and Match**

Addresses are normalized (abbreviations standardized, punctuation stripped,
city aliases applied for STL, OKC, KC) and matched using a two-pass strategy.
Pass 1 matches on the full normalized key (address + city + state + zip).
Pass 2 falls back to address-line only for cases where city/state/zip differ
slightly between systems. Salesforce records are classified as CLEAN, PARTIAL,
or SUSPICIOUS before matching - only CLEAN records enter the pipeline.

**Phase 3 - Write Back**

Matched rows in the Google Sheet are updated with the Salesforce Opportunity
ID, Buildium Property ID, Buildium Lease ID, address quality flag, and sync
status. SYNCED means a full four-field key match. SYNCED_ADDRESS_ONLY means
a fallback address-line-only match. A dry-run mode reads and matches without
writing anything back.

## Stack

- Java 21
- Spring Boot 3.2.3
- Maven
- Google Cloud Platform (target deployment)
- Buildium REST API
- Salesforce REST API with JWT bearer auth
- Google Sheets API v4

## Package Architecture

- `config` - Spring Boot configuration and bean definitions
- `controller` - HTTP endpoints, routing only, no business logic
- `service` - Core business logic and pipeline orchestration
- `client` - Thin wrappers around vendor APIs (Buildium, Google Sheets, Salesforce)
- `model` / `dto` - Domain objects and API response representations
- `util` - Stateless utilities (address normalization, matching)
- `audit` - Centralized event logging

## Running Locally

### Prerequisites

- JDK 21
- Maven (`./mvnw` wrapper included)

### Configuration

Create `src/main/resources/application-local.yml` (gitignored) with your
credentials:

```yaml
integration:
  vendor:
    buildium:
      client-id: your-client-id
      client-secret: your-client-secret
    salesforce:
      enabled: true
      client-id: your-sf-client-id
      username: your-sf-username
      private-key-path: /path/to/private-key.pem

address:
  pipeline:
    dry-run: true
    sheet-id: your-google-sheet-id
    sheet-name: your-sheet-tab-name
```

### Starting the Server

```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

### Triggering the Pipeline

```bash
curl -X POST http://localhost:8080/api/v1/workflows/address-sync/run \
  -H "Content-Type: application/json" \
  -d '{
    "dryRun": true,
    "syncGoogleSheet": true,
    "enrichBuildium": true,
    "sheetId": "your-sheet-id",
    "sheetName": "your-sheet-tab-name"
  }'
```

### Health Check Endpoints

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/api/v1/ping`
- `http://localhost:8080/api/v1/info`
- `http://localhost:8080/api/v1/integrations/health`

## Running Tests

```bash
./mvnw test
```

19 tests across 7 test classes. All tests use mocks - no live credentials
required to run the suite.

## Next Steps

1. Configure credentials in `application-local.yml` and run first live
   dry-run to validate match rates across all three systems
2. Confirm Buildium lease response shape matches assumed structure
3. Flip dry-run off and validate Google Sheet write-back
4. Deploy to Google Cloud Run with Cloud Scheduler trigger
5. Add API key security to workflow endpoints before exposing beyond localhost
