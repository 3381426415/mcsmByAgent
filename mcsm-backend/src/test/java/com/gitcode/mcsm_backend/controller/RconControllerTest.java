package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PluginConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RconControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PluginConnectionManager pluginConnectionManager;

    @InjectMocks
    private RconController rconController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rconController).build();
    }

    @Test
    void send_success() throws Exception {
        when(pluginConnectionManager.sendCommand(anyString(), anyString(), anyMap()))
                .thenReturn(Result.success("执行成功", "There are 5 of a max of 20 players online"));

        mockMvc.perform(post("/api/mcsn/rcon/send")
                        .param("cmd", "list")
                        .param("serverId", "survival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value("There are 5 of a max of 20 players online"));
    }

    @Test
    void send_pluginNotConnected() throws Exception {
        when(pluginConnectionManager.sendCommand(anyString(), anyString(), anyMap()))
                .thenReturn(Result.error("插件未连接: survival"));

        mockMvc.perform(post("/api/mcsn/rcon/send")
                        .param("cmd", "list")
                        .param("serverId", "survival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }
}
