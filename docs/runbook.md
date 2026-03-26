# Runbook — Integration Hub Address Pipeline

## Running the Pipeline Locally

### Prerequisites

- Java 21
- Maven wrapper (`./mvnw`) present in the repo root
- A `.env` file or shell environment with the variables listed in [configuration-reference.md](configuration-reference.md)
- A valid Salesforce RSA private key file at the path set in `SALESFORCE_PRIVATE_KEY_PATH`
- A Google service account JSON key file at the path set in `GOOGLE_APPLICATION_CREDENTIALS`

### Dry Run (no writes to Google Sheets)

A dry run fetches and matches all data but does not write anything back to the sheet. This is the safe default.

```bash
# Using the Maven wrapper
ADDRESS_PIPELINE_DRY_RUN=true \
ADDRESS_PIPELINE_ENABLED=true \
GOOGLE_SHEETS_SPREADSHEET_ID=<your-spreadsheet-id> \
GOOGLE_SHEETS_SHEET_NAME="<your-sheet-tab-name>" \
SALESFORCE_CLIENT_ID=<your-client-id> \
SALESFORCE_USERNAME=<your-sf-username> \
SALESFORCE_PRIVATE_KEY_PATH=/path/to/private.key \
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
BUILDIUM_CLIENT_ID=<your-buildium-id> \
BUILDIUM_CLIENT_SECRET=<your-buildium-secret> \
./mvnw spring-boot:run
```

The process will print pipeline summary logs and exit automatically.

### Live Run (writes results to Google Sheets)

Set `ADDRESS_PIPELINE_DRY_RUN=false`. Everything else is the same as above.

```bash
ADDRESS_PIPELINE_DRY_RUN=false \
... (same env vars as above) \
./mvnw spring-boot:run
```

**Double-check the sheet name and spreadsheet ID before running live.** The pipeline will overwrite existing values in the Salesforce ID, Buildium ID, quality, and sync status columns.

---

## Triggering via curl

When the application is running as a server (i.e., `address.pipeline.enabled=false`), use the REST endpoint to trigger a sync run.

### Dry run via curl

```bash
curl -X POST http://localhost:8080/api/v1/workflows/address-sync/run \
  -H "Content-Type: application/json" \
  -d '{
    "dryRun": true,
    "syncGoogleSheet": false,
    "enrichBuildium": true,
    "sheetId": "<your-spreadsheet-id>",
    "sheetName": "<your-sheet-tab-name>"
  }'
```

### Live run via curl

```bash
curl -X POST http://localhost:8080/api/v1/workflows/address-sync/run \
  -H "Content-Type: application/json" \
  -d '{
    "dryRun": false,
    "syncGoogleSheet": true,
    "enrichBuildium": true,
    "sheetId": "<your-spreadsheet-id>",
    "sheetName": "<your-sheet-tab-name>"
  }'
```

### Run against a local CSV instead of the live sheet

The `csvPath` field overrides the Google Sheets fetch. The CSV must have rows in the format `rowNumber,address` (no header line). This is useful for testing against a sample without valid Google credentials.

```bash
curl -X POST http://localhost:8080/api/v1/workflows/address-sync/run \
  -H "Content-Type: application/json" \
  -d '{
    "dryRun": true,
    "enrichBuildium": false,
    "csvPath": "/tmp/test-addresses.csv"
  }'
```

---

## Reading the Summary Log Output

After each pipeline run the application emits a `[Summary]` block in the logs. Here is what each line means:

```
[Summary] Salesforce Records Fetched: 412
```
Total number of Opportunity records returned by the SOQL query before quality filtering.

```
[Summary] CLEAN: 380, PARTIAL: 28, SUSPICIOUS: 4
```
Breakdown of quality tiers. CLEAN records are eligible for all matching passes. PARTIAL records participate in the address-only fallback only. SUSPICIOUS records are excluded.

```
[Summary] Buildium Active Leases: 218
```
Number of Buildium unit records that had a street address and were indexed for matching.

```
[Summary] Buildium Missing Tenant Address: 3
```
Buildium unit records skipped because the `AddressLine1` field (and all fallback field names) were blank.

```
[Summary] Unique Indexed Buildium Keys: 215
```
Number of distinct normalized keys in the Buildium lookup map. A count lower than the lease count indicates duplicate addresses in Buildium (multiple units at the same address).

```
[Summary] CLEAN rows matched to SF: 198
```
Loan Tape rows that matched a Salesforce record via any pass (full key, address-only, or PARTIAL fallback).

```
[Summary] PARTIAL rows matched to SF (Pass 3): 12
```
Rows that matched only because the Salesforce record itself was PARTIAL (missing city/state/ZIP). These are lower-confidence matches.

```
[Summary] Rows with no SF match: 47
```
Loan Tape rows for which no Salesforce record was found after all three passes. See the `[Debug][NoMatch]` lines for details.

```
[Summary] LT Matched to Buildium: 163
```
Of the rows that matched Salesforce, the number that also found exactly one Buildium record.

```
[Summary] LT No Buildium Match: 35
```
Rows that matched Salesforce but found no Buildium unit at the same normalized address.

```
[Summary] LT Ambiguous Buildium Match: 0
```
Rows that matched Salesforce but found more than one Buildium unit at the same normalized address. These receive a sync status of `AMBIGUOUS_BUILDIUM_MATCH` and no Buildium IDs are written.

```
[Summary] LT Skipped rows: 3
```
Loan Tape rows where both the full key and the address-only key were blank (the row had no address data at all).

```
[Summary] Pending Updates Count: 210
```
Number of sheet rows that will be (or were) updated. In dry-run mode this is logged only; in live mode this is the batch sent to the Sheets API.

```
[Summary] DRY RUN - No updates committed to Google Sheets.
```
Confirms dry-run mode was active. Followed by up to 10 sample update previews.

---

## Sync Status Values

The `SF Address Sync Status` column in the sheet is written by the pipeline. Here is what each value means:

| Status | Meaning |
|--------|---------|
| `SYNCED` | Full composite key match (address, city, state, ZIP all aligned between the Loan Tape row and the Salesforce record). Highest confidence. |
| `SYNCED_ADDRESS_ONLY` | Matched on street address line only. City, state, or ZIP differed between the sheet and Salesforce. Review the standardized address column to confirm it is the right property. |
| `SYNCED_PARTIAL` | The Salesforce record itself was PARTIAL (missing at least one address component). Matched on street address line only. Lower confidence — the Salesforce data should be corrected. |
| `AMBIGUOUS_BUILDIUM_MATCH` | Matched Salesforce successfully, but multiple Buildium units share the same normalized address. Buildium IDs were not written. Manual disambiguation is needed. |

---

## Diagnosing Low Match Rates

If the `[Summary] Rows with no SF match` count is unexpectedly high, check the `[Debug][NoMatch]` log lines:

```
[Debug] No SF match for row 47 -> rawAddress='123 N Main St Oklahoma City OK 74101',
  parsedAddress='123 N Main St', parsedCity='Oklahoma City', parsedState='OK',
  parsedZip='74101', fullKey='123 N MAIN ST|OKC|OK|74101',
  addressOnlyKey='123 N MAIN ST', partialCandidate='none'
```

Each field in this log line is useful for diagnosis:

- **rawAddress** — the exact string from the sheet before any parsing
- **parsedAddress / parsedCity / parsedState / parsedZip** — result of the address splitting logic that tries to parse combined one-line addresses into components
- **fullKey** — the normalized key the pipeline tried to look up in the Salesforce map
- **addressOnlyKey** — the street-line-only key tried in pass 2 and 3
- **partialCandidate** — if a PARTIAL Salesforce record existed for this address, its opportunity ID is shown here; if it says `none`, there is no Salesforce record at all for this address

Common causes of missed matches:

1. **The address in the sheet is formatted differently than in Salesforce.** Compare `fullKey` to the key shown for similar records in Salesforce. A unit number embedded in the Salesforce street field (e.g., `123 Main St Apt 4`) will not match a sheet row that stores the unit number in a separate column.

2. **The Salesforce record is SUSPICIOUS.** Check the SF Opportunity in Salesforce — the street field may contain the city or ZIP code in addition to the street address.

3. **The Salesforce record does not exist.** The `partialCandidate` will be `none`. The property may not yet be in Salesforce.

4. **The sheet address is a combined one-line string that the parser cannot split.** Check `parsedAddress` — if it equals the full `rawAddress`, parsing failed. The full one-line string is then normalized and compared as-is, which may not match if Salesforce stores the components separately.

---

## Health Check Endpoints

### Application liveness

```
GET /api/v1/ping
```

Returns `{"status":"SUCCESS","data":"pong"}` if the application is running and all integration client beans are wired. A failure here indicates a startup or dependency injection problem.

### Integration connectivity

```
GET /api/v1/integrations/health
```

Returns a map of vendor-to-boolean reachability:

```json
{
  "buildium": true
}
```

`true` means the Buildium API responded to a lightweight probe. `false` means either the integration is disabled or the API is unreachable (check credentials and network connectivity).

### Actuator health

```
GET /actuator/health
```

Standard Spring Boot health endpoint. Reports application-level health (disk, JVM, etc.) but does not probe external vendor APIs.

### Actuator metrics

```
GET /actuator/metrics
```

Lists available JVM and HTTP metrics. Useful for observing request counts and latency during a pipeline run.
