package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.service.ServerMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ServerMonitorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServerMetricsService serverMetricsService;

    @InjectMocks
    private ServerMonitorController serverMonitorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serverMonitorController).build();
    }

    @Test
    void getMetrics_success() throws Exception {
        when(serverMetricsService.getLatestMetrics())
                .thenReturn(Map.of("cpu", 25.0, "memory", 50.0, "tps", 20.0, "onlinePlayers", 10));

        mockMvc.perform(get("/api/server/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.cpu").value(25.0));
    }
}
