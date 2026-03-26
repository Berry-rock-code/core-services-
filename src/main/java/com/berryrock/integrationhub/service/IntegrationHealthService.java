package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.client.BuildiumClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates connectivity health checks across all integration vendors.
 *
 * Part of the service layer — called by
 * {@link com.berryrock.integrationhub.controller.IntegrationHealthController} to produce
 * a vendor-by-vendor reachability report. Each entry in the result map corresponds to one
 * external system and indicates whether that system is currently reachable.
 *
 * A {@link LinkedHashMap} is used so that the JSON response always lists vendors in a
 * consistent order, making the output easier to read in log output or monitoring dashboards.
 *
 * Additional vendors (Google Sheets, Salesforce) can be added to {@link #checkAll} as
 * their integration clients gain proper connectivity test methods.
 */
@Service
public class IntegrationHealthService
{
    private final BuildiumClient buildiumClient;

    /**
     * Constructs the service with the required Buildium client dependency.
     *
     * @param buildiumClient client used to perform the Buildium ping
     */
    public IntegrationHealthService(BuildiumClient buildiumClient)
    {
        this.buildiumClient = buildiumClient;
    }

    /**
     * Probes all configured integration endpoints and returns their status.
     *
     * Each entry in the returned map has a vendor name as the key and a boolean
     * reachability flag as the value. A {@code true} value means the vendor responded
     * to a lightweight probe; {@code false} means either the integration is disabled or
     * the probe failed.
     *
     * @return insertion-ordered map of vendor name to reachability boolean
     */
    public Map<String, Object> checkAll()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildium", buildiumClient.ping());
        return result;
    }
}
