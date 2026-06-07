package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PluginConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 命令执行接口 - 通过插件 WebSocket 向 MC 服务器发送命令
 */
@RestController
@RequestMapping("/api/mcsn/rcon")
@PreAuthorize("hasAuthority('admin:server')")
public class RconController {

    @Autowired
    private PluginConnectionManager pluginConnectionManager;

    @PostMapping("/send")
    @LogRecord(
            module = "服务器管理",
            action = "执行命令",
            description = "执行命令：#{#cmd}"
    )
    public Result<String> send(@RequestParam String cmd, @RequestParam(defaultValue = "default") String serverId) {
        Result result = pluginConnectionManager.sendCommand(serverId, "executeCommand", Map.of("command", cmd));
        if (result.getCode() == 2000) {
            return Result.success("指令执行成功", result.getData() != null ? result.getData().toString() : "");
        }
        return Result.error(result.getMsg());
    }
}