package com.gitcode.mcsm_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.LoginVo;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.Account;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BaseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private Account accountService;

    @Mock
    private MyUserService myUserService;

    @InjectMocks
    private BaseController baseController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(baseController).build();
    }

    @Test
    void login_success() throws Exception {
        MyUser user = new MyUser();
        user.setUsername("admin");
        user.setPassword("admin123");

        LoginVo loginVo = new LoginVo("test-token", List.of("admin"), List.of("admin:announcement"));
        when(accountService.login(any(MyUser.class))).thenReturn(Result.success("登录成功", loginVo));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.msg").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void login_fail_wrongPassword() throws Exception {
        MyUser user = new MyUser();
        user.setUsername("admin");
        user.setPassword("wrong");

        when(accountService.login(any(MyUser.class)))
                .thenReturn(Result.error("用户名或密码错误"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void register_success() throws Exception {
        MyUser user = new MyUser();
        user.setUsername("newuser");
        user.setPassword("password123");

        when(myUserService.registerUser(any(MyUser.class)))
                .thenReturn(Result.successMsg("注册成功"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void register_missingUsername() throws Exception {
        MyUser user = new MyUser();
        user.setPassword("password123");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }
}
