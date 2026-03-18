package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.audit.AuditLogService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.model.*;
import com.berryrock.integrationhub.util.AddressNormalizer;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AddressSyncService {

    private final SalesforceClient salesforceClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final BuildiumClient buildiumClient;
    private final AuditLogService auditLogService;

    public AddressSyncService(SalesforceClient salesforceClient,
                              GoogleSheetsClient googleSheetsClient,
                              BuildiumClient buildiumClient,
                              AuditLogService auditLogService) {
        this.salesforceClient = salesforceClient;
        this.googleSheetsClient = googleSheetsClient;
        this.buildiumClient = buildiumClient;
        this.auditLogService = auditLogService;
    }

    public AddressSyncSummary run(AddressSyncRequest request) {
        long startTime = System.currentTimeMillis();
        AddressSyncSummary summary = new AddressSyncSummary();
        summary.setStatus("SUCCESS");

        auditLogService.logWorkflowStarted(request.isDryRun());

        try {
            // 1 & 2. Fetch and Normalize Salesforce Records
            List<SalesforceAddressRecord> sfRecords = salesforceClient.fetchAddressesForGoogleSheetBuildiumSync();
            summary.setSalesforceRecordsFetched(sfRecords.size());
            auditLogService.logSalesforceFetchCompleted(sfRecords.size());

            Map<String, List<SalesforceAddressRecord>> sfMap = new HashMap<>();
            for (SalesforceAddressRecord record : sfRecords) {
                String normAddress = AddressNormalizer.normalize(record.getAddressLine());
                String normCity = AddressNormalizer.normalizeCity(record.getCity());
                String key = AddressNormalizer.buildComparableKey(normAddress, normCity, record.getState(), record.getPostalCode());
                record.setNormalizedAddress(key);

                sfMap.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }

            // 3 & 4. Fetch and Normalize Google Sheet Rows
            List<GoogleSheetAddressRow> sheetRows = new ArrayList<>();
            if (request.isSyncGoogleSheet() || request.isEnrichBuildium()) { // Only fetch if needed
                sheetRows = googleSheetsClient.fetchAddressRows(request.getSheetId(), request.getSheetName());
                summary.setGoogleSheetRowsFetched(sheetRows.size());
                auditLogService.logGoogleSheetFetchCompleted(sheetRows.size());

                for (GoogleSheetAddressRow row : sheetRows) {
                    String normAddress = AddressNormalizer.normalize(row.getAddress());
                    String normCity = AddressNormalizer.normalizeCity(row.getCity());
                    String key = AddressNormalizer.buildComparableKey(normAddress, normCity, row.getState(), row.getPostalCode());
                    row.setNormalizedAddress(key);
                }
            }

            // 5. Match Salesforce -> Google Sheet
            int sheetMatches = 0;
            int sheetDuplicates = 0;
            List<SheetBatchUpdateRequest> batchUpdates = new ArrayList<>();
            Set<String> matchedSfKeys = new HashSet<>();

            if (request.isSyncGoogleSheet()) {
                // Map sheet rows by key to check for duplicates in sheet
                Map<String, List<GoogleSheetAddressRow>> sheetMap = new HashMap<>();
                for (GoogleSheetAddressRow row : sheetRows) {
                    sheetMap.computeIfAbsent(row.getNormalizedAddress(), k -> new ArrayList<>()).add(row);
                }

                for (Map.Entry<String, List<SalesforceAddressRecord>> entry : sfMap.entrySet()) {
                    String key = entry.getKey();
                    List<SalesforceAddressRecord> sfMatches = entry.getValue();

                    if (sfMatches.size() > 1) {
                        summary.addWarning("Duplicate Salesforce records found for key: " + key);
                        sheetDuplicates++;
                        continue; // skip ambiguous
                    }

                    List<GoogleSheetAddressRow> rowMatches = sheetMap.get(key);
                    if (rowMatches != null) {
                        if (rowMatches.size() > 1) {
                            summary.addWarning("Duplicate Google Sheet rows found for key: " + key);
                            sheetDuplicates++;
                            continue; // skip ambiguous
                        }

                        // Perfect match
                        SalesforceAddressRecord sfRecord = sfMatches.get(0);
                        GoogleSheetAddressRow sheetRow = rowMatches.get(0);

                        // Assuming we only update if it doesn't have an ID
                        if (sheetRow.getSalesforceId() == null || !sheetRow.getSalesforceId().equals(sfRecord.getOpportunityId())) {
                             batchUpdates.add(new SheetBatchUpdateRequest(sheetRow.getRowNumber(), sfRecord.getOpportunityId(), sheetRow.getBuildiumId()));
                        }

                        sheetMatches++;
                        matchedSfKeys.add(key);
                    }
                }
                summary.setGoogleSheetMatches(sheetMatches);
                auditLogService.logSalesforceToGoogleSheetMatchesComplete(sheetMatches, sheetDuplicates);
            }

            // 6. Optionally write matched identifiers
            boolean skipWrite = !request.isSyncGoogleSheet() || request.isDryRun();
            if (!skipWrite && !batchUpdates.isEmpty()) {
                googleSheetsClient.batchUpdateAddressMatches(request.getSheetId(), request.getSheetName(), batchUpdates);
            }
            auditLogService.logGoogleSheetWrite(skipWrite, batchUpdates.size());

            // 7 & 8. Fetch and Normalize Buildium Records
            int buildiumMatches = 0;
            int buildiumDuplicates = 0;

            if (request.isEnrichBuildium()) {
                List<BuildiumAddressRecord> buildiumRecords = buildiumClient.fetchActiveLeaseAddresses();
                summary.setBuildiumRecordsFetched(buildiumRecords.size());
                auditLogService.logBuildiumFetchCompleted(buildiumRecords.size());

                Map<String, List<BuildiumAddressRecord>> buildiumMap = new HashMap<>();
                for (BuildiumAddressRecord record : buildiumRecords) {
                    String normAddress = AddressNormalizer.normalize(record.getRawAddress());
                    String normCity = AddressNormalizer.normalizeCity(record.getCity());
                    String key = AddressNormalizer.buildComparableKey(normAddress, normCity, record.getState(), record.getPostalCode());
                    record.setNormalizedAddress(key);

                    buildiumMap.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
                }

                // 9. Match Buildium -> existing normalized set
                for (Map.Entry<String, List<BuildiumAddressRecord>> entry : buildiumMap.entrySet()) {
                    String key = entry.getKey();
                    List<BuildiumAddressRecord> bMatches = entry.getValue();

                    if (bMatches.size() > 1) {
                        summary.addWarning("Duplicate Buildium records found for key: " + key);
                        buildiumDuplicates++;
                        continue;
                    }

                    if (sfMap.containsKey(key)) {
                        List<SalesforceAddressRecord> sfMatches = sfMap.get(key);
                        if (sfMatches.size() > 1) {
                            // Warning already added during SF -> Sheet match, but skip ambiguity
                            continue;
                        }
                        buildiumMatches++;
                        matchedSfKeys.add(key); // Marked as matched
                    }
                }
                summary.setBuildiumMatches(buildiumMatches);
                auditLogService.logBuildiumMatchComplete(buildiumMatches, buildiumDuplicates);
            }

            int unmatched = sfMap.size() - matchedSfKeys.size();
            summary.setUnmatchedCount(unmatched);

            long duration = System.currentTimeMillis() - startTime;
            auditLogService.logWorkflowCompleted(duration, unmatched);

        } catch (Exception e) {
            summary.setStatus("FAILED");
            summary.addWarning("Workflow failed: " + e.getMessage());
            auditLogService.logWorkflowFailed("Exception during address sync", e);
        }

        return summary;
    }
}
