package com.berryrock.integrationhub.client;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.config.BuildiumProperties;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BuildiumClientImplTest
{
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private BuildiumProperties properties;

    private BuildiumClientImpl client;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        client = new BuildiumClientImpl(webClient, properties);
        when(properties.isEnabled()).thenReturn(true);

        // Wire up the WebClient chain
        // This covers .get() -> .uri(...) -> .accept(...) -> .retrieve() -> .bodyToMono()
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(Function.class));
        doReturn(requestHeadersSpec).when(requestHeadersSpec).accept(any());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    }

    @Test
    void testFetchActiveLeaseAddresses_HappyPath()
    {
        // Unit page response - one unit with a clean address
        List<Map<String, Object>> unitPage = List.of(
                Map.of(
                        "Id", "101",
                        "PropertyId", "P1",
                        "Address", Map.of(
                                "AddressLine1", "123 Main St",
                                "City", "Saint Louis",
                                "State", "MO",
                                "PostalCode", "63101"
                        )
                )
        );

        // Lease page response - one active lease tied to unit 101
        List<Map<String, Object>> leasePage = List.of(
                Map.of(
                        "Id", "L999",
                        "Unit", Map.of("Id", "101")
                )
        );

        // First call returns units, second call returns leases
        doReturn(Mono.just(unitPage))
                .doReturn(Mono.just(leasePage))
                .when(responseSpec).bodyToMono(List.class);

        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertEquals(1, results.size());

        BuildiumAddressRecord record = results.get(0);
        assertEquals("123 Main St", record.getRawAddress());
        assertEquals("Saint Louis", record.getCity());
        assertEquals("MO", record.getState());
        assertEquals("63101", record.getPostalCode());
        assertEquals("P1", record.getBuildiumPropertyId());
        assertEquals("101", record.getBuildiumUnitId());
        assertEquals("L999", record.getBuildiumLeaseId());
    }

    @Test
    void testFetchActiveLeaseAddresses_UnitWithNoActiveLease()
    {
        // Unit exists but has no active lease
        List<Map<String, Object>> unitPage = List.of(
                Map.of(
                        "Id", "202",
                        "PropertyId", "P2",
                        "Address", Map.of(
                                "AddressLine1", "456 Oak Ave",
                                "City", "Clayton",
                                "State", "MO",
                                "PostalCode", "63105"
                        )
                )
        );

        List<Map<String, Object>> leasePage = List.of(); // No active leases

        doReturn(Mono.just(unitPage))
                .doReturn(Mono.just(leasePage))
                .when(responseSpec).bodyToMono(List.class);

        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertEquals(1, results.size());
        assertEquals("456 Oak Ave", results.get(0).getRawAddress());
        // Lease ID should be null - unit exists but isn't actively leased
        assertNull(results.get(0).getBuildiumLeaseId());
    }

    @Test
    void testFetchActiveLeaseAddresses_UnitMissingAddressLine1_IsSkipped()
    {
        // Unit with no AddressLine1 should be skipped entirely
        List<Map<String, Object>> unitPage = List.of(
                Map.of(
                        "Id", "303",
                        "PropertyId", "P3",
                        "Address", Map.of(
                                "City", "Saint Louis",
                                "State", "MO",
                                "PostalCode", "63101"
                                // No AddressLine1
                        )
                )
        );

        List<Map<String, Object>> leasePage = List.of();

        doReturn(Mono.just(unitPage))
                .doReturn(Mono.just(leasePage))
                .when(responseSpec).bodyToMono(List.class);

        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertTrue(results.isEmpty());
    }

    @Test
    void testFetchActiveLeaseAddresses_DisabledIntegration_ReturnsEmpty()
    {
        when(properties.isEnabled()).thenReturn(false);

        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertTrue(results.isEmpty());
        verifyNoInteractions(webClient);
    }

    @Test
    void testFetchActiveLeaseAddresses_Pagination()
    {
        // Simulate exactly PAGE_SIZE (1000) units coming back on page 1,
        // which triggers a second page fetch that returns empty - ending pagination
        List<Map<String, Object>> fullPage = buildUnitPage(1000, 1);
        List<Map<String, Object>> emptyPage = List.of();
        List<Map<String, Object>> leasePage = List.of();

        doReturn(Mono.just(fullPage))   // units page 1
                .doReturn(Mono.just(emptyPage)) // units page 2 - empty, stops pagination
                .doReturn(Mono.just(leasePage)) // leases page 1
                .when(responseSpec).bodyToMono(List.class);

        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertEquals(1000, results.size());
    }

    @Test
    void testFetchActiveLeaseAddresses_ApiFailure_ReturnsEmptyGracefully()
    {
        doReturn(Mono.error(new RuntimeException("Buildium API unavailable")))
                .when(responseSpec).bodyToMono(List.class);

        // Should not throw - should log and return empty
        List<BuildiumAddressRecord> results = client.fetchActiveLeaseAddresses();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // Helper to generate a page of N units with valid addresses
    private List<Map<String, Object>> buildUnitPage(int count, int startId)
    {
        List<Map<String, Object>> units = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            units.add(Map.of(
                    "Id", String.valueOf(startId + i),
                    "PropertyId", "P" + (startId + i),
                    "Address", Map.of(
                            "AddressLine1", (startId + i) + " Test St",
                            "City", "Saint Louis",
                            "State", "MO",
                            "PostalCode", "63101"
                    )
            ));
        }
        return units;
    }
}