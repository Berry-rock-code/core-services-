package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.audit.AuditLogService;
import com.berryrock.integrationhub.client.BuildiumClient;
import com.berryrock.integrationhub.client.GoogleSheetsClient;
import com.berryrock.integrationhub.client.SalesforceClient;
import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.model.BuildiumAddressRecord;
import com.berryrock.integrationhub.model.GoogleSheetAddressRow;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import com.berryrock.integrationhub.util.AddressMatcher;
import com.berryrock.integrationhub.util.AddressNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AddressSyncServiceTest {

    @Mock
    private SalesforceClient salesforceClient;

    @Mock
    private GoogleSheetsClient googleSheetsClient;

    @Mock
    private BuildiumClient buildiumClient;

    @Mock
    private AuditLogService auditLogService;

    private AddressNormalizer addressNormalizer;
    private AddressMatcher addressMatcher;

    private AddressSyncService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addressNormalizer = new AddressNormalizer();
        addressMatcher = new AddressMatcher();
        service = new AddressSyncService(salesforceClient, googleSheetsClient, buildiumClient, auditLogService, addressNormalizer, addressMatcher);
    }

    @Test
    void testRun_SuccessPath() {
        // Setup mock data
        SalesforceAddressRecord sf1 = createSfRecord("SF1", "123 Main Street", "Saint Louis", "MO", "63101");
        SalesforceAddressRecord sf2 = createSfRecord("SF2", "456 Elm Ave", "St. Louis", "MO", "63102");
        when(salesforceClient.fetchAddressesForGoogleSheetBuildiumSync()).thenReturn(Arrays.asList(sf1, sf2));

        GoogleSheetAddressRow gs1 = createGsRow(1, "123 Main St", "St Louis", "MO", "63101", null, null);
        GoogleSheetAddressRow gs2 = createGsRow(2, "999 Oak Road", "Clayton", "MO", "63105", null, null);
        when(googleSheetsClient.fetchAddressRows(anyString(), anyString())).thenReturn(Arrays.asList(gs1, gs2));

        BuildiumAddressRecord bd1 = createBdRecord("P1", "123 Main Street", "Saint Louis", "MO", "63101");
        BuildiumAddressRecord bd2 = createBdRecord("P2", "555 Pine Ln", "Saint Louis", "MO", "63103");
        when(buildiumClient.fetchActiveLeaseAddresses()).thenReturn(Arrays.asList(bd1, bd2));

        AddressSyncRequest request = new AddressSyncRequest();
        request.setDryRun(false);
        request.setSyncGoogleSheet(true);
        request.setEnrichBuildium(true);
        request.setSheetId("id");
        request.setSheetName("name");

        AddressSyncSummary summary = service.run(request);

        assertEquals("SUCCESS", summary.getStatus());
        assertEquals(2, summary.getSalesforceRecordsFetched());
        assertEquals(2, summary.getGoogleSheetRowsFetched());
        assertEquals(2, summary.getBuildiumRecordsFetched());

        // Match expected counts:
        // SF has 123 Main St and 456 Elm Ave.
        // GS has 123 Main St (matches) and 999 Oak Rd.
        // BD has 123 Main St (matches) and 555 Pine Ln.
        assertEquals(1, summary.getGoogleSheetMatches());
        assertEquals(1, summary.getBuildiumMatches());

        // Unmatched: 456 Elm Ave is in SF but no matches.
        assertEquals(1, summary.getUnmatchedCount());
        assertTrue(summary.getWarnings() == null || summary.getWarnings().isEmpty());

        // Verify sheet write was called for 1 row
        verify(googleSheetsClient, times(1)).batchUpdateAddressMatches(anyString(), anyString(), anyList());
    }

    @Test
    void testRun_DryRun_NoWrites() {
        SalesforceAddressRecord sf1 = createSfRecord("SF1", "123 Main Street", "Saint Louis", "MO", "63101");
        when(salesforceClient.fetchAddressesForGoogleSheetBuildiumSync()).thenReturn(Collections.singletonList(sf1));

        GoogleSheetAddressRow gs1 = createGsRow(1, "123 Main St", "St Louis", "MO", "63101", null, null);
        when(googleSheetsClient.fetchAddressRows(anyString(), anyString())).thenReturn(Collections.singletonList(gs1));

        AddressSyncRequest request = new AddressSyncRequest();
        request.setDryRun(true);
        request.setSyncGoogleSheet(true);
        request.setEnrichBuildium(false);
        request.setSheetId("id");
        request.setSheetName("name");

        AddressSyncSummary summary = service.run(request);

        assertEquals("SUCCESS", summary.getStatus());
        assertEquals(1, summary.getGoogleSheetMatches());

        // Verify write was NOT called because of dryRun
        verify(googleSheetsClient, never()).batchUpdateAddressMatches(anyString(), anyString(), anyList());
    }

    @Test
    void testRun_DuplicateCollisions_WarnAndSkip() {
        // SF has duplicate 123 Main St
        SalesforceAddressRecord sf1 = createSfRecord("SF1", "123 Main Street", "Saint Louis", "MO", "63101");
        SalesforceAddressRecord sf2 = createSfRecord("SF2", "123 Main Street", "Saint Louis", "MO", "63101");
        when(salesforceClient.fetchAddressesForGoogleSheetBuildiumSync()).thenReturn(Arrays.asList(sf1, sf2));

        // GS has duplicate 456 Elm Ave
        SalesforceAddressRecord sf3 = createSfRecord("SF3", "456 Elm Ave", "St. Louis", "MO", "63102");
        when(salesforceClient.fetchAddressesForGoogleSheetBuildiumSync()).thenReturn(Arrays.asList(sf1, sf2, sf3));

        GoogleSheetAddressRow gs1 = createGsRow(1, "123 Main St", "St Louis", "MO", "63101", null, null);
        GoogleSheetAddressRow gs2 = createGsRow(2, "456 Elm Ave", "St Louis", "MO", "63102", null, null);
        GoogleSheetAddressRow gs3 = createGsRow(3, "456 Elm Ave", "St Louis", "MO", "63102", null, null);
        when(googleSheetsClient.fetchAddressRows(anyString(), anyString())).thenReturn(Arrays.asList(gs1, gs2, gs3));

        AddressSyncRequest request = new AddressSyncRequest();
        request.setDryRun(true);
        request.setSyncGoogleSheet(true);
        request.setEnrichBuildium(false);
        request.setSheetId("id");
        request.setSheetName("name");

        AddressSyncSummary summary = service.run(request);

        assertEquals("SUCCESS", summary.getStatus());

        // Duplicates share the same normalized address key.
        // SF dupes 123 Main St (key: 123 MAIN ST ST LOUIS MO 63101) match GS 123 Main St
        // SF 456 Elm Ave matches GS dupes 456 Elm Ave (key: 456 ELM AVE ST LOUIS MO 63102)
        assertEquals(2, summary.getGoogleSheetMatches());

        // There should be warnings
        assertNotNull(summary.getWarnings());
        assertFalse(summary.getWarnings().isEmpty());
        assertTrue(summary.getWarnings().stream().anyMatch(w -> w.contains("Duplicate Salesforce records")));
        assertTrue(summary.getWarnings().stream().anyMatch(w -> w.contains("Duplicate Google Sheet rows")));
    }

    private SalesforceAddressRecord createSfRecord(String id, String addr, String city, String state, String zip) {
        SalesforceAddressRecord r = new SalesforceAddressRecord();
        r.setOpportunityId(id);
        r.setAddressLine(addr);
        r.setCity(city);
        r.setState(state);
        r.setPostalCode(zip);
        return r;
    }

    private GoogleSheetAddressRow createGsRow(int row, String addr, String city, String state, String zip, String sfId, String bId) {
        GoogleSheetAddressRow r = new GoogleSheetAddressRow();
        r.setRowNumber(row);
        r.setAddress(addr);
        r.setCity(city);
        r.setState(state);
        r.setPostalCode(zip);
        r.setSalesforceId(sfId);
        r.setBuildiumId(bId);
        return r;
    }

    private BuildiumAddressRecord createBdRecord(String id, String addr, String city, String state, String zip) {
        BuildiumAddressRecord r = new BuildiumAddressRecord();
        r.setBuildiumPropertyId(id);
        r.setRawAddress(addr);
        r.setCity(city);
        r.setState(state);
        r.setPostalCode(zip);
        return r;
    }
}
