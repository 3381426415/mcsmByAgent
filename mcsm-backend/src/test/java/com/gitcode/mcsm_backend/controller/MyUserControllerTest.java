package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.MyUserService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MyUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MyUserService userService;

    @InjectMocks
    private MyUserController myUserController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(myUserController).build();
    }

    private MyUser createUser(Long id, String username) {
        MyUser user = new MyUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setBaned(false);
        user.setMoney(1000L);
        return user;
    }

    @Test
    void list_allUsers() throws Exception {
        when(userService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(createUser(1L, "admin")));

        mockMvc.perform(get("/api/users/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].username").value("admin"));
    }

    @Test
    void add_success() throws Exception {
        when(userService.save(any(MyUser.class))).thenReturn(true);

        mockMvc.perform(post("/api/users/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUser(null, "newuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void add_failure() throws Exception {
        when(userService.save(any(MyUser.class))).thenReturn(false);

        mockMvc.perform(post("/api/users/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUser(null, "fail"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void update_success() throws Exception {
        when(userService.updateById(any(MyUser.class))).thenReturn(true);

        mockMvc.perform(put("/api/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUser(1L, "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void delete_success() throws Exception {
        when(userService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void setBanPublish_success() throws Exception {
        when(userService.updateById(any(MyUser.class))).thenReturn(true);

        mockMvc.perform(put("/api/users/ban-publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"banPublish\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
