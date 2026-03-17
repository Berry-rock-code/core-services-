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

@RestController
@RequestMapping("/api/v1")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final SystemIntegrationService systemIntegrationService;

    public SystemController(SystemIntegrationService systemIntegrationService) {
        this.systemIntegrationService = systemIntegrationService;
    }

    /**
     * Endpoint for a simple application response.
     * Often used to confirm application-level readiness beyond just Actuator health.
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        log.info("Received request for /api/v1/ping");
        
        // This validates dependency injection structure is working as expected
        systemIntegrationService.pingClients();
        
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }

    /**
     * Returns basic service metadata (e.g., service name, environment, framework).
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> info() {
        log.info("Received request for /api/v1/info");
        
        Map<String, String> infoData = systemIntegrationService.getServiceInfo();
        
        return ResponseEntity.ok(ApiResponse.success(infoData));
    }
}
