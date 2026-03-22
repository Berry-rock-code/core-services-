package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.audit.AuditLogService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import com.berryrock.integrationhub.model.SheetBatchUpdateRequest;
import com.berryrock.integrationhub.util.AddressMatcher;
import com.berryrock.integrationhub.util.AddressNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AddressSyncService {

    private static final Logger log = LoggerFactory.getLogger(AddressSyncService.class);

    private final SalesforceClient salesforceClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final BuildiumClient buildiumClient;
    private final AuditLogService auditLogService;
    private final AddressNormalizer addressNormalizer;
    private final AddressMatcher addressMatcher;

    public AddressSyncService(SalesforceClient salesforceClient,
                              GoogleSheetsClient googleSheetsClient,
                              BuildiumClient buildiumClient,
                              AuditLogService auditLogService,
                              AddressNormalizer addressNormalizer,
                              AddressMatcher addressMatcher) {
        this.salesforceClient = salesforceClient;
        this.googleSheetsClient = googleSheetsClient;
        this.buildiumClient = buildiumClient;
        this.auditLogService = auditLogService;
        this.addressNormalizer = addressNormalizer;
        this.addressMatcher = addressMatcher;
    }

    public AddressSyncSummary runSync(AddressSyncRequest request) {
        AddressSyncSummary summary = new AddressSyncSummary();
        summary.setStatus("SUCCESS");

        if (request.isDryRun()) {
            summary.getWarnings().add("Dry run enabled. No external systems were modified.");
        }

        // 1. Fetch
        List<SalesforceAddressRecord> sfRecords = salesforceClient.fetchAddressesForGoogleSheetBuildiumSync();
        List<GoogleSheetAddressRow> gsRows;

        if (request.getCsvPath() != null && !request.getCsvPath().isEmpty()) {
            log.info("Loading local CSV test data from: {}", request.getCsvPath());
            gsRows = readLocalCsv(request.getCsvPath(), summary);
        } else {
            gsRows = googleSheetsClient.fetchAddressRows(request.getSheetId(), request.getSheetName());
        }

        List<BuildiumAddressRecord> bdRecords = request.isEnrichBuildium()
                ? buildiumClient.fetchActiveLeaseAddresses()
                : new ArrayList<>();

        summary.setSalesforceRecordsFetched(sfRecords.size());
        summary.setGoogleSheetRowsFetched(gsRows.size());
        summary.setBuildiumRecordsFetched(bdRecords.size());

        // 2. Normalize
        sfRecords.forEach(r -> r.setNormalizedAddress(
                createFullNormalizedAddress(r.getAddressLine(), r.getCity(), r.getState(), r.getPostalCode())
        ));
        gsRows.forEach(r -> r.setNormalizedAddress(
                createFullNormalizedAddress(r.getAddress(), r.getCity(), r.getState(), r.getPostalCode())
        ));
        bdRecords.forEach(r -> r.setNormalizedAddress(
                createFullNormalizedAddress(r.getRawAddress(), r.getCity(), r.getState(), r.getPostalCode())
        ));

        // 3. Group / Index
        Map<String, List<SalesforceAddressRecord>> sfMap = addressMatcher.groupRecordsByNormalizedAddress(sfRecords, SalesforceAddressRecord::getNormalizedAddress);
        Map<String, List<GoogleSheetAddressRow>> gsMap = addressMatcher.groupRecordsByNormalizedAddress(gsRows, GoogleSheetAddressRow::getNormalizedAddress);
        Map<String, List<BuildiumAddressRecord>> bdMap = addressMatcher.groupRecordsByNormalizedAddress(bdRecords, BuildiumAddressRecord::getNormalizedAddress);

        // Check for duplicates
        checkForDuplicates(sfMap, "Salesforce records", summary);
        checkForDuplicates(gsMap, "Google Sheet rows", summary);
        checkForDuplicates(bdMap, "Buildium records", summary);

        // 4. Match
        int sfToGsMatches = 0;
        int gsToBdMatches = 0;
        int unmatchedCount = 0;

        List<SheetBatchUpdateRequest> updates = new ArrayList<>();

        for (Map.Entry<String, List<SalesforceAddressRecord>> entry : sfMap.entrySet()) {
            String normAddress = entry.getKey();
            List<SalesforceAddressRecord> sfs = entry.getValue();

            // Just use the first one if there are duplicates
            SalesforceAddressRecord sf = sfs.get(0);

            if (gsMap.containsKey(normAddress)) {
                List<GoogleSheetAddressRow> gss = gsMap.get(normAddress);
                GoogleSheetAddressRow gs = gss.get(0); // Take first

                // Only count the unique normalized address once
                sfToGsMatches++;

                String buildiumId = null;
                if (bdMap.containsKey(normAddress)) {
                    buildiumId = bdMap.get(normAddress).get(0).getBuildiumPropertyId();
                }

                if (request.isSyncGoogleSheet() && !request.isDryRun()) {
                    SheetBatchUpdateRequest req = new SheetBatchUpdateRequest();
                    req.setRowNumber(gs.getRowNumber());
                    req.setSalesforceId(sf.getOpportunityId());
                    req.setBuildiumId(buildiumId);
                    updates.add(req);
                }
            } else {
                unmatchedCount++;
            }
        }

        // Count Sheets to Buildium Matches for the summary (unique normalized addresses)
        for (String normAddress : gsMap.keySet()) {
            if (bdMap.containsKey(normAddress)) {
                gsToBdMatches++;
            }
        }

        summary.setGoogleSheetMatches(sfToGsMatches);
        summary.setBuildiumMatches(gsToBdMatches);
        summary.setUnmatchedCount(unmatchedCount);

        // 5. Write back
        if (!updates.isEmpty() && (request.getCsvPath() == null || request.getCsvPath().isEmpty())) {
            googleSheetsClient.batchUpdateAddressMatches(request.getSheetId(), request.getSheetName(), updates);
        }

        return summary;
    }

    private List<GoogleSheetAddressRow> readLocalCsv(String csvPath, AddressSyncSummary summary) {
        List<GoogleSheetAddressRow> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Simple parse assuming format: rowNumber,"address"
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    try {
                        int rowNum = Integer.parseInt(parts[0].trim());
                        String addr = parts[1].trim();
                        if (addr.startsWith("\"") && addr.endsWith("\"")) {
                            addr = addr.substring(1, addr.length() - 1);
                        }

                        GoogleSheetAddressRow row = new GoogleSheetAddressRow();
                        row.setRowNumber(rowNum);
                        row.setAddress(addr);
                        rows.add(row);
                    } catch (NumberFormatException e) {
                        // Skip header or malformed row
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to read CSV at path: {}", csvPath, e);
            summary.getWarnings().add("Failed to load local CSV: " + e.getMessage());
        }
        return rows;
    }

    private String createFullNormalizedAddress(String address, String city, String state, String zip) {
        String normAddr = addressNormalizer.normalize(address);
        String normCity = addressNormalizer.normalizeCity(city);

        StringBuilder sb = new StringBuilder();
        if (normAddr != null) sb.append(normAddr);
        if (normCity != null) sb.append(" ").append(normCity);
        if (state != null) sb.append(" ").append(state.toUpperCase());
        if (zip != null) sb.append(" ").append(zip);

        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private <T> void checkForDuplicates(Map<String, List<T>> map, String source, AddressSyncSummary summary) {
        for (Map.Entry<String, List<T>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                summary.getWarnings().add(String.format("Duplicate %s found for normalized address: %s", source, entry.getKey()));
            }
        }
    }

    // For older tests that might call run(request) instead of runSync
    public AddressSyncSummary run(AddressSyncRequest request) {
        return runSync(request);
    }
}
