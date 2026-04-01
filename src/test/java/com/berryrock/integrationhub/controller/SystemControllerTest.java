package com.berryrock.integrationhub.controller;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.service.SystemIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemIntegrationService systemIntegrationService;

    @Test
    void testPingEndpoint() throws Exception {
        when(systemIntegrationService.pingClients()).thenReturn(Map.of("buildium", true));

        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void testInfoEndpoint() throws Exception {
        when(systemIntegrationService.getServiceInfo()).thenReturn(Map.of("service", "test-service"));

        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.service").value("test-service"));
    }
}
