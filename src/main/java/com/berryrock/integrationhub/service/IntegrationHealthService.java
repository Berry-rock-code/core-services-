package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.client.BuildiumClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IntegrationHealthService
{
    private final BuildiumClient buildiumClient;

    public IntegrationHealthService(BuildiumClient buildiumClient)
    {
        this.buildiumClient = buildiumClient;
    }

    public Map<String, Object> checkAll()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildium", buildiumClient.ping());
        return result;
    }
}