package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PluginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PluginService pluginService;

    @InjectMocks
    private PluginController pluginController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pluginController).build();
    }

    @Test
    void list_success() throws Exception {
        when(pluginService.getPluginList())
                .thenReturn(Result.success("获取成功", List.of(Map.of("name", "TestPlugin"))));

        mockMvc.perform(get("/api/plugins/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void enable_success() throws Exception {
        when(pluginService.enablePlugin(anyString()))
                .thenReturn(Result.successMsg("启用成功"));

        mockMvc.perform(post("/api/plugins/enable").param("fileName", "test.jar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void disable_success() throws Exception {
        when(pluginService.disablePlugin(anyString()))
                .thenReturn(Result.successMsg("禁用成功"));

        mockMvc.perform(post("/api/plugins/disable").param("fileName", "test.jar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void delete_success() throws Exception {
        when(pluginService.deletePlugin(anyString()))
                .thenReturn(Result.successMsg("删除成功"));

        mockMvc.perform(delete("/api/plugins/delete").param("fileName", "test.jar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void upload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jar", "application/java-archive", "content".getBytes());
        when(pluginService.uploadPlugin(any()))
                .thenReturn(Result.successMsg("上传成功"));

        mockMvc.perform(multipart("/api/plugins/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void listByServer_success() throws Exception {
        when(pluginService.getPluginList("server1"))
                .thenReturn(Result.success("获取成功", List.of()));

        mockMvc.perform(get("/api/plugins/server1/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
