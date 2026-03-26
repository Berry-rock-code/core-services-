package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.service.IntegrationHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller that exposes connectivity health checks for all integration vendors.
 *
 * Part of the controller layer — provides a single endpoint that probes each integrated
 * external system (currently Buildium) and reports their reachability status. This is
 * distinct from the Spring Actuator {@code /actuator/health} endpoint, which reports
 * internal application health; this endpoint specifically tests outbound vendor connections.
 *
 * Useful for verifying that credentials and network connectivity are correct after
 * deployment or configuration changes without running a full pipeline.
 *
 * Base path: {@code /api/v1/integrations}
 */
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationHealthController
{
    private final IntegrationHealthService integrationHealthService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param integrationHealthService service that aggregates per-vendor health checks
     */
    public IntegrationHealthController(IntegrationHealthService integrationHealthService)
    {
        this.integrationHealthService = integrationHealthService;
    }

    /**
     * Probes all configured integration endpoints and returns their status.
     *
     * The response is a map where each key is a vendor name (e.g., {@code "buildium"})
     * and the value is {@code true} if that vendor responded successfully, or {@code false}
     * if the ping failed or the integration is disabled.
     *
     * @return {@code 200 OK} with a map of vendor name to boolean reachability status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health()
    {
        return ResponseEntity.ok(integrationHealthService.checkAll());
    }
}
