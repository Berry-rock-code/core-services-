# Integration Hub — Address Pipeline Summary

## What it does

The pipeline links property records across three systems — **Salesforce** (source of truth for purchased properties), **Google Sheets / Loan Tape** (the team's operational working sheet), and **Buildium** (property management platform) — by matching addresses across all three and writing the resolved IDs back into the sheet. The end result is that each Loan Tape row gets a Salesforce Opportunity ID and Buildium property/lease ID populated automatically, so downstream consumers never need to touch either system directly.

---

## How it does it

### Phase 1 — Fetch

All three systems are queried independently. Salesforce is queried via SOQL over JWT bearer auth. The Loan Tape is read from a configured Google Sheet range, with column positions resolved dynamically from the header row so layout changes don't require code changes. Buildium unit addresses and active lease IDs are fetched from two endpoints and joined on unit ID.

### Phase 2 — Normalize

Every address string from every source is run through a deterministic 8-rule normalization pipeline:

| # | Rule | Example |
|---|------|---------|
| 1 | Uppercase | `123 Main St` → `123 MAIN ST` |
| 2 | Punctuation removal (`[.,#']`) | `St. John's Rd` → `ST JOHNS RD` |
| 3 | Whitespace collapse | `123  Main  St` → `123 MAIN ST` |
| 4 | ZIP+4 strip | `74101-4321` → `74101` |
| 5 | Street suffix abbreviation | `STREET` → `ST`, `AVENUE` → `AVE`, etc. |
| 6 | City alias resolution | `OKLAHOMA CITY` → `OKC`, `SAINT LOUIS` → `STL`, `KANSAS CITY` → `KC` |
| 7 | Directional abbreviation (compound before cardinal) | `NORTHWEST` → `NW`, `NORTH` → `N`, etc. |
| 8 | Trailing unit suffix strip | `123 MAIN ST - 12` → `123 MAIN ST` |

After normalization the four components are joined into a pipe-delimited lookup key:

```
123 N MAIN ST|OKC|OK|74101
```

### Phase 3 — Classify

Salesforce records are tiered before matching:

| Tier | Condition | Matching eligibility |
|------|-----------|----------------------|
| `CLEAN` | All four address components present | Full-key and address-only matching |
| `PARTIAL` | At least one of city, state, or ZIP is missing | Address-only fallback only (Pass 3) |
| `SUSPICIOUS` | Street field contains embedded city or ZIP | Excluded from all matching |

### Phase 4 — Match

Each Loan Tape row is matched against Salesforce in priority order:

| Pass | Strategy | Result status |
|------|----------|---------------|
| 1 | Full composite key (`address\|city\|state\|zip`) | `SYNCED` |
| 2 | Street line only, CLEAN SF records | `SYNCED_ADDRESS_ONLY` |
| 3 | Street line only, PARTIAL SF records | `SYNCED_PARTIAL` |

Matched rows then go through the same two-pass strategy against Buildium. Multiple Buildium hits produce `AMBIGUOUS_BUILDIUM_MATCH` and the Buildium columns are left blank for manual resolution.

### Phase 5 — Write Back

All resolved updates are sent to the Sheets API in a single batch call. Dry-run mode (`ADDRESS_PIPELINE_DRY_RUN=true`) runs all phases but skips the write and logs the first 10 proposed updates instead.

---

## Where it lives

| File | Role |
|------|------|
| `service/AddressPipelineService.java` | Orchestrates the startup-time batch pipeline (Phases 1–5) |
| `service/AddressSyncService.java` | Handles the same pipeline when triggered via the REST API |
| `util/AddressNormalizer.java` | Single canonical normalization implementation; shared by both service paths |
| `service/AddressQualityService.java` | Classifies Salesforce records as CLEAN / PARTIAL / SUSPICIOUS |
| `util/AddressMatcher.java` | Groups records into lookup maps by normalized key for matching |
| `client/SalesforceClientImpl.java` | Fetches Opportunities via SOQL over JWT bearer auth |
| `client/GoogleSheetsClientImpl.java` | Reads the Loan Tape and writes results back via Sheets API v4 |
| `client/BuildiumClientImpl.java` | Fetches unit addresses and active leases from Buildium |
| `config/AddressPipelineProperties.java` | Binds `address.pipeline.*` config (sheet ID, dry-run flag, column header names) |
| `test/.../AddressNormalizerTest.java` | Unit tests covering all 8 normalization rules and the composite key format |

All source files are under `src/main/java/com/berryrock/integrationhub/` and tests under `src/test/java/com/berryrock/integrationhub/`.
