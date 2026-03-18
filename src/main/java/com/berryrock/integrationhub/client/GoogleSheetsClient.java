package com.berryrock.integrationhub.client;

/**
 * Placeholder for Google Sheets Integration Client.
 * 
 * Future Implementation Details:
 * - Load credentials via standard fallback (JSON string -> File Path -> Default GCP Service Account).
 * - Initialize Google Sheets v4 API Client.
 * - Map operations to ensure sheet exists, fetch values (A1 notation), update/append rows.
 * - Port custom "UPSERT" business logic built on top of basic API limits here.
 */
public interface GoogleSheetsClient {
    boolean ping();
    
    // TODO: Add methods like void upsertRows(String sheetTitle, List<List<Object>> rows)

    java.util.List<com.berryrock.integrationhub.model.GoogleSheetAddressRow> fetchAddressRows(String sheetId, String sheetName);
    void batchUpdateAddressMatches(String sheetId, String sheetName, java.util.List<com.berryrock.integrationhub.model.SheetBatchUpdateRequest> updates);
}
