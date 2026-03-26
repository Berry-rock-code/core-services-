# Configuration Reference

All configuration is loaded from `src/main/resources/application.yml`. Every property that accepts a secret or environment-specific value has an environment variable override in the format shown below. Properties without a default are **required** in non-local environments.

Environment variable names follow Spring Boot's relaxed binding convention: dots become underscores, hyphens become underscores, and the result is uppercased. For example, `address.pipeline.dry-run` maps to `ADDRESS_PIPELINE_DRY_RUN`.

---

## address.pipeline.*

Controls the startup-time address sync pipeline run by `AddressPipelineRunner`.

---

### `address.pipeline.enabled`

| Field | Value |
|-------|-------|
| Type | boolean |
| Default | `true` |
| Required | No |
| Env override | `ADDRESS_PIPELINE_ENABLED` |

When `true`, the pipeline runs automatically on application startup via `AddressPipelineRunner` and the process exits cleanly after completion. Set to `false` to run the application as a persistent HTTP server without triggering the pipeline on startup.

---

### `address.pipeline.dry-run`

| Field | Value |
|-------|-------|
| Type | boolean |
| Default | `true` |
| Required | No |
| Env override | `ADDRESS_PIPELINE_DRY_RUN` |

When `true`, the full fetch, normalize, and match phases run but no data is written back to Google Sheets. The first 10 proposed updates are logged instead. Set to `false` only when you are ready to commit results to the live sheet.

**Important:** The default is `true` to prevent accidental writes in new deployments. You must explicitly set this to `false` in a production run.

---

### `address.pipeline.sheet-id`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty — resolves from `GOOGLE_SHEETS_SPREADSHEET_ID`) |
| Required | Yes |
| Env override | `GOOGLE_SHEETS_SPREADSHEET_ID` |

The Google Sheets spreadsheet ID for the Loan Tape. Found in the spreadsheet URL: `https://docs.google.com/spreadsheets/d/<spreadsheet-id>/edit`.

---

### `address.pipeline.sheet-name`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty — resolves from `GOOGLE_SHEETS_SHEET_NAME`) |
| Required | Yes |
| Env override | `GOOGLE_SHEETS_SHEET_NAME` |

The name of the sheet tab within the spreadsheet (e.g., `Sheet1` or `Loan Tape`). Must match the tab name exactly, including capitalization.

---

### `address.pipeline.header-row`

| Field | Value |
|-------|-------|
| Type | integer |
| Default | `2` |
| Required | No |
| Env override | `ADDRESS_PIPELINE_HEADER_ROW` |

The 1-based row index of the header row in the sheet. Rows above this index are skipped. Row `header-row` is treated as the header and used to build the column-name-to-index map. Data rows begin at `header-row + 1`.

---

### `address.pipeline.header.*`

These properties map logical field names to the exact column header text used in the Loan Tape. The pipeline reads the header row dynamically and resolves column positions by matching these strings. If a column does not exist in the sheet, its value in affected rows is silently left `null`.

| Property | Default value | Description |
|----------|---------------|-------------|
| `address.pipeline.header.address` | `Address` | Street address column |
| `address.pipeline.header.city` | `City` | City column |
| `address.pipeline.header.state` | `State` | State column |
| `address.pipeline.header.postal-code` | `Zip` | ZIP / postal code column |
| `address.pipeline.header.salesforce-id` | `Salesforce ID` | Salesforce Opportunity ID written back by the pipeline |
| `address.pipeline.header.buildium-id` | `Buildium ID` | Buildium property ID written back by the pipeline |
| `address.pipeline.header.sf-standardized-address` | `SF Standardized Address` | Standardized address from the matched SF record |
| `address.pipeline.header.sf-address-quality` | `SF Address Quality` | Quality tier (CLEAN / PARTIAL / SUSPICIOUS) |
| `address.pipeline.header.sf-address-sync-status` | `SF Address Sync Status` | Sync status (SYNCED / SYNCED_ADDRESS_ONLY / etc.) |
| `address.pipeline.header.buildium-lease-id` | `Buildium lease ID` | Buildium lease ID from the active-lease join |
| `address.pipeline.header.buildium-property-id` | `Buildium Property ID` | Buildium property ID |

---

## integration.vendor.buildium.*

Controls connectivity to the Buildium property management API.

---

### `integration.vendor.buildium.enabled`

| Field | Value |
|-------|-------|
| Type | boolean |
| Default | `true` |
| Required | No |
| Env override | `BUILDIUM_ENABLED` |

When `false`, all `BuildiumClient` methods return empty results immediately without making any HTTP calls. Use this to run the pipeline in Salesforce-to-sheet-only mode.

---

### `integration.vendor.buildium.base-url`

| Field | Value |
|-------|-------|
| Type | string |
| Default | `https://api.buildium.com` |
| Required | No |
| Env override | `BUILDIUM_BASE_URL` |

Base URL for the Buildium REST API. Do not include a trailing slash.

---

### `integration.vendor.buildium.client-id`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty) |
| Required | Yes |
| Env override | `BUILDIUM_CLIENT_ID` |

Buildium API client ID. Obtain from the Buildium developer portal. Sent as the `x-buildium-client-id` header on every request.

---

### `integration.vendor.buildium.client-secret`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty) |
| Required | Yes |
| Env override | `BUILDIUM_CLIENT_SECRET` |

Buildium API client secret. Sent as the `x-buildium-client-secret` header on every request. Treat as a credential — do not commit this value to source control.

---

### `integration.vendor.buildium.connect-timeout-seconds`

| Field | Value |
|-------|-------|
| Type | integer |
| Default | `10` |
| Required | No |
| Env override | `BUILDIUM_CONNECT_TIMEOUT_SECONDS` |

TCP connection timeout for outbound requests to the Buildium API.

---

### `integration.vendor.buildium.read-timeout-seconds`

| Field | Value |
|-------|-------|
| Type | integer |
| Default | `30` |
| Required | No |
| Env override | `BUILDIUM_READ_TIMEOUT_SECONDS` |

Read timeout for outbound requests to the Buildium API. Increase this if paginated unit/lease fetches time out on large portfolios.

---

### `integration.vendor.buildium.page-size`

| Field | Value |
|-------|-------|
| Type | integer |
| Default | `100` |
| Required | No |
| Env override | *(none — set in YAML)* |

Configured page size for Buildium API pagination. Note: `BuildiumClientImpl` overrides this internally to `1000` for the units and leases endpoints. The YAML value of `100` is used for the lower-level `getRentalsPage` method only.

---

## integration.vendor.google-sheets.*

Controls connectivity to the Google Sheets API.

---

### `integration.vendor.google-sheets.enabled`

| Field | Value |
|-------|-------|
| Type | boolean |
| Default | `true` |
| Required | No |
| Env override | `GOOGLE_SHEETS_ENABLED` |

When `false`, all `GoogleSheetsClient` methods return empty results or no-op without calling the API.

---

### `integration.vendor.google-sheets.application-name`

| Field | Value |
|-------|-------|
| Type | string |
| Default | `integration-hub` |
| Required | No |
| Env override | `GOOGLE_SHEETS_APPLICATION_NAME` |

The application name sent to the Google API client. Appears in the `User-Agent` header and in Google Cloud usage logs.

---

### `integration.vendor.google-sheets.spreadsheet-id`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty — resolves from `GOOGLE_SHEETS_SPREADSHEET_ID`) |
| Required | Yes |
| Env override | `GOOGLE_SHEETS_SPREADSHEET_ID` |

The Google Sheets spreadsheet ID. Same value as `address.pipeline.sheet-id`; both properties resolve from the same environment variable.

---

### `integration.vendor.google-sheets.credentials-path`

| Field | Value |
|-------|-------|
| Type | string (file path) |
| Default | (empty — resolves from `GOOGLE_APPLICATION_CREDENTIALS`) |
| Required | Yes in non-GCP environments |
| Env override | `GOOGLE_APPLICATION_CREDENTIALS` |

Absolute path to a Google service account JSON key file. If blank or the file cannot be read, the client falls back to Application Default Credentials (ADC), which works automatically on GCP Compute Engine and Cloud Run. In local development, set this to the path of a downloaded service account key.

---

## integration.vendor.salesforce.*

Controls connectivity to the Salesforce REST API.

---

### `integration.vendor.salesforce.enabled`

| Field | Value |
|-------|-------|
| Type | boolean |
| Default | `true` |
| Required | No |
| Env override | `SALESFORCE_ENABLED` |

When `false`, `SalesforceClient.fetchAddressesForGoogleSheetBuildiumSync()` returns an empty list without authenticating. Set to `false` for Buildium-only testing.

---

### `integration.vendor.salesforce.login-url`

| Field | Value |
|-------|-------|
| Type | string |
| Default | `https://login.salesforce.com` |
| Required | No |
| Env override | *(none — set in YAML)* |

The Salesforce OAuth2 token endpoint base URL. Use `https://test.salesforce.com` for sandbox environments.

---

### `integration.vendor.salesforce.client-id`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty) |
| Required | Yes |
| Env override | `SALESFORCE_CLIENT_ID` |

Salesforce Connected App consumer key. Used as the `iss` (issuer) claim in the JWT bearer token.

---

### `integration.vendor.salesforce.username`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty) |
| Required | Yes |
| Env override | `SALESFORCE_USERNAME` |

Salesforce username of the integration user. Used as the `sub` (subject) claim in the JWT bearer token. The user must have been pre-authorized for JWT bearer flow in the Connected App settings.

---

### `integration.vendor.salesforce.private-key-path`

| Field | Value |
|-------|-------|
| Type | string (file path) |
| Default | (empty) |
| Required | Yes |
| Env override | `SALESFORCE_PRIVATE_KEY_PATH` |

Absolute path to the PEM-formatted RSA private key file used to sign the JWT. The corresponding public certificate must be uploaded to the Salesforce Connected App. Treat the private key as a credential — do not commit it to source control.

---

### `integration.vendor.salesforce.api-version`

| Field | Value |
|-------|-------|
| Type | string |
| Default | `v61.0` |
| Required | No |
| Env override | *(none — set in YAML)* |

Salesforce REST API version used for SOQL queries. Update this when upgrading to a newer Salesforce release.

---

### `integration.vendor.salesforce.token-skew-seconds`

| Field | Value |
|-------|-------|
| Type | integer |
| Default | `60` |
| Required | No |
| Env override | `SALESFORCE_TOKEN_SKEW_SECONDS` |

Clock skew buffer in seconds added to the JWT expiry calculation. Accommodates minor time drift between the application server and the Salesforce token endpoint.

---

### `integration.vendor.salesforce.fields.*`

Maps logical field names to the actual Salesforce API field names on the Opportunity object. Changing these allows the pipeline to adapt to custom field API names without a code change.

| Property | Default | Description |
|----------|---------|-------------|
| `fields.id` | `Id` | Opportunity record ID |
| `fields.full-address` | `New_Purchase_Address__Street__s` | Street address from the compound address field |
| `fields.city` | `New_Purchase_Address__City__s` | City from the compound address field |
| `fields.state` | `New_Purchase_Address__StateCode__s` | State code from the compound address field |
| `fields.postal-code` | `New_Purchase_Address__PostalCode__s` | ZIP code from the compound address field |
| `fields.country-code` | `New_Purchase_Address__CountryCode__s` | Country code from the compound address field |
| `fields.stage` | `StageName` | Opportunity stage field, used with `query.stage-filter` |

---

### `integration.vendor.salesforce.query.stage-filter`

| Field | Value |
|-------|-------|
| Type | string |
| Default | (empty — no filter applied) |
| Required | No |
| Env override | *(none — set in YAML)* |

When non-empty, appends `AND StageName = '<value>'` to the SOQL WHERE clause. Use this to restrict the pipeline to Opportunities in a specific stage (e.g., `Closed Won`). When empty, all Opportunities with a non-null street address are returned.
