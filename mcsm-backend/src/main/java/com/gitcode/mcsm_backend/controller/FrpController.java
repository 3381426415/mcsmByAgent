package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.FrpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FRP 客户端管理 API
 */
@RestController
@RequestMapping("/api/frp")
public class FrpController {

    @Autowired
    private FrpService frpService;

    @GetMapping("/status")
    public Result<Map> getStatus() {
        return Result.success("ok", Map.of(
                "status", frpService.getStatus(),
                "installed", frpService.isInstalled(),
                "running", frpService.isRunning()
        ));
    }

    @PostMapping("/start")
    public Result<String> start() {
        if (!frpService.isInstalled()) {
            return Result.error("frpc 未安装，请将 frpc 二进制放入 frp/ 目录");
        }
        boolean success = frpService.start();
        return success ? Result.successMsg("frpc 已启动") : Result.error("frpc 启动失败，查看日志了解详情");
    }

    @PostMapping("/stop")
    public Result<String> stop() {
        boolean success = frpService.stop();
        return success ? Result.successMsg("frpc 已停止") : Result.error("frpc 停止失败");
    }

    @GetMapping("/config")
    public Result<String> getConfig() {
        return Result.success("ok", frpService.getConfig());
    }

    @PutMapping("/config")
    public Result<String> saveConfig(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null) return Result.error("缺少 content 字段");
        boolean success = frpService.saveConfig(content);
        return success ? Result.successMsg("配置已保存") : Result.error("配置保存失败");
    }

    @GetMapping("/logs")
    public Result<Map> getLogs(@RequestParam(defaultValue = "100") int lines) {
        List<String> logs = frpService.getLogs(lines);
        return Result.success("ok", Map.of("logs", logs));
    }
}
