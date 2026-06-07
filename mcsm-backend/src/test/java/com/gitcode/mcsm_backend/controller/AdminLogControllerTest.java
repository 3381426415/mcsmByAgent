package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.AdminLog;
import com.gitcode.mcsm_backend.service.AdminLogService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminLogController adminLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminLogController).build();
    }

    @Test
    void list_allLogs() throws Exception {
        Page<AdminLog> page = new Page<>(1, 10);
        AdminLog log = new AdminLog();
        log.setId(1L);
        log.setAction("测试操作");
        page.setRecords(List.of(log));
        page.setTotal(1);
        when(adminLogService.getLogPage(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/admin-log/list").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.records[0].action").value("测试操作"));
    }

    @Test
    void list_withModuleFilter() throws Exception {
        Page<AdminLog> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(adminLogService.getLogPage(1, 10, "用户管理", null)).thenReturn(page);

        mockMvc.perform(get("/api/admin-log/list")
                        .param("page", "1").param("size", "10").param("module", "用户管理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
