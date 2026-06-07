package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlayerControllTest {

    private MockMvc mockMvc;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerControll playerControll;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(playerControll).build();
    }

    @Test
    void listAllPlayers_success() throws Exception {
        PlayerDTO player = new PlayerDTO();
        player.setNickname("TestPlayer");
        when(playerService.getAllPlayers()).thenReturn(List.of(player));

        mockMvc.perform(get("/api/player/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].nickname").value("TestPlayer"));
    }

    @Test
    void listAllPlayers_failure() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(null);

        mockMvc.perform(get("/api/player/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void kickPlayer_success() throws Exception {
        when(playerService.kickPlayerRemote(anyString()))
                .thenReturn(Result.successMsg("踢出成功"));

        mockMvc.perform(post("/api/player/kick").param("uuid", "test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void banPlayer_success() throws Exception {
        when(playerService.banPlayerRemote(anyString()))
                .thenReturn(Result.successMsg("封禁成功"));

        mockMvc.perform(post("/api/player/ban").param("uuid", "test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void unbanPlayer_success() throws Exception {
        when(playerService.unbanPlayerRemote(anyString()))
                .thenReturn(Result.successMsg("解封成功"));

        mockMvc.perform(post("/api/player/unban").param("uuid", "test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void isPlayerBanned_notBanned() throws Exception {
        when(playerService.isPlayerBannedRemote(anyString()))
                .thenReturn(Result.success("查询成功", false));

        mockMvc.perform(get("/api/player/is-banned").param("uuid", "test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void getInventory_success() throws Exception {
        when(playerService.getPlayerInventoryRemote(anyString()))
                .thenReturn(Result.success("获取成功", List.of()));

        mockMvc.perform(get("/api/player/inventory").param("uuid", "test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void updateBySlot_success() throws Exception {
        when(playerService.updateItemBySlotRemote(anyString(), anyInt(), any()))
                .thenReturn(Result.successMsg("更新成功"));

        mockMvc.perform(post("/api/player/inventory/updateBySlot")
                        .param("uuid", "test-uuid")
                        .param("slot", "0")
                        .param("newCount", "64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
