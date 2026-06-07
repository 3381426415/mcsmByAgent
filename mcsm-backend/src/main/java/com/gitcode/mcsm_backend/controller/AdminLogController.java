package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.AdminLog;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AdminLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
/**
 * 管理员日志接口 - 查询管理员操作日志（分页、按操作人筛选）
 */
@RequestMapping("/api/admin-log")
@PreAuthorize("hasAuthority('admin:log')")
public class AdminLogController {

    @Autowired
    private AdminLogService adminLogService;

    /**
     * 分页查询日志
     */
    @GetMapping("/list")
    public Result<Page<AdminLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operatorName) {
        Page<AdminLog> data = adminLogService.getLogPage(page, size, module, operatorName);
        return Result.success("获取成功", data);
    }
}