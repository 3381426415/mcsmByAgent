package com.gitcode.mcsm_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PlayerService;
import com.gitcode.mcsm_backend.service.ServerMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GameServerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlayerService playerService;

    @Mock
    private ServerMetricsService metricsService;

    @InjectMocks
    private gameServerController gameServerController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gameServerController).build();
    }

    @Test
    void getStatus_success() throws Exception {
        when(metricsService.getGameplayerAndTps())
                .thenReturn(Result.success("获取成功", Map.of("online", 10, "tps", 20.0)));

        mockMvc.perform(get("/api/gameServer/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void syncStatus_success() throws Exception {
        when(playerService.syncPlayerStatus(any(PlayerDTO.class)))
                .thenReturn(Result.successMsg("同步成功"));

        PlayerDTO dto = new PlayerDTO();
        dto.setNickname("TestPlayer");
        dto.setUuid("test-uuid");

        mockMvc.perform(post("/api/gameServer/sync-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void syncStatusWithServer_success() throws Exception {
        when(playerService.syncPlayerStatus(any(PlayerDTO.class)))
                .thenReturn(Result.successMsg("同步成功"));

        PlayerDTO dto = new PlayerDTO();
        dto.setNickname("TestPlayer");

        mockMvc.perform(post("/api/gameServer/sync-status/server1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
