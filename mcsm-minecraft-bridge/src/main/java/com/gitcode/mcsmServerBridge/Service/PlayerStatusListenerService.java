package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.Manager.DatabaseManager;
import com.gitcode.mcsmServerBridge.McsmBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class PlayerStatusListenerService implements Listener {
    private final McsmBridge plugin;
    private final DatabaseManager dbManager;
    private final String serverId;

    public PlayerStatusListenerService(McsmBridge plugin, DatabaseManager dbManager, String serverId) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.serverId = serverId;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getName();
        long now = System.currentTimeMillis();

        // 异步写数据库
        CompletableFuture.runAsync(() -> {
            syncPlayerOnline(uuid, name, 1, now);
            checkPendingItems(uuid, event);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getName();
        long now = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> syncPlayerOnline(uuid, name, 0, now));
    }

    /**
     * 同步玩家在线状态到 gameplayer 表
     * 上线：INSERT 或 UPDATE（无条件）
     * 下线：仅当 serverId 匹配时才置离线（防止群组服切换误标）
     */
    private void syncPlayerOnline(String uuid, String name, int isOnline, long lastPlayed) {
        try (Connection conn = dbManager.getConnection()) {
            if (isOnline == 1) {
                // 上线：INSERT ... ON DUPLICATE KEY UPDATE
                String sql = "INSERT INTO gameplayer (uuid, nickname, is_online, last_played, server_id) " +
                        "VALUES (?, ?, 1, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), is_online = 1, " +
                        "last_played = VALUES(last_played), server_id = VALUES(server_id)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid);
                    ps.setString(2, name);
                    ps.setLong(3, lastPlayed);
                    ps.setString(4, serverId);
                    ps.executeUpdate();
                }
            } else {
                // 下线：仅 serverId 匹配时才更新
                String sql = "UPDATE gameplayer SET is_online = 0, last_played = ?, server_id = NULL " +
                        "WHERE uuid = ? AND server_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, lastPlayed);
                    ps.setString(2, uuid);
                    ps.setString(3, serverId);
                    ps.executeUpdate();
                }
            }
            plugin.getLogger().info("Sync status [" + serverId + "]: " + name + " -> " + (isOnline == 1 ? "online" : "offline"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sync player status: " + e.getMessage());
        }
    }

    private void checkPendingItems(String uuid, PlayerJoinEvent event) {
        try {
            int count = plugin.getPendingItemService().getUnclaimedItems(uuid).size();
            if (count > 0) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    event.getPlayer().sendMessage("§a§l你有 " + count + " 件待领取物品！");
                    event.getPlayer().sendMessage("§7输入 §f/claim §7领取。");
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check pending items: " + e.getMessage());
        }
    }
}
