package com.gitcode.mcsmServerBridge.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsmServerBridge.Common.Result;
import com.gitcode.mcsmServerBridge.McsmBridge;
import com.gitcode.mcsmServerBridge.Service.NbtService;
import com.gitcode.mcsmServerBridge.Service.PendingItemService;
import com.gitcode.mcsmServerBridge.Service.PlayerService;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public class PluginWebSocketClient extends WebSocketClient {

    private final McsmBridge plugin;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MCSM-WS-Heartbeat");
        t.setDaemon(true);
        return t;
    });
    private final java.util.concurrent.ExecutorService commandExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "MCSM-Cmd");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> heartbeatTask;
    private boolean intentionallyClosed = false;

    public PluginWebSocketClient(McsmBridge plugin, URI serverUri) {
        super(serverUri);
        this.plugin = plugin;
        setConnectionLostTimeout(0); // 禁用库自带的超时，自己管理
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        plugin.getLogger().info("[WS] Connected to backend: " + getURI());

        // 发送注册消息
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "register");
        msg.put("serverId", plugin.getConfig().getString("server-id", "default"));
        msg.put("version", plugin.getDescription().getVersion());
        send(json(msg));

        // 启动心跳
        startHeartbeat();
    }

    @Override
    public void onMessage(String message) {
        Map<String, Object> data;
        try {
            data = objectMapper.readValue(message, new TypeReference<>() {});
        } catch (Exception e) {
            plugin.getLogger().warning("[WS] Message parse failed: " + message);
            return;
        }

        String type = (String) data.get("type");
        if (type == null) return;

        switch (type) {
            case "register_ack" -> plugin.getLogger().info("[WS] Registered: " + data.get("serverId"));
            case "command" -> handleCommand(data);
            case "heartbeat_ack" -> { /* 正常 */ }
            default -> plugin.getLogger().warning("[WS] Unknown message type: " + type);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        stopHeartbeat();
        plugin.getLogger().warning("[WS] Connection closed: code=" + code + " reason=" + reason);

        if (!intentionallyClosed) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        plugin.getLogger().severe("[WS] Error: " + ex.getMessage());
    }

    public void closeIntentionally() {
        intentionallyClosed = true;
        stopHeartbeat();
        close();
    }

    // ==================== 命令分发 ====================

    @SuppressWarnings("unchecked")
    private void handleCommand(Map<String, Object> message) {
        String requestId = (String) message.get("requestId");
        String action = (String) message.get("action");
        Map<String, Object> params = (Map<String, Object>) message.getOrDefault("params", Map.of());

        if (requestId == null || action == null) return;

        // 异步处理，避免阻塞 WebSocket 线程（使用固定大小线程池）
        CompletableFuture.supplyAsync(() -> dispatchAction(action, params), commandExecutor)
                .thenAccept(result -> sendResponse(requestId, result))
                .exceptionally(ex -> {
                    sendResponse(requestId, Result.error("Command execution error: " + ex.getMessage()));
                    return null;
                });
    }

    private Result<?> dispatchAction(String action, Map<String, Object> params) {
        PlayerService playerService = plugin.getPlayerService();
        NbtService nbtService = plugin.getNbtService();
        PendingItemService pendingService = plugin.getPendingItemService();

        return switch (action) {
            case "getServerStatus" -> getServerStatus();
            case "kickPlayer" -> playerService.kickPlayer((String) params.get("uuid"));
            case "banPlayer" -> playerService.banPlayer((String) params.get("uuid"));
            case "unbanPlayer" -> playerService.unbanPlayer((String) params.get("uuid"));
            case "isBanned" -> playerService.isPlayerBanned((String) params.get("uuid"));
            case "getInventory" -> nbtService.getInventoryJson(java.util.UUID.fromString((String) params.get("uuid")));
            case "updateItemSlot" -> {
                java.util.UUID uuid = java.util.UUID.fromString((String) params.get("uuid"));
                int slot = toInt(params.get("slot"));
                int newCount = toInt(params.get("newCount"));
                yield nbtService.updateItemBySlot(uuid, slot, newCount);
            }
            case "executeCommand" -> executeCommand((String) params.get("command"));
            case "addPendingChange" -> addPendingChange(params);
            default -> Result.error("Unknown command: " + action);
        };
    }

    // ==================== 业务方法 ====================

    private Result<?> getServerStatus() {
        // 需要在主线程获取 Bukkit 数据
        try {
            Map<String, Object> data = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("onlineCount", Bukkit.getOnlinePlayers().size());
                result.put("tps", String.format("%.2f", Bukkit.getTPS()[0]));
                return result;
            }).get(5, TimeUnit.SECONDS);
            return Result.success("Query success", data);
        } catch (Exception e) {
            return Result.error("Get status failed: " + e.getMessage());
        }
    }

    private Result<?> executeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return Result.error("Command cannot be empty");
        }
        try {
            boolean success = Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            ).get(5, TimeUnit.SECONDS);
            return success ? Result.successMsg("Command executed") : Result.error("Command execution failed");
        } catch (Exception e) {
            return Result.error("Command execution error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Result<?> addPendingChange(Map<String, Object> params) {
        try {
            PendingItemService service = plugin.getPendingItemService();
            String playerUuid = (String) params.get("playerUuid");
            String itemKey = (String) params.get("itemKey");
            int amount = toInt(params.get("amount"));
            String displayName = (String) params.get("displayName");
            String nbtData = (String) params.get("nbtData");
            String source = (String) params.get("source");
            String sourceId = (String) params.get("sourceId");
            return service.addPendingItem(playerUuid, itemKey, amount, displayName, nbtData, source, sourceId);
        } catch (Exception e) {
            return Result.error("Store item failed: " + e.getMessage());
        }
    }

    // ==================== 通信 ====================

    private void sendResponse(String requestId, Result<?> result) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "response");
        msg.put("requestId", requestId);
        msg.put("code", result.getCode());
        msg.put("msg", result.getMsg());
        msg.put("data", result.getData());
        send(json(msg));
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                send(json(Map.of("type", "heartbeat")));
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    private void scheduleReconnect() {
        plugin.getLogger().info("[WS] Reconnecting in 5s...");
        scheduler.schedule(() -> {
            if (intentionallyClosed) return;
            try {
                reconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[WS] Reconnect failed: " + e.getMessage());
                scheduleReconnect(); // 继续重试
            }
        }, 5, TimeUnit.SECONDS);
    }

    // ==================== 工具 ====================

    private String json(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) return Integer.parseInt(s);
        return 0;
    }
}
