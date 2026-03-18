package com.berryrock.integrationhub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsClientImpl implements GoogleSheetsClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsClientImpl.class);

    // TODO: Inject Google Sheets client service configuration here

    @Override
    public boolean ping() {
        log.debug("Pinging Google Sheets API (placeholder)");
        return true;
    }

    @Override
    public java.util.List<com.berryrock.integrationhub.model.GoogleSheetAddressRow> fetchAddressRows(String sheetId, String sheetName) {
        log.info("Mock fetching Google Sheets rows from sheetId: {}, sheetName: {}", sheetId, sheetName);
        java.util.List<com.berryrock.integrationhub.model.GoogleSheetAddressRow> mockRows = new java.util.ArrayList<>();

        com.berryrock.integrationhub.model.GoogleSheetAddressRow row1 = new com.berryrock.integrationhub.model.GoogleSheetAddressRow();
        row1.setRowNumber(2);
        row1.setAddress("123 Main St");
        row1.setCity("St Louis");
        row1.setState("MO");
        row1.setPostalCode("63101");
        mockRows.add(row1);

        com.berryrock.integrationhub.model.GoogleSheetAddressRow row2 = new com.berryrock.integrationhub.model.GoogleSheetAddressRow();
        row2.setRowNumber(3);
        row2.setAddress("456 Elm Ave");
        row2.setCity("St Louis");
        row2.setState("MO");
        row2.setPostalCode("63102");
        mockRows.add(row2);

        // This row will not match any Salesforce record
        com.berryrock.integrationhub.model.GoogleSheetAddressRow row3 = new com.berryrock.integrationhub.model.GoogleSheetAddressRow();
        row3.setRowNumber(4);
        row3.setAddress("999 Pine Road");
        row3.setCity("Clayton");
        row3.setState("MO");
        row3.setPostalCode("63105");
        mockRows.add(row3);

        // This row will duplicate the first to test sheet side duplication warning
        com.berryrock.integrationhub.model.GoogleSheetAddressRow row4 = new com.berryrock.integrationhub.model.GoogleSheetAddressRow();
        row4.setRowNumber(5);
        row4.setAddress("123 Main St");
        row4.setCity("St Louis");
        row4.setState("MO");
        row4.setPostalCode("63101");
        mockRows.add(row4);

        return mockRows;
    }

    @Override
    public void batchUpdateAddressMatches(String sheetId, String sheetName, java.util.List<com.berryrock.integrationhub.model.SheetBatchUpdateRequest> updates) {
        log.info("Mock batch updating Google Sheets matches for sheetId: {}, sheetName: {}. Total updates: {}", sheetId, sheetName, updates.size());
        for (com.berryrock.integrationhub.model.SheetBatchUpdateRequest update : updates) {
            log.debug("Mock Update Row {}: sfId={}, buildiumId={}", update.getRowNumber(), update.getSalesforceId(), update.getBuildiumId());
        }
    }
}
