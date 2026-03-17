package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.service.IntegrationHealthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationHealthController
{
    private final IntegrationHealthService integrationHealthService;

    public IntegrationHealthController(IntegrationHealthService integrationHealthService)
    {
        this.integrationHealthService = integrationHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health()
    {
        return ResponseEntity.ok(integrationHealthService.checkAll());
    }
}