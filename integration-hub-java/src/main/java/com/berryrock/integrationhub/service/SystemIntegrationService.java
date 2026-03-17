package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.audit.AuditService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SystemIntegrationService.class);

    private final BuildiumClient buildiumClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final SalesforceClient salesforceClient;
    private final AuditService auditService;

    public SystemIntegrationService(BuildiumClient buildiumClient, 
                                    GoogleSheetsClient googleSheetsClient,
                                    SalesforceClient salesforceClient,
                                    AuditService auditService) {
        this.buildiumClient = buildiumClient;
        this.googleSheetsClient = googleSheetsClient;
        this.salesforceClient = salesforceClient;
        this.auditService = auditService;
    }

    /**
     * Orchestrates checking status of underlying services.
     */
    public Map<String, Object> pingClients() {
        log.info("Executing client ping across all integration boundaries.");
        
        Map<String, Object> status = new HashMap<>();
        status.put("buildium", buildiumClient.ping());
        status.put("googleSheets", googleSheetsClient.ping());
        status.put("salesforceConfigured", salesforceClient.isConfigured());
        
        auditService.logEvent("PING_CHECK", "SYSTEM", "Verified integration client placeholders.");

        return status;
    }

    /**
     * Retrieves application metadata
     */
    public Map<String, String> getServiceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("service", "integration-hub");
        info.put("description", "Unified integration and automation core");
        info.put("language", "Java 21");
        info.put("framework", "Spring Boot");
        return info;
    }
}
