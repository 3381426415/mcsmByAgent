package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.RedeemCode;
import com.gitcode.mcsm_backend.service.RedeemCodeService;
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
class RedeemCodeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RedeemCodeService redeemCodeService;

    @InjectMocks
    private RedeemCodeController redeemCodeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(redeemCodeController).build();
    }

    @Test
    void list_success() throws Exception {
        Page<RedeemCode> page = new Page<>(1, 20);
        RedeemCode code = new RedeemCode();
        code.setId(1L);
        code.setCode("TEST123");
        code.setAmount(100);
        page.setRecords(List.of(code));
        page.setTotal(1);
        when(redeemCodeService.page(any(Page.class))).thenReturn(page);

        mockMvc.perform(get("/api/redeem/list").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void create_success() throws Exception {
        when(redeemCodeService.save(any(RedeemCode.class))).thenReturn(true);

        mockMvc.perform(post("/api/redeem/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void delete_success() throws Exception {
        when(redeemCodeService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/redeem/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
