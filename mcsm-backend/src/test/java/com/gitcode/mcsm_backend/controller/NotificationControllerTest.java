package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.Notification;
import com.gitcode.mcsm_backend.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        MyUser mockUser = new MyUser();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(auth.getPrincipal()).thenReturn(mockUser);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_success() throws Exception {
        Page<Notification> page = new Page<>(1, 10);
        Notification n = new Notification();
        n.setId(1L);
        n.setTitle("测试通知");
        page.setRecords(List.of(n));
        page.setTotal(1);
        when(notificationService.getUserNotifications(anyLong(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/notification/list").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.records[0].title").value("测试通知"));
    }

    @Test
    void unreadCount_success() throws Exception {
        when(notificationService.getUnreadCount(anyLong())).thenReturn(5L);

        mockMvc.perform(get("/api/notification/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void markAsRead_success() throws Exception {
        doNothing().when(notificationService).markAsRead(anyLong(), anyLong());

        mockMvc.perform(post("/api/notification/read/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void markAllAsRead_success() throws Exception {
        doNothing().when(notificationService).markAllAsRead(anyLong());

        mockMvc.perform(post("/api/notification/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
