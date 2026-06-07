package com.gitcode.mcsm_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.Permission;
import com.gitcode.mcsm_backend.Entity.Role;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.mapper.PermissionMapper;
import com.gitcode.mcsm_backend.service.PermissionService;
import com.gitcode.mcsm_backend.service.RoleService;
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
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private RoleController roleController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
    }

    @Test
    void list_success() throws Exception {
        Role role = new Role();
        role.setRoleId(1L);
        role.setRoleName("admin");
        when(roleService.list()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/roles/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].roleName").value("admin"));
    }

    @Test
    void add_success() throws Exception {
        when(roleService.save(any(Role.class))).thenReturn(true);

        mockMvc.perform(post("/api/roles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Role())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void delete_systemRole_shouldFail() throws Exception {
        mockMvc.perform(delete("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void delete_success() throws Exception {
        when(roleService.removeById(3L)).thenReturn(true);

        mockMvc.perform(delete("/api/roles/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void getAllPermissions_success() throws Exception {
        Permission perm = new Permission();
        perm.setPermId(1);
        perm.setName("用户管理");
        when(permissionService.getAllPermissions()).thenReturn(List.of(perm));

        mockMvc.perform(get("/api/roles/all-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void assignPermissions_success() throws Exception {
        doNothing().when(permissionService).updateRolePermissions(anyLong(), any());

        mockMvc.perform(post("/api/roles/assign-permissions")
                        .param("roleId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
