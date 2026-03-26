package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.dto.ApiResponse;
import com.berryrock.integrationhub.service.SystemIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for application-level system endpoints.
 *
 * Part of the controller layer — exposes a lightweight ping endpoint and a service
 * metadata endpoint. Both endpoints are primarily used during deployment verification
 * and local development to confirm that the service is running and that dependency
 * injection is wired correctly.
 *
 * These endpoints complement the Spring Actuator endpoints ({@code /actuator/health},
 * {@code /actuator/info}) with application-specific readiness signals.
 *
 * Base path: {@code /api/v1}
 */
@RestController
@RequestMapping("/api/v1")
public class SystemController
{
    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final SystemIntegrationService systemIntegrationService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param systemIntegrationService service providing ping and metadata operations
     */
    public SystemController(SystemIntegrationService systemIntegrationService)
    {
        this.systemIntegrationService = systemIntegrationService;
    }

    /**
     * Simple liveness check endpoint.
     *
     * Calls {@link SystemIntegrationService#pingClients()} as a side effect, which
     * exercises the dependency injection graph for all three integration client beans.
     * Returns {@code "pong"} wrapped in an {@link ApiResponse} envelope.
     *
     * @return {@code 200 OK} with {@code "pong"} as the response data
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping()
    {
        log.info("Received request for /api/v1/ping");

        // Exercises DI wiring for all integration client beans as a readiness side-effect
        systemIntegrationService.pingClients();

        return ResponseEntity.ok(ApiResponse.success("pong"));
    }

    /**
     * Returns static metadata about the running service.
     *
     * Provides the service name, description, runtime language, and framework version.
     * Useful for confirming which build is deployed without reading environment variables.
     *
     * @return {@code 200 OK} with a map of service metadata key-value pairs
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> info()
    {
        log.info("Received request for /api/v1/info");
        Map<String, String> infoData = systemIntegrationService.getServiceInfo();
        return ResponseEntity.ok(ApiResponse.success(infoData));
    }
}
