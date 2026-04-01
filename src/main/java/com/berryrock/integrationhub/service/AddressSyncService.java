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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API-triggered address synchronization service.
 *
 * Part of the service layer — implements the on-demand sync path exposed via the
 * REST endpoint {@code POST /api/v1/workflows/address-sync/run}. Unlike the startup
 * pipeline, this service supports dry-run mode, optional CSV input in place of the
 * live Google Sheet, and selective Buildium enrichment via the
 * {@link com.berryrock.integrationhub.dto.AddressSyncRequest} flags.
 *
 * Address matching uses a two-pass strategy: a full normalized key match first, followed
 * by an address-line-only fallback for records where city, state, or ZIP differs between
 * systems. Duplicate keys within any data set are surfaced as warnings in the returned
 * {@link com.berryrock.integrationhub.dto.AddressSyncSummary}.
 */
@Service
public class AddressSyncService
{
    private static final Logger log = LoggerFactory.getLogger(AddressSyncService.class);

    private enum AddressQuality { CLEAN, PARTIAL, SUSPICIOUS }

    private final SalesforceClient salesforceClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final BuildiumClient buildiumClient;
    private final AuditLogService auditLogService;
    private final AddressNormalizer addressNormalizer;
    private final AddressMatcher addressMatcher;

    /**
     * Constructs the sync service with all required integration clients and helpers.
     *
     * @param salesforceClient   client for fetching Opportunity records from Salesforce
     * @param googleSheetsClient client for reading and writing the Loan Tape Google Sheet
     * @param buildiumClient     client for fetching active-lease address records from Buildium
     * @param auditLogService    service for logging workflow lifecycle events
     * @param addressNormalizer  utility for normalizing and keying address strings
     * @param addressMatcher     utility for grouping records into normalized-address lookup maps
     */
    public AddressSyncService(SalesforceClient salesforceClient,
                              GoogleSheetsClient googleSheetsClient,
                              BuildiumClient buildiumClient,
                              AuditLogService auditLogService,
                              AddressNormalizer addressNormalizer,
                              AddressMatcher addressMatcher)
    {
        this.salesforceClient = salesforceClient;
        this.googleSheetsClient = googleSheetsClient;
        this.buildiumClient = buildiumClient;
        this.auditLogService = auditLogService;
        this.addressNormalizer = addressNormalizer;
        this.addressMatcher = addressMatcher;
    }

    /**
     * Executes the full sync workflow for the given request configuration.
     *
     * Fetches records from Salesforce, Google Sheets (or a local CSV), and optionally
     * Buildium; normalizes addresses; classifies Salesforce record quality; builds lookup
     * maps; performs two-pass matching; and optionally writes results back to Google Sheets.
     *
     * @param request the sync configuration, including dry-run flag, sheet overrides, and
     *                optional CSV path
     * @return a summary containing record counts and any warnings generated during the run
     */
    public AddressSyncSummary runSync(AddressSyncRequest request)
    {
        AddressSyncSummary summary = new AddressSyncSummary();
        summary.setStatus("SUCCESS");

        if (request.isDryRun())
        {
            summary.getWarnings().add("Dry run enabled. No external systems were modified.");
        }

        // 1. Fetch
        List<SalesforceAddressRecord> sfRecords = salesforceClient.fetchAddressesForGoogleSheetBuildiumSync();
        List<GoogleSheetAddressRow> gsRows;

        if (request.getCsvPath() != null && !request.getCsvPath().isEmpty())
        {
            log.info("Loading local CSV test data from: {}", request.getCsvPath());
            gsRows = readLocalCsv(request.getCsvPath(), summary);
        }
        else
        {
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

        // 3. Classify Salesforce records and retain only CLEAN ones for matching
        List<SalesforceAddressRecord> cleanSfRecords = new ArrayList<>();
        int partialCount = 0;
        int suspiciousCount = 0;

        for (SalesforceAddressRecord record : sfRecords)
        {
            AddressQuality quality = classifySalesforceAddress(record);
            if (quality == AddressQuality.CLEAN)
            {
                cleanSfRecords.add(record);
            }
            else if (quality == AddressQuality.PARTIAL)
            {
                partialCount++;
            }
            else
            {
                suspiciousCount++;
            }
        }

        if (partialCount > 0)
        {
            summary.getWarnings().add(partialCount + " Salesforce record(s) skipped due to PARTIAL address quality.");
        }
        if (suspiciousCount > 0)
        {
            summary.getWarnings().add(suspiciousCount + " Salesforce record(s) skipped due to SUSPICIOUS address quality.");
        }

        // 4. Group / Index
        Map<String, List<SalesforceAddressRecord>> sfMap =
                addressMatcher.groupRecordsByNormalizedAddress(cleanSfRecords, SalesforceAddressRecord::getNormalizedAddress);
        Map<String, List<GoogleSheetAddressRow>> gsMap =
                addressMatcher.groupRecordsByNormalizedAddress(gsRows, GoogleSheetAddressRow::getNormalizedAddress);
        Map<String, List<BuildiumAddressRecord>> bdMap =
                addressMatcher.groupRecordsByNormalizedAddress(bdRecords, BuildiumAddressRecord::getNormalizedAddress);

        // Address-line-only index for fallback matching
        Map<String, List<GoogleSheetAddressRow>> gsAddressOnlyMap =
                addressMatcher.groupRecordsByNormalizedAddress(gsRows, r -> addressNormalizer.normalize(r.getAddress()));

        // Check for duplicates
        checkForDuplicates(sfMap, "Salesforce records", summary);
        checkForDuplicates(gsMap, "Google Sheet rows", summary);
        checkForDuplicates(bdMap, "Buildium records", summary);

        // 5. Match (two-pass: full key first, then address-line-only fallback)
        int sfToGsMatches = 0;
        int gsToBdMatches = 0;
        int unmatchedCount = 0;
        int addressOnlyMatches = 0;

        List<SheetBatchUpdateRequest> updates = new ArrayList<>();

        for (Map.Entry<String, List<SalesforceAddressRecord>> entry : sfMap.entrySet())
        {
            String fullKey = entry.getKey();
            SalesforceAddressRecord sf = entry.getValue().get(0);
            String addrOnlyKey = addressNormalizer.normalize(sf.getAddressLine());

            if (gsMap.containsKey(fullKey))
            {
                // Pass 1: full normalized key match (SYNCED)
                GoogleSheetAddressRow gs = gsMap.get(fullKey).get(0);
                sfToGsMatches++;

                String buildiumId = null;
                if (bdMap.containsKey(fullKey))
                {
                    buildiumId = bdMap.get(fullKey).get(0).getBuildiumPropertyId();
                }

                if (request.isSyncGoogleSheet() && !request.isDryRun())
                {
                    SheetBatchUpdateRequest req = new SheetBatchUpdateRequest();
                    req.setRowNumber(gs.getRowNumber());
                    req.setSalesforceId(sf.getOpportunityId());
                    req.setBuildiumId(buildiumId);
                    updates.add(req);
                }
            }
            else if (addrOnlyKey != null && gsAddressOnlyMap.containsKey(addrOnlyKey))
            {
                // Pass 2: address-line-only fallback match (SYNCED_ADDRESS_ONLY)
                GoogleSheetAddressRow gs = gsAddressOnlyMap.get(addrOnlyKey).get(0);
                sfToGsMatches++;
                addressOnlyMatches++;
                log.debug("Address-only match (SYNCED_ADDRESS_ONLY) for SF opportunity {}: {}", sf.getOpportunityId(), addrOnlyKey);

                String buildiumId = null;
                if (bdMap.containsKey(fullKey))
                {
                    buildiumId = bdMap.get(fullKey).get(0).getBuildiumPropertyId();
                }

                if (request.isSyncGoogleSheet() && !request.isDryRun()) {
                    SheetBatchUpdateRequest req = new SheetBatchUpdateRequest();
                    req.setRowNumber(gs.getRowNumber());
                    req.setSalesforceId(sf.getOpportunityId());
                    req.setBuildiumId(buildiumId);
                    updates.add(req);
                }
            }
            else
            {
                unmatchedCount++;
            }
        }

        if (addressOnlyMatches > 0)
        {
            summary.getWarnings().add(addressOnlyMatches + " record(s) matched on address line only (SYNCED_ADDRESS_ONLY).");
        }

        // Count Sheets to Buildium matches for the summary
        for (String key : gsMap.keySet())
        {
            if (bdMap.containsKey(key))
            {
                gsToBdMatches++;
            }
        }

        summary.setGoogleSheetMatches(sfToGsMatches);
        summary.setBuildiumMatches(gsToBdMatches);
        summary.setUnmatchedCount(unmatchedCount);

        // 6. Write back
        if (!updates.isEmpty() && (request.getCsvPath() == null || request.getCsvPath().isEmpty()))
        {
            googleSheetsClient.batchUpdateAddressMatches(request.getSheetId(), request.getSheetName(), updates);
        }

        return summary;
    }

    private AddressQuality classifySalesforceAddress(SalesforceAddressRecord record)
    {
        if (StringUtils.isBlank(record.getAddressLine()))
        {
            return AddressQuality.SUSPICIOUS;
        }
        if (StringUtils.isBlank(record.getCity())
                || StringUtils.isBlank(record.getState())
                || StringUtils.isBlank(record.getPostalCode()))
        {
            return AddressQuality.PARTIAL;
        }
        return AddressQuality.CLEAN;
    }

    private List<GoogleSheetAddressRow> readLocalCsv(String csvPath, AddressSyncSummary summary)
    {
        List<GoogleSheetAddressRow> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.trim().isEmpty())
                {
                    continue;
                }

                String[] parts = line.split(",", 2);
                if (parts.length == 2)
                {
                    try
                    {
                        int rowNum = Integer.parseInt(parts[0].trim());
                        String addr = parts[1].trim();
                        if (addr.startsWith("\"") && addr.endsWith("\""))
                        {
                            addr = addr.substring(1, addr.length() - 1);
                        }

                        GoogleSheetAddressRow row = new GoogleSheetAddressRow();
                        row.setRowNumber(rowNum);
                        row.setAddress(addr);
                        rows.add(row);
                    }
                    catch (NumberFormatException e)
                    {
                        // Skip header or malformed row
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Failed to read CSV at path: {}", csvPath, e);
            summary.getWarnings().add("Failed to load local CSV: " + e.getMessage());
        }
        return rows;
    }

    private String createFullNormalizedAddress(String address, String city, String state, String zip)
    {
        return addressNormalizer.buildNormalizedKey(address, city, state, zip);
    }

    private <T> void checkForDuplicates(Map<String, List<T>> map, String source, AddressSyncSummary summary)
    {
        for (Map.Entry<String, List<T>> entry : map.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                summary.getWarnings().add(
                        String.format("Duplicate %s found for normalized address: %s", source, entry.getKey())
                );
            }
        }
    }

    /**
     * Delegates to {@link #runSync(AddressSyncRequest)}.
     *
     * Provided as a convenience alias for callers that prefer the shorter method name.
     *
     * @param request the sync configuration
     * @return the sync result summary
     */
    public AddressSyncSummary run(AddressSyncRequest request)
    {
        return runSync(request);
    }
}
