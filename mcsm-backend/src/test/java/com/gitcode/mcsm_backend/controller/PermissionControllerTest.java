package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController).build();
    }

    @Test
    void getUserRoles_success() throws Exception {
        when(permissionService.getRoleNamesByUserId(1L))
                .thenReturn(List.of("admin", "user"));

        mockMvc.perform(get("/api/permissions/user/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0]").value("admin"));
    }

    @Test
    void getUserPermissionCodes_success() throws Exception {
        when(permissionService.getPermissionCodesByUserId(1L))
                .thenReturn(List.of("admin:user", "admin:role"));

        mockMvc.perform(get("/api/permissions/user/codes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void updateUserRoles_success() throws Exception {
        doNothing().when(permissionService).updateUserRoles(anyLong(), any());

        mockMvc.perform(post("/api/permissions/user/update-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"roleId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void updateUserRoles_missingRoleId() throws Exception {
        mockMvc.perform(post("/api/permissions/user/update-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void updateRolePermissions_success() throws Exception {
        doNothing().when(permissionService).updateRolePermissions(anyLong(), any());

        mockMvc.perform(post("/api/permissions/role/update-permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":1,\"permIds\":[1,2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
