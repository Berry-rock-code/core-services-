# Integration Hub — Architecture Overview

## Purpose

The integration hub synchronizes property address data across three systems:

- **Salesforce** — source of truth for Opportunity records, each of which carries a purchased property address
- **Google Sheets (Loan Tape)** — the operational working sheet maintained by the team, containing one row per property deal
- **Buildium** — the property management platform, containing rental unit and active-lease records

The goal of a pipeline run is to link each Loan Tape row to its matching Salesforce Opportunity and Buildium unit, then write the resolved IDs and quality metadata back into the sheet so downstream consumers do not need to access Salesforce or Buildium directly.

---

## Three-Phase Pipeline

### Phase 1 — Fetch

All three data sources are queried independently at the start of each run.

1. **Salesforce fetch** — `SalesforceClientImpl` authenticates via the JWT bearer token flow, issues a SOQL query against the Opportunity object, and paginates through all result pages. Only Opportunities with a non-null street address are returned.

2. **Loan Tape fetch** — `GoogleSheetsClientImpl` reads the full sheet range (`A:Z`) starting from the configured header row. The header row is parsed dynamically to build a column-name-to-index map, so the sheet layout can change without a code deployment.

3. **Buildium fetch** — `BuildiumClientImpl` calls `/v1/rentals/units` (for address data) and `/v1/leases?leasestatuses=Active` (for lease IDs), then joins the two result sets on unit ID. Units without a street address are skipped.

### Phase 2 — Normalize and Match

Before matching, every address string from all three sources is run through the normalization pipeline to produce a consistent uppercase, abbreviated, punctuation-free form. See [address-normalization.md](address-normalization.md) for the full rule set.

Two normalized lookup structures are built for Salesforce records:

| Map | Key | Contents |
|-----|-----|----------|
| `cleanSfByNormalized` | Full composite key (`ADDRESS\|CITY\|STATE\|ZIP`) | CLEAN-quality SF records |
| `cleanSfByAddressOnly` | Normalized street line only | CLEAN-quality SF records |
| `partialSfByAddressOnly` | Normalized street line only | PARTIAL-quality SF records |

Two equivalent structures are built for Buildium records. The Loan Tape rows are then matched using a **two-pass strategy**:

**Pass 1 — Full key match**
The normalized composite key for the Loan Tape row is looked up in `cleanSfByNormalized`. If found, the record is classified `SYNCED`.

**Pass 2 — Address-only fallback**
If Pass 1 fails, the normalized street line alone is looked up in `cleanSfByAddressOnly`. If found, the record is classified `SYNCED_ADDRESS_ONLY` (city/state/ZIP may differ between the sheet and Salesforce).

**Pass 3 — PARTIAL fallback**
If both of the above fail, the street line is looked up in `partialSfByAddressOnly`. If found, the record is classified `SYNCED_PARTIAL` (the Salesforce record itself was missing some address components).

If no SF match is found the row is logged as `[Debug][NoMatch]` and skipped.

For each row that matched Salesforce, the same two-pass strategy (full key, then address-only) is applied against the Buildium lookup maps:

- **Single Buildium match** — Buildium property and lease IDs are written to the update request.
- **Multiple Buildium matches** — classified `AMBIGUOUS_BUILDIUM_MATCH`; no Buildium IDs are written.
- **No Buildium match** — Salesforce data is still written back; Buildium columns are left blank.

### Phase 3 — Write Back

If `address.pipeline.dry-run` is `false`, `GoogleSheetsClientImpl.batchUpdateAddressMatches` is called with the full list of update requests. Each non-null field in each `SheetBatchUpdateRequest` becomes one `ValueRange` in a single Sheets API `batchUpdate` call.

In dry-run mode, the first 10 proposed updates are logged and no writes are made.

---

## Two-Pass Matching Strategy

The two-pass approach is a deliberate tradeoff between precision and recall:

- **Full key** matching is precise: city, state, and ZIP must all align. This is the default for clean addresses and produces a `SYNCED` status.
- **Address-only fallback** trades precision for recall: if the city/state/ZIP differ between the sheet and Salesforce (a common data entry inconsistency), the record can still be matched on street address alone. This produces `SYNCED_ADDRESS_ONLY` and flags to reviewers that manual validation is advisable.

A future planned enhancement is a **Buildium-anchored pass** for Loan Tape rows that have no Salesforce record at all. In this pass, the row would be matched directly against Buildium by street address, and the Buildium property ID would be written back without a Salesforce linkage. This handles properties that were acquired but not yet entered into Salesforce.

---

## Three-Tier Quality Classification

Before a Salesforce record enters any lookup map, `AddressQualityService.classify` assigns it one of three quality tiers:

| Tier | Condition | Pipeline treatment |
|------|-----------|--------------------|
| `CLEAN` | All four components present; no cross-field contamination detected | Eligible for full-key and address-only matching |
| `PARTIAL` | At least one of city, state, or ZIP is missing | Eligible for address-only fallback only (Pass 3) |
| `SUSPICIOUS` | Street line contains the ZIP, city, or state code, or contains "OK"/"OKC" as a standalone word | Excluded from all matching |

Counts for each tier are logged in the `[Summary]` block at the end of every pipeline run.

---

## Data Flow Diagram

```
+------------------+        SOQL query         +---------------------+
|   Salesforce     |-------------------------->| SalesforceClientImpl|
|  (Opportunities) |<--------------------------| JWT bearer auth     |
+------------------+    address records         +---------------------+
                                                          |
                                                          | List<SalesforceAddressRecord>
                                                          v
                                              +---------------------------+
+------------------+   Sheets API v4          |   AddressPipelineService  |
|  Google Sheets   |------------------------->|                           |
|  (Loan Tape)     |<-------------------------| normalize -> classify ->   |
+------------------+   batch update           |   two-pass match          |
                                              |   -> write-back           |
+------------------+   /v1/rentals/units      +---------------------------+
|    Buildium      |   /v1/leases             |
|  (Units/Leases)  |------------------------->|
+------------------+   address records        |
```

**Legend**

- Arrows from external systems into `AddressPipelineService` represent the **Fetch** phase.
- Processing inside the box represents the **Normalize + Match** phase.
- The arrow back to Google Sheets represents the **Write Back** phase.
- Buildium data is used read-only in the current pipeline; write-back to Buildium is not implemented.

---

## Planned: Buildium-Anchored Pass

Some Loan Tape rows correspond to properties that are actively managed in Buildium but have not yet been entered into Salesforce. The current pipeline cannot link these rows to Buildium because the SF-first matching strategy requires a Salesforce record to exist.

The planned Buildium-anchored pass would run after all SF-matched rows are resolved and process the remaining unmatched rows by looking them up directly in the Buildium address index. If a unique Buildium match is found, the Buildium property and lease IDs would be written back without a Salesforce ID. This will surface as a new sync status value, tentatively `BUILDIUM_ONLY`.
