package com.gitcode.mcsm_backend.controller;


import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PlayerService;
import com.gitcode.mcsm_backend.service.ServerMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 游戏服务器接口 - 查询玩家列表、服务器指标、玩家详情等
 */
@Slf4j
@RestController
@RequestMapping("/api/gameServer")
public class gameServerController {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ServerMetricsService metricsService;

    @Value("${plugin.secret:}")
    private String pluginSecret;

    @GetMapping("/status")
    public Result<Map<String, Object>> getGameplayerNumsAndTps() {
        return metricsService.getGameplayerAndTps();
    }

    @PostMapping("/sync-status")
    public Result<String> syncStatus(@RequestBody PlayerDTO dto,
                                      @RequestHeader(value = "X-Plugin-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(pluginSecret)) {
            return Result.error("未授权");
        }
        log.info("被调用：{}", dto);
        return playerService.syncPlayerStatus(dto);
    }

    @PostMapping("/sync-status/{serverId}")
    public Result<String> syncStatusWithServer(@PathVariable String serverId, @RequestBody PlayerDTO dto,
                                                @RequestHeader(value = "X-Plugin-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(pluginSecret)) {
            return Result.error("未授权");
        }
        dto.setServerId(serverId);
        log.info("被调用 [服务器: {}]: {}", serverId, dto);
        return playerService.syncPlayerStatus(dto);
    }

}
