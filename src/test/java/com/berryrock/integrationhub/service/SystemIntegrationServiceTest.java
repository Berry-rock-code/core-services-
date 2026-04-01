package com.berryrock.integrationhub.service;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.audit.AuditService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemIntegrationServiceTest
{
    @Mock
    private BuildiumClient buildiumClient;

    @Mock
    private GoogleSheetsClient googleSheetsClient;

    @Mock
    private SalesforceClient salesforceClient;

    @Mock
    private AuditService auditService;

    private SystemIntegrationService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new SystemIntegrationService(
                buildiumClient,
                googleSheetsClient,
                salesforceClient,
                auditService
        );
    }

    @Test
    void testPingClients()
    {
        when(buildiumClient.ping()).thenReturn(true);
        when(googleSheetsClient.ping()).thenReturn(true);
        when(salesforceClient.isConfigured()).thenReturn(false);

        Map<String, Object> result = service.pingClients();

        assertTrue((Boolean) result.get("buildium"));
        assertTrue((Boolean) result.get("googleSheets"));
        assertEquals(false, result.get("salesforceConfigured"));

        verify(auditService).logEvent(
                "PING_CHECK",
                "SYSTEM",
                "Verified integration client placeholders."
        );
    }
}
