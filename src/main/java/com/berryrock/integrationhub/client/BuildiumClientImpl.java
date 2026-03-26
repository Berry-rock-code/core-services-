package com.berryrock.integrationhub.client;

import com.berryrock.integrationhub.config.BuildiumProperties;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BuildiumClientImpl implements BuildiumClient
{
    private static final Logger log = LoggerFactory.getLogger(BuildiumClientImpl.class);

    private static final int PAGE_SIZE = 1000;

    private final WebClient buildiumWebClient;
    private final BuildiumProperties properties;

    public BuildiumClientImpl(WebClient buildiumWebClient, BuildiumProperties properties)
    {
        this.buildiumWebClient = buildiumWebClient;
        this.properties = properties;
    }

    @Override
    public boolean ping()
    {
        if (!properties.isEnabled())
        {
            return false;
        }

        try
        {
            List<Map<String, Object>> page = getRentalsPage(1, 0);
            return page != null;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRentalsPage(int limit, int offset)
    {
        try
        {
            List<?> raw = buildiumWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/rentals")
                            .queryParam("limit", limit)
                            .queryParam("offset", offset)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (raw == null)
            {
                return Collections.emptyList();
            }

            return (List<Map<String, Object>>) (List<?>) raw;
        }
        catch (Exception e)
        {
            log.warn("Failed to fetch rentals page from Buildium. {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAllRentals()
    {
        List<Map<String, Object>> all = new ArrayList<>();
        int offset = 0;

        while (true)
        {
            List<Map<String, Object>> page = getRentalsPage(PAGE_SIZE, offset);
            if (page.isEmpty())
            {
                break;
            }
            all.addAll(page);
            if (page.size() < PAGE_SIZE)
            {
                break;
            }
            offset += PAGE_SIZE;
        }

        return all;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BuildiumAddressRecord> fetchActiveLeaseAddresses()
    {
        if (!properties.isEnabled())
        {
            return new ArrayList<>();
        }

        log.info("Fetching active lease addresses from Buildium (units + leases join)");

        try
        {
            // Phase 1: Fetch all rental units (these carry the address data)
            List<Map<String, Object>> allUnits = fetchAllPages("/v1/rentals/units", Collections.emptyMap());

            // Phase 2: Fetch all active leases (these carry the lease ID and link to units)
            List<Map<String, Object>> allLeases = fetchAllPages("/v1/leases", Map.of("leasestatuses", "Active"));

            // Phase 3: Build unitId -> leaseId lookup from the lease data
            Map<String, String> unitIdToLeaseId = new HashMap<>();
            for (Map<String, Object> lease : allLeases)
            {
                Object unitObj = lease.get("Unit");
                if (unitObj instanceof Map<?, ?> unitMap)
                {
                    String unitId = asString(((Map<String, Object>) unitMap).get("Id"));
                    String leaseId = asString(lease.get("Id"));
                    if (unitId != null && leaseId != null)
                    {
                        unitIdToLeaseId.put(unitId, leaseId);
                    }
                }
            }

            // Phase 4: Build address records from units, joining lease IDs where available
            List<BuildiumAddressRecord> records = new ArrayList<>();

            for (Map<String, Object> unit : allUnits)
            {
                Object addressObj = unit.get("Address");
                if (!(addressObj instanceof Map<?, ?>))
                {
                    continue;
                }

                Map<String, Object> addressMap = (Map<String, Object>) addressObj;

                String addressLine1 = firstNonBlank(
                        asString(addressMap.get("AddressLine1")),
                        asString(addressMap.get("Street")),
                        asString(addressMap.get("Address1")),
                        asString(addressMap.get("Line1"))
                );

                // Skip units without a street address -- nothing to match on
                if (addressLine1 == null || addressLine1.trim().isEmpty())
                {
                    continue;
                }

                String unitId = asString(unit.get("Id"));

                BuildiumAddressRecord record = new BuildiumAddressRecord();
                record.setBuildiumUnitId(unitId);
                record.setBuildiumPropertyId(asString(unit.get("PropertyId")));
                record.setRawAddress(addressLine1);

                record.setCity(firstNonBlank(
                        asString(addressMap.get("City")),
                        asString(addressMap.get("city"))
                ));

                record.setState(firstNonBlank(
                        asString(addressMap.get("State")),
                        asString(addressMap.get("StateCode")),
                        asString(addressMap.get("state"))
                ));

                record.setPostalCode(firstNonBlank(
                        asString(addressMap.get("PostalCode")),
                        asString(addressMap.get("Zip")),
                        asString(addressMap.get("ZipCode")),
                        asString(addressMap.get("postalCode"))
                ));

                // Join active lease if one exists for this unit
                if (unitId != null)
                {
                    record.setBuildiumLeaseId(unitIdToLeaseId.get(unitId));
                }

                records.add(record);
            }

            log.info("Built {} address records from {} units and {} active leases",
                    records.size(), allUnits.size(), allLeases.size());

            return records;
        }
        catch (Exception e)
        {
            log.warn("Failed to fetch active lease addresses from Buildium: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // -- Internal pagination helper ------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAllPages(String path, Map<String, String> extraParams)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        int offset = 0;

        while (true)
        {
            final int currentOffset = offset;

            List<?> raw = buildiumWebClient
                    .get()
                    .uri(uriBuilder ->
                    {
                        uriBuilder.path(path)
                                .queryParam("limit", PAGE_SIZE)
                                .queryParam("offset", currentOffset);
                        extraParams.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (raw == null || raw.isEmpty())
            {
                break;
            }

            all.addAll((List<Map<String, Object>>) (List<?>) raw);

            if (raw.size() < PAGE_SIZE)
            {
                break;
            }

            offset += PAGE_SIZE;
        }

        return all;
    }

    // -- Utility methods -----------------------------------------------------------

    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values)
    {
        if (values == null)
        {
            return null;
        }

        for (String value : values)
        {
            if (value != null && !value.trim().isEmpty())
            {
                return value;
            }
        }

        return null;
    }
}