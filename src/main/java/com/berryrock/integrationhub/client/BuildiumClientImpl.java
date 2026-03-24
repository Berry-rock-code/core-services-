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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

            return raw == null ? Collections.emptyList() : (List<Map<String, Object>>) (List<?>) raw;
        }
        catch (Exception e)
        {
            log.warn("Failed to fetch rentals page: {}", e.getMessage());
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
            if (page.isEmpty()) break;
            all.addAll(page);
            if (page.size() < PAGE_SIZE) break;
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

        log.info("Fetching rental units from Buildium");
        List<Map<String, Object>> allUnits = fetchAllUnits();
        log.info("Fetched {} total units from Buildium", allUnits.size());

        log.info("Fetching all active leases from Buildium");
        Map<String, String> leaseIdByUnitId = fetchActiveLeaseIdsByUnitId();
        log.info("Fetched {} active leases from Buildium", leaseIdByUnitId.size());

        List<BuildiumAddressRecord> records = new ArrayList<>();

        for (Map<String, Object> unit : allUnits)
        {
            Object addressObj = unit.get("Address");
            if (!(addressObj instanceof Map<?, ?> rawAddress))
            {
                continue;
            }

            Map<String, Object> address = (Map<String, Object>) rawAddress;

            String addressLine1 = asString(address.get("AddressLine1"));
            if (addressLine1 == null || addressLine1.isBlank())
            {
                continue;
            }

            String unitId = asString(unit.get("Id"));
            String propertyId = asString(unit.get("PropertyId"));

            BuildiumAddressRecord record = new BuildiumAddressRecord();
            record.setBuildiumPropertyId(propertyId);
            record.setBuildiumUnitId(unitId);
            record.setRawAddress(addressLine1);
            record.setCity(asString(address.get("City")));
            record.setState(asString(address.get("State")));
            record.setPostalCode(asString(address.get("PostalCode")));
            record.setBuildiumLeaseId(leaseIdByUnitId.get(unitId));

            records.add(record);
        }

        log.info("Built {} BuildiumAddressRecords with address data", records.size());
        return records;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBestAddress(Map<String, Object> rental)
    {
        Object directAddressObj = rental.get("Address");
        if (directAddressObj instanceof Map<?, ?> directAddressRaw)
        {
            return (Map<String, Object>) directAddressRaw;
        }

        Object currentTenantsObj = rental.get("CurrentTenants");
        if (currentTenantsObj instanceof List<?> currentTenants)
        {
            for (Object tenantObj : currentTenants)
            {
                if (!(tenantObj instanceof Map<?, ?> tenantMapRaw))
                {
                    continue;
                }

                Map<String, Object> tenantMap = (Map<String, Object>) tenantMapRaw;

                Object addressObj = tenantMap.get("Address");
                if (addressObj instanceof Map<?, ?> addressMapRaw)
                {
                    return (Map<String, Object>) addressMapRaw;
                }

                Object mailingAddressObj = tenantMap.get("MailingAddress");
                if (mailingAddressObj instanceof Map<?, ?> mailingMapRaw)
                {
                    return (Map<String, Object>) mailingMapRaw;
                }

                Object tenantAddressObj = tenantMap.get("TenantAddress");
                if (tenantAddressObj instanceof Map<?, ?> tenantAddressMapRaw)
                {
                    return (Map<String, Object>) tenantAddressMapRaw;
                }
            }
        }

        return null;
    }

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

    @Override
    @SuppressWarnings("unchecked")
    public List<BuildiumAddressRecord> fetchActiveLeaseAddresses()
    {
        if (!properties.isEnabled())
        {
            return new ArrayList<>();
        }

        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BuildiumClientImpl.class);
        log.info("Fetching active lease addresses from Buildium using pagination");

        List<Map<String, Object>> allRentals = getAllRentals();
        List<BuildiumAddressRecord> records = new ArrayList<>();

        if (!allRentals.isEmpty())
        {
            Map<String, Object> sample = allRentals.get(0);
            log.info("Sample Buildium rental keys: {}", sample.keySet());
            log.info("Sample Buildium rental payload: {}", sample);
        }

        for (Map<String, Object> rental : allRentals)
        {
            BuildiumAddressRecord record = new BuildiumAddressRecord();

            record.setBuildiumPropertyId(asString(rental.get("Id")));
            record.setBuildiumUnitId(asString(rental.get("UnitId")));

            Map<String, Object> addressMap = extractBestAddress(rental);

            if (addressMap != null)
            {
                record.setRawAddress(firstNonBlank(
                        asString(addressMap.get("AddressLine1")),
                        asString(addressMap.get("Street")),
                        asString(addressMap.get("Address1")),
                        asString(addressMap.get("Line1"))
                ));

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
            }

            List<Map<String, Object>> leases = (List<Map<String, Object>>) (List<?>) raw;
            return asString(leases.get(0).get("Id"));
        }
        catch (Exception e)
        {
            log.warn("Failed to fetch active lease for unitId={}: {}", unitId, e.getMessage());
            return null;
        }
    }

    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}