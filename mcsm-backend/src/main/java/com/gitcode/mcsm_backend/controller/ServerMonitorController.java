package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.ServerMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
/**
 * 服务器监控接口 - 查询服务器状态、TPS、在线玩家等运行指标
 */
@RequestMapping("/api/server")
@CrossOrigin(origins = "*")
public class ServerMonitorController {

    @Autowired
    private ServerMetricsService serverMetricsService;

    @GetMapping("/metrics")
    public ResponseEntity<Result<Map<String, Object>>> getMetrics() {
        Map<String, Object> data = serverMetricsService.getLatestMetrics();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(Result.success("获取监控数据成功", data));
    }
}