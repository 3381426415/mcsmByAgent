package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gitcode.mcsm_backend.Entity.Announcement;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AnnouncementService;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private AnnouncementController announcementController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(announcementController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
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

    private Announcement createAnnouncement(Long id, String title, Integer type) {
        Announcement a = new Announcement();
        a.setId(id);
        a.setTitle(title);
        a.setContent("测试内容");
        a.setType(type);
        a.setIsPublished(1);
        a.setServerIds("[\"default\"]");
        a.setCreateTime(LocalDateTime.now());
        a.setCreateBy(1L);
        return a;
    }

    @Test
    void latest_returnsWebAnnouncements() throws Exception {
        when(announcementService.getLatestWebAnnouncements())
                .thenReturn(List.of(createAnnouncement(1L, "测试公告", 1)));

        mockMvc.perform(get("/api/announcement/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].title").value("测试公告"));
    }

    @Test
    void list_returnsPagedAnnouncements() throws Exception {
        Page<Announcement> page = new Page<>(1, 10);
        page.setRecords(List.of(createAnnouncement(1L, "测试公告", 1)));
        page.setTotal(1);
        when(announcementService.getAnnouncementPage(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/announcement/list")
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.records[0].title").value("测试公告"));
    }

    @Test
    void list_withTypeFilter() throws Exception {
        Page<Announcement> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(announcementService.getAnnouncementPage(1, 10, 2)).thenReturn(page);

        mockMvc.perform(get("/api/announcement/list")
                        .param("page", "1").param("size", "10").param("type", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void publish_success() throws Exception {
        when(announcementService.publishAnnouncement(any(), anyLong()))
                .thenReturn(Result.successMsg("发布成功"));

        mockMvc.perform(post("/api/announcement/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAnnouncement(null, "新公告", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.msg").value("发布成功"));
    }

    @Test
    void publish_gameAnnouncementWithServerIds() throws Exception {
        Announcement a = createAnnouncement(null, "游戏公告", 2);
        a.setServerIds("[\"default\"]");
        when(announcementService.publishAnnouncement(any(), anyLong()))
                .thenReturn(Result.successMsg("发布成功"));

        mockMvc.perform(post("/api/announcement/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void saveDraft_success() throws Exception {
        when(announcementService.saveDraft(any(), anyLong()))
                .thenReturn(Result.successMsg("草稿保存成功"));

        mockMvc.perform(post("/api/announcement/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAnnouncement(null, "草稿", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void update_success() throws Exception {
        when(announcementService.updateAnnouncement(any()))
                .thenReturn(Result.successMsg("更新成功"));

        mockMvc.perform(put("/api/announcement/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAnnouncement(1L, "更新", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void delete_success() throws Exception {
        when(announcementService.deleteAnnouncement(1L))
                .thenReturn(Result.successMsg("删除成功"));

        mockMvc.perform(delete("/api/announcement/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void publish_failure() throws Exception {
        when(announcementService.publishAnnouncement(any(), anyLong()))
                .thenReturn(Result.error("发布失败"));

        mockMvc.perform(post("/api/announcement/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAnnouncement(null, "失败", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));
    }
}
