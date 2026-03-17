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
}
