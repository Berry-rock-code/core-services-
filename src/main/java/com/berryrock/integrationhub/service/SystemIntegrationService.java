package com.berryrock.integrationhub.service;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.audit.AuditService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service providing system-level operations used by the general-purpose REST endpoints.
 *
 * Part of the service layer — supports the {@link com.berryrock.integrationhub.controller.SystemController}
 * endpoints ({@code /api/v1/ping} and {@code /api/v1/info}). Keeps business logic out of
 * the controller layer by encapsulating the client ping orchestration and static service
 * metadata here.
 *
 * The {@link #pingClients()} method is also used as a dependency-injection smoke test:
 * if any client bean fails to wire correctly at startup, calling this method on the first
 * request will surface the problem immediately rather than at the first pipeline run.
 */
@Service
public class SystemIntegrationService
{
    private static final Logger log = LoggerFactory.getLogger(SystemIntegrationService.class);

    private final BuildiumClient buildiumClient;
    private final GoogleSheetsClient googleSheetsClient;
    private final SalesforceClient salesforceClient;
    private final AuditService auditService;

    /**
     * Constructs the service with all required integration client and audit dependencies.
     *
     * @param buildiumClient      Buildium API client
     * @param googleSheetsClient  Google Sheets API client
     * @param salesforceClient    Salesforce REST API client
     * @param auditService        compliance audit logging service
     */
    public SystemIntegrationService(BuildiumClient buildiumClient,
                                    GoogleSheetsClient googleSheetsClient,
                                    SalesforceClient salesforceClient,
                                    AuditService auditService)
    {
        this.buildiumClient = buildiumClient;
        this.googleSheetsClient = googleSheetsClient;
        this.salesforceClient = salesforceClient;
        this.auditService = auditService;
    }

    /**
     * Pings all configured integration clients and returns their connectivity status.
     *
     * Calls the lightweight probe method on each client. For Salesforce, which does not
     * expose a separate ping, the configured flag is reported instead. Each result is
     * collected into a map keyed by vendor name. An audit event is recorded after all
     * probes complete.
     *
     * @return map of vendor name to status; {@code buildium} and {@code googleSheets}
     *         contain boolean reachability; {@code salesforceConfigured} contains the
     *         enabled flag
     */
    public Map<String, Object> pingClients()
    {
        log.info("Executing client ping across all integration boundaries.");

        Map<String, Object> status = new HashMap<>();
        status.put("buildium", buildiumClient.ping());
        status.put("googleSheets", googleSheetsClient.ping());
        status.put("salesforceConfigured", salesforceClient.isConfigured());

        auditService.logEvent("PING_CHECK", "SYSTEM", "Verified integration client placeholders.");

        return status;
    }

    /**
     * Returns static metadata describing the running service.
     *
     * Provides the service name, human-readable description, Java version, and
     * framework. Used by the {@code /api/v1/info} endpoint to confirm which build is
     * deployed without requiring access to environment variables.
     *
     * @return map of metadata key-value pairs
     */
    public Map<String, String> getServiceInfo()
    {
        Map<String, String> info = new HashMap<>();
        info.put("service", "integration-hub");
        info.put("description", "Unified integration and automation core");
        info.put("language", "Java 21");
        info.put("framework", "Spring Boot");
        return info;
    }
}
