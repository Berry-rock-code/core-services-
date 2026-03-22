package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import com.berryrock.integrationhub.config.AddressPipelineProperties;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import com.berryrock.integrationhub.model.SheetBatchUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AddressPipelineService.class);

    private final SalesforceClient salesforceClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final BuildiumClient buildiumClient;
    private final AddressNormalizationService normalizationService;
    private final AddressQualityService qualityService;
    private final AddressPipelineProperties properties;

    public AddressPipelineService(SalesforceClient salesforceClient,
                                  GoogleSheetsClient googleSheetsClient,
                                  BuildiumClient buildiumClient,
                                  AddressNormalizationService normalizationService,
                                  AddressQualityService qualityService,
                                  AddressPipelineProperties properties) {
        this.salesforceClient = salesforceClient;
        this.googleSheetsClient = googleSheetsClient;
        this.buildiumClient = buildiumClient;
        this.normalizationService = normalizationService;
        this.qualityService = qualityService;
        this.properties = properties;
    }

    public void runPipeline() {
        if (!properties.isEnabled()) {
            log.info("Address Pipeline is disabled.");
            return;
        }

        log.info("=== Starting Address Pipeline ===");

        // Stage 1: Salesforce Extraction and Quality Scoring
        List<SalesforceAddressRecord> sfRecords = salesforceClient.fetchAddressesForGoogleSheetBuildiumSync();
        int cleanCount = 0;
        int partialCount = 0;
        int suspiciousCount = 0;

        Map<String, SalesforceAddressRecord> cleanSfRecords = new HashMap<>();

        for (SalesforceAddressRecord record : sfRecords) {
            if (record.getOpportunityId() == null) continue;

            AddressQualityService.Quality quality = qualityService.classify(record);
            record.setQuality(quality.name());

            switch (quality) {
                case CLEAN:
                    cleanCount++;
                    record.setNormalizedAddress(normalizationService.buildNormalizedKey(
                            record.getAddressLine(), record.getCity(), record.getState(), record.getPostalCode()
                    ));
                    cleanSfRecords.put(record.getOpportunityId(), record);
                    break;
                case PARTIAL:
                    partialCount++;
                    break;
                case SUSPICIOUS:
                    suspiciousCount++;
                    break;
            }
        }

        log.info("[Summary] Salesforce Records Fetched: {}", sfRecords.size());
        log.info("[Summary] CLEAN: {}, PARTIAL: {}, SUSPICIOUS: {}", cleanCount, partialCount, suspiciousCount);

        // Stage 2: Loan Tape Sync
        List<GoogleSheetAddressRow> sheetRows = googleSheetsClient.fetchAddressRows(properties.getSheetId(), properties.getSheetName());

        List<SheetBatchUpdateRequest> updates = new ArrayList<>();
        int syncedToLT = 0;
        int noLTMatch = 0;

        // Stage 3: Buildium Enrichment Prep
        List<BuildiumAddressRecord> buildiumRecords = buildiumClient.fetchActiveLeaseAddresses();
        Map<String, List<BuildiumAddressRecord>> buildiumByNormKey = new HashMap<>();
        int missingTenantAddress = 0;

        for (BuildiumAddressRecord br : buildiumRecords) {
            if (br.getRawAddress() == null) {
                missingTenantAddress++;
                continue;
            }

            br.setNormalizedAddress(normalizationService.buildNormalizedKey(
                    br.getRawAddress(), br.getCity(), br.getState(), br.getPostalCode()
            ));

            buildiumByNormKey.computeIfAbsent(br.getNormalizedAddress(), k -> new ArrayList<>()).add(br);
        }

        log.info("[Summary] Buildium Active Leases: {}", buildiumRecords.size());
        log.info("[Summary] Buildium Missing Tenant Address: {}", missingTenantAddress);
        log.info("[Summary] Unique Indexed Buildium Keys: {}", buildiumByNormKey.size());

        // Stage 4: Process Rows
        int ltMatch = 0;
        int ltNoMatch = 0;
        int ltAmbiguous = 0;
        int ltSkipped = 0;

        for (GoogleSheetAddressRow row : sheetRows) {
            if (row.getSalesforceId() == null || row.getSalesforceId().trim().isEmpty()) {
                ltSkipped++;
                continue; // Cannot match SF
            }

            SalesforceAddressRecord sfRecord = cleanSfRecords.get(row.getSalesforceId());
            if (sfRecord == null) {
                // Not in clean pool, skip
                ltSkipped++;
                continue;
            }

            syncedToLT++;

            SheetBatchUpdateRequest update = new SheetBatchUpdateRequest();
            update.setRowNumber(row.getRowNumber());
            update.setSalesforceId(sfRecord.getOpportunityId()); // Already set but for completeness
            update.setSfStandardizedAddress(sfRecord.getRawAddress());
            update.setSfAddressQuality(sfRecord.getQuality());
            update.setSfAddressSyncStatus("SYNCED");

            // Buildium Matching
            List<BuildiumAddressRecord> candidates = buildiumByNormKey.get(sfRecord.getNormalizedAddress());
            if (candidates == null || candidates.isEmpty()) {
                ltNoMatch++;
            } else if (candidates.size() == 1) {
                ltMatch++;
                BuildiumAddressRecord matchedBr = candidates.get(0);
                update.setBuildiumLeaseId(matchedBr.getBuildiumUnitId());
                update.setBuildiumPropertyId(matchedBr.getBuildiumPropertyId());
            } else {
                ltAmbiguous++;
            }

            updates.add(update);
        }

        noLTMatch = cleanCount - syncedToLT;

        log.info("[Summary] CLEAN rows synced to LT: {}", syncedToLT);
        log.info("[Summary] CLEAN rows with no LT match: {}", noLTMatch);
        log.info("[Summary] LT Matched to Buildium: {}", ltMatch);
        log.info("[Summary] LT No Buildium Match: {}", ltNoMatch);
        log.info("[Summary] LT Ambiguous Buildium Match: {}", ltAmbiguous);
        log.info("[Summary] LT Skipped rows: {}", ltSkipped);
        log.info("[Summary] Pending Updates Count: {}", updates.size());

        // Stage 5: Write Sheet Updates
        if (properties.isDryRun()) {
            log.info("[Summary] DRY RUN - No updates committed to Google Sheets.");
            for (int i = 0; i < Math.min(updates.size(), 10); i++) {
                SheetBatchUpdateRequest u = updates.get(i);
                log.info("Dry-run Sample {}: Row {}, SF Id: {}, BR Property Id: {}, BR Lease Id: {}, Standardized Address: {}, Quality: {}, Sync Status: {}",
                        i+1, u.getRowNumber(), u.getSalesforceId(), u.getBuildiumPropertyId(), u.getBuildiumLeaseId(), u.getSfStandardizedAddress(), u.getSfAddressQuality(), u.getSfAddressSyncStatus());
            }
        } else {
            if (!updates.isEmpty()) {
                googleSheetsClient.batchUpdateAddressMatches(properties.getSheetId(), properties.getSheetName(), updates);
                log.info("[Summary] Updates committed to Google Sheets.");
            } else {
                log.info("[Summary] No updates to commit.");
            }
        }

        log.info("=== Finished Address Pipeline ===");
    }
}
