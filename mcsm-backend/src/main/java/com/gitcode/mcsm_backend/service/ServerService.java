package com.gitcode.mcsm_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 服务器服务 — 兼容层
 * 委托给 ServerManager，保持旧调用方不受影响
 * 默认操作 "default" 服务器实例
 */
@Service
public class ServerService {

    @Autowired
    private ServerManager serverManager;

    private ServerInstance si() {
        ServerInstance d = serverManager.getDefault();
        if (d == null) throw new IllegalStateException("未配置默认服务器实例");
        return d;
    }

    public boolean isRunning() {
        ServerInstance d = serverManager.getDefault();
        return d != null && d.getStatus() == ServerInstance.Status.RUNNING;
    }

    public ServerStatus getStatus() {
        ServerInstance si = si();
        return new ServerStatus(
                si.getStatus() == ServerInstance.Status.RUNNING,
                si.getPid());
    }

    public boolean startServer() { return si().start(); }
    public boolean stopServer() { return si().stop(); }
    public boolean restartServer() { return si().restart(); }
    public boolean sendCommand(String command) { return si().sendCommand(command); }

    // ==================== 多服务器方法 ====================

    public ServerInstance getInstance(String serverId) {
        return serverManager.get(serverId);
    }

    public List<Map<String, Object>> listServers() {
        return serverManager.listAll();
    }

    public RingBuffer.ConsoleData getConsole(String serverId, long since) {
        ServerInstance si = serverManager.get(serverId);
        if (si == null) return new RingBuffer.ConsoleData(java.util.Collections.emptyList(), -1);
        return si.getConsole(since);
    }

    public record ServerStatus(boolean running, long pid) {}
}
