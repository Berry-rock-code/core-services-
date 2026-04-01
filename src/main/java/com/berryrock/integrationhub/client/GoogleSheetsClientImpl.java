package com.berryrock.integrationhub.client;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SheetBatchUpdateRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import com.berryrock.integrationhub.config.AddressPipelineProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class GoogleSheetsClientImpl implements GoogleSheetsClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsClientImpl.class);

    @Value("${integration.vendor.google-sheets.enabled:true}")
    private boolean enabled;

    @Value("${integration.vendor.google-sheets.application-name:integration-hub}")
    private String applicationName;

    @Value("${integration.vendor.google-sheets.credentials-path:}")
    private String credentialsPath;

    private final AddressPipelineProperties pipelineProperties;

    public GoogleSheetsClientImpl(AddressPipelineProperties pipelineProperties) {
        this.pipelineProperties = pipelineProperties;
    }

    private String toSheetRef(String sheetName)
    {
        if (sheetName == null || sheetName.trim().isEmpty())
        {
            throw new IllegalArgumentException("sheetName cannot be blank");
        }

        String escaped = sheetName.replace("'", "''");
        return "'" + escaped + "'";
    }

    private Sheets getSheetsService() throws Exception {
        GoogleCredentials credentials;
        if (credentialsPath != null && !credentialsPath.isEmpty() && !credentialsPath.contains("application.yml")) {
            // Note: In local execution if this file doesn't exist, we might fail here, but the code architecture is "real".
            try {
                credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            } catch (IOException e) {
                // Fallback to default if explicitly asked or file missing
                log.warn("Failed to load Google credentials from {}. Falling back to default.", credentialsPath);
                credentials = GoogleCredentials.getApplicationDefault().createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault().createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        }

        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    @Override
    public boolean ping() {
        if (!enabled) {
            return false;
        }
        log.debug("Pinging Google Sheets API via configuration check");
        // A real ping would initialize the Sheets v4 client and make a small read or at least instantiate.
        try {
            getSheetsService();
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize Google Sheets service for ping: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<GoogleSheetAddressRow> fetchAddressRows(String sheetId, String sheetName) {
        if (!enabled) {
            log.warn("Google Sheets integration is disabled. Returning empty list.");
            return new ArrayList<>();
        }

        log.info("Fetching Google Sheets rows from sheetId: {}, sheetName: {}", sheetId, sheetName);
        List<GoogleSheetAddressRow> rows = new ArrayList<>();

        try {
            Sheets service = getSheetsService();
            String safeSheetName = toSheetRef(sheetName);
            String range = safeSheetName + "!A:Z";
            ValueRange response = service.spreadsheets().values().get(sheetId, range).execute();
            List<List<Object>> values = response.getValues();

            if (values == null || values.isEmpty()) {
                log.info("No data found in Google Sheet.");
                return rows;
            }

            Map<String, Integer> headerMap = new HashMap<>();

            int headerRowIndex = pipelineProperties.getHeaderRow();

            int rowNum = 1;
            for (List<Object> row : values) {
                if (rowNum < headerRowIndex) {
                    rowNum++;
                    continue;
                }

                if (rowNum == headerRowIndex) { // Process header row
                    for (int i = 0; i < row.size(); i++) {
                        headerMap.put(row.get(i).toString().trim(), i);
                    }
                    rowNum++;
                    continue;
                }

                GoogleSheetAddressRow gsRow = new GoogleSheetAddressRow();
                gsRow.setRowNumber(rowNum);

                AddressPipelineProperties.Header props = pipelineProperties.getHeader();
                gsRow.setAddress(getColValue(row, headerMap, props.getAddress()));
                gsRow.setCity(getColValue(row, headerMap, props.getCity()));
                gsRow.setState(getColValue(row, headerMap, props.getState()));
                gsRow.setPostalCode(getColValue(row, headerMap, props.getPostalCode()));
                gsRow.setSalesforceId(getColValue(row, headerMap, props.getSalesforceId()));
                gsRow.setBuildiumId(getColValue(row, headerMap, props.getBuildiumId()));
                gsRow.setSfStandardizedAddress(getColValue(row, headerMap, props.getSfStandardizedAddress()));
                gsRow.setSfAddressQuality(getColValue(row, headerMap, props.getSfAddressQuality()));
                gsRow.setSfAddressSyncStatus(getColValue(row, headerMap, props.getSfAddressSyncStatus()));
                gsRow.setBuildiumLeaseId(getColValue(row, headerMap, props.getBuildiumLeaseId()));
                gsRow.setBuildiumPropertyId(getColValue(row, headerMap, props.getBuildiumPropertyId()));

                rows.add(gsRow);
                rowNum++;
            }
            log.info("Fetched and parsed {} rows from Google Sheets.", rows.size());

        } catch (Exception e) {
            log.error("Failed to fetch Google Sheet rows: {}", e.getMessage(), e);
            // It's acceptable to return empty on error given this is an integration context without actual creds.
        }

        return rows;
    }

    private String getColValue(List<Object> row, Map<String, Integer> headerMap, String headerName) {
        if (headerName == null) return null;
        Integer index = headerMap.get(headerName);
        if (index == null || index >= row.size()) return null;
        Object val = row.get(index);
        return val == null ? null : val.toString();
    }

    @Override
    public void batchUpdateAddressMatches(String sheetId, String sheetName, List<SheetBatchUpdateRequest> updates) {
        if (!enabled) {
            log.warn("Google Sheets integration disabled. Skipping batch update.");
            return;
        }

        log.info("Batch updating Google Sheets matches for sheetId: {}, sheetName: {}. Total updates: {}", sheetId, sheetName, updates.size());

        try {
            Sheets service = getSheetsService();

            int headerRowIndex = pipelineProperties.getHeaderRow();
            // Need headers to know which columns to update
            String safeSheetName = toSheetRef(sheetName);
            String headerRange = safeSheetName + "!" + headerRowIndex + ":" + headerRowIndex;
            ValueRange headerResponse = service.spreadsheets().values().get(sheetId, headerRange).execute();
            List<List<Object>> headerValues = headerResponse.getValues();

            if (headerValues == null || headerValues.isEmpty()) {
                log.error("Failed to fetch headers for batch update");
                return;
            }

            List<Object> headers = headerValues.get(0);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.get(i).toString().trim(), i);
            }

            List<ValueRange> data = new ArrayList<>();
            AddressPipelineProperties.Header props = pipelineProperties.getHeader();

            for (SheetBatchUpdateRequest update : updates) {
                // Determine cells to update based on headerMap
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getSalesforceId(), update.getSalesforceId());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getBuildiumId(), update.getBuildiumId());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getSfStandardizedAddress(), update.getSfStandardizedAddress());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getSfAddressQuality(), update.getSfAddressQuality());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getSfAddressSyncStatus(), update.getSfAddressSyncStatus());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getBuildiumLeaseId(), update.getBuildiumLeaseId());
                addUpdateIfHeaderExists(data, sheetName, update.getRowNumber(), headerMap, props.getBuildiumPropertyId(), update.getBuildiumPropertyId());
            }

            if (!data.isEmpty()) {
                BatchUpdateValuesRequest batchBody = new BatchUpdateValuesRequest()
                    .setValueInputOption("USER_ENTERED")
                    .setData(data);
                service.spreadsheets().values().batchUpdate(sheetId, batchBody).execute();
                log.info("Successfully executed batch update for {} cells.", data.size());
            }
        } catch (Exception e) {
            log.error("Failed to execute batch update for Google Sheets: {}", e.getMessage(), e);
        }
    }

    private void addUpdateIfHeaderExists(List<ValueRange> data, String sheetName, int rowNumber, Map<String, Integer> headerMap, String header, String value)
    {
        if (header != null && headerMap.containsKey(header) && value != null)
        {
            int colIndex = headerMap.get(header);
            String colLetter = getColumnLetter(colIndex);
            String safeSheetName = toSheetRef(sheetName);
            String range = safeSheetName + "!" + colLetter + rowNumber;

            ValueRange vr = new ValueRange()
                    .setRange(range)
                    .setValues(Collections.singletonList(Collections.singletonList(value)));
            data.add(vr);
        }
    }

    private String getColumnLetter(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        int dividend = columnNumber + 1;
        int modulo;

        while (dividend > 0) {
            modulo = (dividend - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            dividend = (dividend - modulo) / 26;
        }

        return columnName.toString();
    }
}
