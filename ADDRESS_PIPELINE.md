# Address Pipeline

The Address Pipeline replaces the previous standalone Go script. It fetches addresses from Salesforce, Normalizes and classifies them, Matches them with active Buildium Leases, and outputs the results back into a Google Sheet (Loan Tape).

## How to enable

You can enable the pipeline by setting the `address.pipeline.enabled` property to `true`. This property can be provided in `application.yml` or passed via environment variable:

```bash
ADDRESS_PIPELINE_ENABLED=true ./mvnw spring-boot:run
```

When enabled, the service will run the `AddressPipelineRunner` sequentially at startup, log the operations and summary, and gracefully shut down the application after executing.

## Dry-run Mode

By default, the application runs in dry-run mode (`address.pipeline.dry-run=true`). This ensures it will read from all systems, process mappings, and summarize output without committing write-backs to Google Sheets.

To execute live updates, explicitly pass `ADDRESS_PIPELINE_DRY_RUN=false` as an environment variable.

## Configuration Options

You must configure integrations, such as Salesforce (`SALESFORCE_ENABLED=true`), Google Sheets (`GOOGLE_SHEETS_ENABLED=true`), and Buildium (`BUILDIUM_ENABLED=true`) under `integration.vendor.*` for the pipeline to function correctly.

Google Sheet column headers can be mapped in `application.yml` under `address.pipeline.header.*`. If your sheet changes its header names, you can point the app to the new columns without code changes.

```yaml
address:
  pipeline:
    enabled: true
    dry-run: true
    sheet-id: "your-google-sheet-id"
    sheet-name: "Loan Tape"
    header:
      address: "Address"
      city: "City"
      state: "State"
      postal-code: "Zip"
      salesforce-id: "Salesforce ID"
      buildium-id: "Buildium ID"
      sf-standardized-address: "SF Standardized Address"
      sf-address-quality: "SF Address Quality"
      sf-address-sync-status: "SF Address Sync Status"
      buildium-lease-id: "Buildium lease ID"
      buildium-property-id: "Buildium Property ID"
```

## Failure Modes
- Integration Disabled: If Salesforce, Buildium, or Google Sheets integration is disabled, the pipeline will simply process empty data for that vendor.
- Unmatched Salesforce IDs: Rows in the Loan Tape that map to a Salesforce ID that wasn't flagged as "CLEAN" will be skipped.
- Ambiguous Leases: Buildium entries that map to multiple normalized addresses will be considered ambiguous and skipped.
