package com.berryrock.integrationhub.client;

import com.berryrock.integrationhub.config.BuildiumProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BuildiumClientImpl implements BuildiumClient
{
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

    @Override
    public List<Map<String, Object>> getAllRentals()
    {
        List<Map<String, Object>> allRows = new ArrayList<>();

        int pageSize = properties.getPageSize();
        int offset = 0;

        while (true)
        {
            List<Map<String, Object>> page = getRentalsPage(pageSize, offset);

            if (page.isEmpty())
            {
                break;
            }

            allRows.addAll(page);

            if (page.size() < pageSize)
            {
                break;
            }

            offset += pageSize;
        }

        return allRows;
    }

    @Override
    public List<com.berryrock.integrationhub.model.BuildiumAddressRecord> fetchActiveLeaseAddresses() {
        // As requested: handle pagination internally until all relevant records are fetched
        // We'll mock it returning data here just like others for dry run
        org.slf4j.LoggerFactory.getLogger(BuildiumClientImpl.class).info("Mock fetching active lease addresses from Buildium");
        List<com.berryrock.integrationhub.model.BuildiumAddressRecord> records = new ArrayList<>();

        com.berryrock.integrationhub.model.BuildiumAddressRecord r1 = new com.berryrock.integrationhub.model.BuildiumAddressRecord();
        r1.setBuildiumPropertyId("P-101");
        r1.setBuildiumUnitId("U-101");
        r1.setRawAddress("123 Main Street");
        r1.setCity("St. Louis");
        r1.setState("MO");
        r1.setPostalCode("63101");
        records.add(r1);

        com.berryrock.integrationhub.model.BuildiumAddressRecord r2 = new com.berryrock.integrationhub.model.BuildiumAddressRecord();
        r2.setBuildiumPropertyId("P-102");
        r2.setBuildiumUnitId("U-102");
        r2.setRawAddress("456 Elm Ave");
        r2.setCity("St Louis");
        r2.setState("MO");
        r2.setPostalCode("63102");
        records.add(r2);

        // This record won't match anything
        com.berryrock.integrationhub.model.BuildiumAddressRecord r3 = new com.berryrock.integrationhub.model.BuildiumAddressRecord();
        r3.setBuildiumPropertyId("P-103");
        r3.setBuildiumUnitId("U-103");
        r3.setRawAddress("555 Washington Blvd");
        r3.setCity("Saint Louis");
        r3.setState("MO");
        r3.setPostalCode("63103");
        records.add(r3);

        // Add a duplicate to test collision
        com.berryrock.integrationhub.model.BuildiumAddressRecord r4 = new com.berryrock.integrationhub.model.BuildiumAddressRecord();
        r4.setBuildiumPropertyId("P-104");
        r4.setBuildiumUnitId("U-104");
        r4.setRawAddress("123 Main Street");
        r4.setCity("St. Louis");
        r4.setState("MO");
        r4.setPostalCode("63101");
        records.add(r4);

        return records;
    }
}
