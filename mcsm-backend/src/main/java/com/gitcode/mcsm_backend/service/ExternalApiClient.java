package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 外部 API 调用客户端 — 通过 WebSocket 向 MC 服务器插件发送命令
 */
@Service
public class ExternalApiClient {

    @Autowired
    private PluginConnectionManager pluginConnectionManager;

    /**
     * 通过 WebSocket 向指定插件发送命令
     */
    public Result sendToPlugin(String serverId, String action, Map<String, Object> params) {
        return pluginConnectionManager.sendCommand(serverId, action, params);
    }

    /**
     * 检查插件是否通过 WebSocket 连接
     */
    public boolean isPluginConnected(String serverId) {
        return pluginConnectionManager.isConnected(serverId);
    }
}
