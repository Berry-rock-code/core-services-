package com.berryrock.integrationhub.client;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SheetBatchUpdateRequest;

import java.util.List;

/**
 * Contract for all interactions with the Google Sheets API.
 *
 * Part of the client package — defines the operations needed by the address pipeline to
 * read the Loan Tape spreadsheet and write matched Salesforce and Buildium IDs back into it.
 *
 * The column layout of the sheet is configured via
 * {@link com.berryrock.integrationhub.config.AddressPipelineProperties.Header}; the
 * implementation resolves column positions dynamically from the header row rather than
 * using hard-coded column letters. This allows the sheet to gain or lose columns without
 * a code change.
 *
 * Implementations must honor the {@code integration.vendor.google-sheets.enabled} flag:
 * when {@code false}, all methods must return empty results or no-op without making any
 * outbound API calls.
 */
public interface GoogleSheetsClient
{
    /**
     * Performs a lightweight connectivity check against the Google Sheets API.
     *
     * Attempts to initialize the authenticated Sheets service client. Returns {@code false}
     * immediately if the integration is disabled.
     *
     * @return {@code true} if the Sheets API client can be initialized; {@code false} otherwise
     */
    boolean ping();

    /**
     * Reads all data rows from the specified sheet and maps them to typed row objects.
     *
     * Skips rows above the configured header row index. Reads the header row to build a
     * dynamic column-name-to-index map, then maps each subsequent row to a
     * {@link GoogleSheetAddressRow}. Columns whose header is not present in the sheet
     * are silently left as {@code null} on the row object.
     *
     * @param sheetId   Google Sheets spreadsheet ID
     * @param sheetName name of the sheet tab to read from
     * @return list of typed row objects; empty list if the sheet has no data or the
     *         integration is disabled
     */
    List<GoogleSheetAddressRow> fetchAddressRows(String sheetId, String sheetName);

    /**
     * Writes matched Salesforce and Buildium IDs back into the sheet using the Sheets API
     * batch update endpoint.
     *
     * Fetches the header row to determine column positions, then builds one
     * {@code ValueRange} per non-null field per update request. All updates are submitted
     * in a single {@code batchUpdate} call to minimize API quota usage. Only cells whose
     * corresponding value in the {@link SheetBatchUpdateRequest} is non-null are included
     * in the batch, so pre-existing values in other columns are not overwritten.
     *
     * @param sheetId   Google Sheets spreadsheet ID
     * @param sheetName name of the sheet tab to update
     * @param updates   list of per-row update requests; the method is a no-op if this is empty
     */
    void batchUpdateAddressMatches(String sheetId, String sheetName, List<SheetBatchUpdateRequest> updates);
}
