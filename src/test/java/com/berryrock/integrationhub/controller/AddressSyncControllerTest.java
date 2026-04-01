package com.berryrock.integrationhub.controller;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.service.AddressSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AddressSyncControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AddressSyncService addressSyncService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AddressSyncController controller = new AddressSyncController(addressSyncService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testRunAddressSync_Success() throws Exception {
        AddressSyncSummary summary = new AddressSyncSummary();
        summary.setStatus("SUCCESS");
        summary.setSalesforceRecordsFetched(10);
        summary.setGoogleSheetRowsFetched(20);
        summary.setGoogleSheetMatches(5);
        summary.setBuildiumRecordsFetched(15);
        summary.setBuildiumMatches(3);
        summary.setUnmatchedCount(2);

        when(addressSyncService.runSync(any(AddressSyncRequest.class))).thenReturn(summary);

        AddressSyncRequest request = new AddressSyncRequest();
        request.setDryRun(true);
        request.setSyncGoogleSheet(true);
        request.setEnrichBuildium(true);

        mockMvc.perform(post("/api/v1/workflows/address-sync/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.salesforceRecordsFetched").value(10))
                .andExpect(jsonPath("$.googleSheetRowsFetched").value(20));
    }
}
