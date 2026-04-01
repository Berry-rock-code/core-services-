package com.berryrock.integrationhub.service;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import com.berryrock.integrationhub.config.AddressPipelineProperties;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import com.berryrock.integrationhub.model.SheetBatchUpdateRequest;
import com.berryrock.integrationhub.util.AddressNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressPipelineService
{
    private static final Logger log = LoggerFactory.getLogger(AddressPipelineService.class);

    private final SalesforceClient salesforceClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final BuildiumClient buildiumClient;
    private final AddressNormalizer addressNormalizer;
    private final AddressQualityService qualityService;
    private final AddressPipelineProperties properties;

    public AddressPipelineService(
            SalesforceClient salesforceClient,
            GoogleSheetsClient googleSheetsClient,
            BuildiumClient buildiumClient,
            AddressNormalizer addressNormalizer,
            AddressQualityService qualityService,
            AddressPipelineProperties properties
    )
    {
        this.salesforceClient = salesforceClient;
        this.googleSheetsClient = googleSheetsClient;
        this.buildiumClient = buildiumClient;
        this.addressNormalizer = addressNormalizer;
        this.qualityService = qualityService;
        this.properties = properties;
    }

    public void runPipeline()
    {
        if (!properties.isEnabled())
        {
            log.info("Address Pipeline is disabled.");
            return;
        }

        log.info("=== Starting Address Pipeline ===");

        List<SalesforceAddressRecord> sfRecords = salesforceClient.fetchAddressesForGoogleSheetBuildiumSync();

        int cleanCount = 0;
        int partialCount = 0;
        int suspiciousCount = 0;

        Map<String, SalesforceAddressRecord> cleanSfByNormalized = new HashMap<>();
        Map<String, SalesforceAddressRecord> cleanSfByAddressOnly = new HashMap<>();
        Map<String, SalesforceAddressRecord> partialSfByAddressOnly = new HashMap<>();

        for (SalesforceAddressRecord record : sfRecords)
        {
            if (record.getOpportunityId() == null)
            {
                continue;
            }

            AddressQualityService.Quality quality = qualityService.classify(record);
            record.setQuality(quality.name());

            switch (quality)
            {
                case CLEAN:
                    cleanCount++;

                    record.setNormalizedAddress(
                            addressNormalizer.buildNormalizedKey(
                                    record.getAddressLine(),
                                    record.getCity(),
                                    record.getState(),
                                    record.getPostalCode()
                            )
                    );

                    String sfAddressOnlyKey = buildAddressOnlyKey(record.getAddressLine());

                    if (record.getNormalizedAddress() != null && !record.getNormalizedAddress().isBlank())
                    {
                        cleanSfByNormalized.put(record.getNormalizedAddress(), record);
                    }

                    if (sfAddressOnlyKey != null && !sfAddressOnlyKey.isBlank())
                    {
                        cleanSfByAddressOnly.put(sfAddressOnlyKey, record);
                    }
                    break;

                case PARTIAL:
                    partialCount++;

                    String partialAddressOnlyKey = buildAddressOnlyKey(record.getAddressLine());

                    if (partialAddressOnlyKey != null && !partialAddressOnlyKey.isBlank())
                    {
                        partialSfByAddressOnly.put(partialAddressOnlyKey, record);
                    }
                    break;

                case SUSPICIOUS:
                    suspiciousCount++;
                    break;

                default:
                    break;
            }
        }

        log.info("[Summary] Salesforce Records Fetched: {}", sfRecords.size());
        log.info("[Summary] CLEAN: {}, PARTIAL: {}, SUSPICIOUS: {}", cleanCount, partialCount, suspiciousCount);

        List<GoogleSheetAddressRow> sheetRows =
                googleSheetsClient.fetchAddressRows(properties.getSheetId(), properties.getSheetName());

        List<BuildiumAddressRecord> buildiumRecords = buildiumClient.fetchActiveLeaseAddresses();
        Map<String, List<BuildiumAddressRecord>> buildiumByNormKey = new HashMap<>();
        Map<String, List<BuildiumAddressRecord>> buildiumByAddressOnly = new HashMap<>();
        int missingTenantAddress = 0;

        for (BuildiumAddressRecord br : buildiumRecords)
        {
            if (br.getRawAddress() == null || br.getRawAddress().trim().isEmpty())
            {
                missingTenantAddress++;
                continue;
            }

            br.setNormalizedAddress(
                    addressNormalizer.buildNormalizedKey(
                            br.getRawAddress(),
                            br.getCity(),
                            br.getState(),
                            br.getPostalCode()
                    )
            );

            String buildiumAddressOnlyKey = buildAddressOnlyKey(br.getRawAddress());

            if (br.getNormalizedAddress() != null && !br.getNormalizedAddress().isBlank())
            {
                buildiumByNormKey.computeIfAbsent(br.getNormalizedAddress(), k -> new ArrayList<>()).add(br);
            }

            if (buildiumAddressOnlyKey != null && !buildiumAddressOnlyKey.isBlank())
            {
                buildiumByAddressOnly.computeIfAbsent(buildiumAddressOnlyKey, k -> new ArrayList<>()).add(br);
            }
        }

        log.info("[Summary] Buildium Active Leases: {}", buildiumRecords.size());
        log.info("[Summary] Buildium Missing Tenant Address: {}", missingTenantAddress);
        log.info("[Summary] Unique Indexed Buildium Keys: {}", buildiumByNormKey.size());

        List<SheetBatchUpdateRequest> updates = new ArrayList<>();

        int ltMatchedToSf = 0;
        int ltMatchedToSfPartial = 0;
        int ltNoSfMatch = 0;
        int ltMatchedToBuildium = 0;
        int ltNoBuildiumMatch = 0;
        int ltAmbiguousBuildiumMatch = 0;
        int ltSkipped = 0;

        for (GoogleSheetAddressRow row : sheetRows)
        {
            ParsedSheetAddress parsed = parseSheetRowAddress(row);

            String rowNormalized = addressNormalizer.buildNormalizedKey(
                    parsed.addressLine,
                    parsed.city,
                    parsed.state,
                    parsed.postalCode
            );

            String rowAddressOnlyKey = buildAddressOnlyKey(parsed.addressLine);

            if ((rowNormalized == null || rowNormalized.isBlank())
                    && (rowAddressOnlyKey == null || rowAddressOnlyKey.isBlank()))
            {
                ltSkipped++;
                continue;
            }

            SalesforceAddressRecord sfRecord = null;
            boolean matchedByAddressOnly = false;

            if (rowNormalized != null && !rowNormalized.isBlank())
            {
                sfRecord = cleanSfByNormalized.get(rowNormalized);
            }

            if (sfRecord == null && rowAddressOnlyKey != null && !rowAddressOnlyKey.isBlank())
            {
                sfRecord = cleanSfByAddressOnly.get(rowAddressOnlyKey);
                matchedByAddressOnly = sfRecord != null;
            }

            if (sfRecord == null && rowAddressOnlyKey != null && !rowAddressOnlyKey.isBlank())
            {
                sfRecord = partialSfByAddressOnly.get(rowAddressOnlyKey);
                if (sfRecord != null)
                {
                    matchedByAddressOnly = true;
                    ltMatchedToSfPartial++;
                }
            }

            if (sfRecord == null)
            {
                if (ltNoSfMatch < 100)
                {
                    String partialCandidate = "none";
                    if (rowAddressOnlyKey != null && !rowAddressOnlyKey.isBlank())
                    {
                        SalesforceAddressRecord partialMatch = partialSfByAddressOnly.get(rowAddressOnlyKey);
                        if (partialMatch != null)
                        {
                            partialCandidate = partialMatch.getOpportunityId() + " (PARTIAL)";
                        }
                    }

                    log.info(
                            "[Debug] No SF match for row {} -> rawAddress='{}', parsedAddress='{}', parsedCity='{}', parsedState='{}', parsedZip='{}', fullKey='{}', addressOnlyKey='{}', partialCandidate='{}'",
                            row.getRowNumber(),
                            row.getAddress(),
                            parsed.addressLine,
                            parsed.city,
                            parsed.state,
                            parsed.postalCode,
                            rowNormalized,
                            rowAddressOnlyKey,
                            partialCandidate
                    );
                }

                ltNoSfMatch++;
                continue;
            }

            ltMatchedToSf++;

            SheetBatchUpdateRequest update = new SheetBatchUpdateRequest();
            update.setRowNumber(row.getRowNumber());
            update.setSalesforceId(sfRecord.getOpportunityId());
            update.setSfStandardizedAddress(sfRecord.getRawAddress());
            update.setSfAddressQuality(sfRecord.getQuality());
            String syncStatus;
            if ("PARTIAL".equals(sfRecord.getQuality()))
            {
                syncStatus = "SYNCED_PARTIAL";
            }
            else if (matchedByAddressOnly)
            {
                syncStatus = "SYNCED_ADDRESS_ONLY";
            }
            else
            {
                syncStatus = "SYNCED";
            }
            update.setSfAddressSyncStatus(syncStatus);

            List<BuildiumAddressRecord> candidates = null;

            if (rowNormalized != null && !rowNormalized.isBlank())
            {
                candidates = buildiumByNormKey.get(rowNormalized);
            }

            if ((candidates == null || candidates.isEmpty())
                    && rowAddressOnlyKey != null && !rowAddressOnlyKey.isBlank())
            {
                candidates = buildiumByAddressOnly.get(rowAddressOnlyKey);
            }

            if (candidates == null || candidates.isEmpty())
            {
                ltNoBuildiumMatch++;
            }
            else if (candidates.size() == 1)
            {
                ltMatchedToBuildium++;
                BuildiumAddressRecord matchedBr = candidates.get(0);

                update.setBuildiumId(matchedBr.getBuildiumPropertyId());
                update.setBuildiumPropertyId(matchedBr.getBuildiumPropertyId());
                update.setBuildiumLeaseId(matchedBr.getBuildiumUnitId());
            }
            else
            {
                ltAmbiguousBuildiumMatch++;
                update.setSfAddressSyncStatus("AMBIGUOUS_BUILDIUM_MATCH");
            }

            updates.add(update);
        }

        log.info("[Summary] CLEAN rows matched to SF: {}", ltMatchedToSf);
        log.info("[Summary] PARTIAL rows matched to SF (Pass 3): {}", ltMatchedToSfPartial);
        log.info("[Summary] Rows with no SF match: {}", ltNoSfMatch);
        log.info("[Summary] LT Matched to Buildium: {}", ltMatchedToBuildium);
        log.info("[Summary] LT No Buildium Match: {}", ltNoBuildiumMatch);
        log.info("[Summary] LT Ambiguous Buildium Match: {}", ltAmbiguousBuildiumMatch);
        log.info("[Summary] LT Skipped rows: {}", ltSkipped);
        log.info("[Summary] Pending Updates Count: {}", updates.size());

        if (properties.isDryRun())
        {
            log.info("[Summary] DRY RUN - No updates committed to Google Sheets.");

            for (int i = 0; i < Math.min(updates.size(), 10); i++)
            {
                SheetBatchUpdateRequest u = updates.get(i);
                log.info(
                        "Dry-run Sample {}: Row {}, SF Id: {}, BR Property Id: {}, BR Lease Id: {}, Standardized Address: {}, Quality: {}, Sync Status: {}",
                        i + 1,
                        u.getRowNumber(),
                        u.getSalesforceId(),
                        u.getBuildiumPropertyId(),
                        u.getBuildiumLeaseId(),
                        u.getSfStandardizedAddress(),
                        u.getSfAddressQuality(),
                        u.getSfAddressSyncStatus()
                );
            }
        }
        else
        {
            if (!updates.isEmpty())
            {
                googleSheetsClient.batchUpdateAddressMatches(
                        properties.getSheetId(),
                        properties.getSheetName(),
                        updates
                );
                log.info("[Summary] Updates committed to Google Sheets.");
            }
            else
            {
                log.info("[Summary] No updates to commit.");
            }
        }

        log.info("=== Finished Address Pipeline ===");
    }

    private String buildAddressOnlyKey(String addressLine)
    {
        if (addressLine == null || addressLine.trim().isEmpty())
        {
            return null;
        }

        return addressNormalizer.normalize(addressLine);
    }

    private ParsedSheetAddress parseSheetRowAddress(GoogleSheetAddressRow row)
    {
        String rawAddress = safe(row.getAddress());
        String city = safe(row.getCity());
        String state = safe(row.getState());
        String postalCode = safe(row.getPostalCode());

        if (!rawAddress.isBlank())
        {
            ParsedAddressParts parts = null;

            if (!city.isBlank())
            {
                parts = splitCombinedAddressUsingKnownCity(rawAddress, city);
            }

            if (parts == null)
            {
                parts = splitCombinedAddressFallback(rawAddress);
            }

            if (parts != null)
            {
                if (!parts.addressLine.isBlank())
                {
                    rawAddress = parts.addressLine;
                }

                if (city.isBlank() && !parts.city.isBlank())
                {
                    city = parts.city;
                }

                if (state.isBlank() && !parts.state.isBlank())
                {
                    state = parts.state;
                }

                if (postalCode.isBlank() && !parts.postalCode.isBlank())
                {
                    postalCode = parts.postalCode;
                }
            }
        }

        return new ParsedSheetAddress(
                blankToNull(rawAddress),
                blankToNull(city),
                blankToNull(state),
                blankToNull(postalCode)
        );
    }

    private ParsedAddressParts splitCombinedAddressUsingKnownCity(String raw, String knownCity)
    {
        String cleaned = safe(raw).replaceAll("\\s+", " ").trim();
        String city = safe(knownCity);

        if (cleaned.isBlank() || city.isBlank())
        {
            return null;
        }

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "^(.*?)(?:,?\\s+)"
                        + java.util.regex.Pattern.quote(city)
                        + "(?:,?\\s+)([A-Za-z]{2})\\s+(\\d{5})(?:-\\d{4})?$",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher m = p.matcher(cleaned);

        if (m.matches())
        {
            return new ParsedAddressParts(
                    m.group(1).trim(),
                    city,
                    m.group(2).trim(),
                    m.group(3).trim()
            );
        }

        return null;
    }

    private ParsedAddressParts splitCombinedAddressFallback(String raw)
    {
        String cleaned = safe(raw).replaceAll("\\s+", " ").trim();

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "^(.*?)(?:,\\s*|\\s+)([A-Za-z][A-Za-z .'-]+?)(?:,\\s*|\\s+)([A-Za-z]{2})\\s+(\\d{5})(?:-\\d{4})?$"
        );

        java.util.regex.Matcher m = p.matcher(cleaned);

        if (m.matches())
        {
            return new ParsedAddressParts(
                    m.group(1).trim(),
                    m.group(2).trim(),
                    m.group(3).trim(),
                    m.group(4).trim()
            );
        }

        return new ParsedAddressParts(cleaned, "", "", "");
    }

    private String safe(String value)
    {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static class ParsedSheetAddress
    {
        private final String addressLine;
        private final String city;
        private final String state;
        private final String postalCode;

        private ParsedSheetAddress(String addressLine, String city, String state, String postalCode)
        {
            this.addressLine = addressLine;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
        }
    }

    private static class ParsedAddressParts
    {
        private final String addressLine;
        private final String city;
        private final String state;
        private final String postalCode;

        private ParsedAddressParts(String addressLine, String city, String state, String postalCode)
        {
            this.addressLine = addressLine;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
        }
    }
}