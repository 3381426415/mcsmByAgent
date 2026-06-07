package com.gitcode.mcsm_backend.config;

import com.gitcode.mcsm_backend.handler.PluginWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 插件 WebSocket 配置 - 注册 /ws/plugin 端点
 * 与前端 STOMP WebSocket（/ws）互不干扰
 */
@Configuration
@EnableWebSocket
public class PluginWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private PluginWebSocketHandler pluginWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pluginWebSocketHandler, "/ws/plugin")
                .setAllowedOrigins("*");
    }
}
