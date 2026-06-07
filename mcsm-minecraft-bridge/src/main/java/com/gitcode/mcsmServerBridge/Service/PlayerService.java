package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.Common.Result;
import com.gitcode.mcsmServerBridge.Manager.DatabaseManager;
import com.gitcode.mcsmServerBridge.McsmBridge;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import com.gitcode.mcsmServerBridge.Service.OnlinePlayerEditService;
import java.util.UUID;

public class PlayerService {

    private final McsmBridge plugin;

    public PlayerService(McsmBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * 踢出在线玩家
     */
    public Result<String> kickPlayer(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                String name = player.getName();
                player.kickPlayer("You have been kicked by admin");
                return Result.success("Player " + name + " kicked", null);
            }
            return Result.error("Player not online or not found");
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid UUID format");
        } catch (Exception e) {
            return Result.error("Kick failed: " + e.getMessage());
        }
    }

    /**
     * 封禁玩家（支持离线封禁，按 UUID 封禁）
     */
    public Result<String> banPlayer(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName();
            if (name == null) {
                name = uuidStr;
            }

            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            ProfileBanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
            banList.addBan(profile, "Banned by admin", (java.util.Date) null, "Console");

            if (offlinePlayer.isOnline()) {
                offlinePlayer.getPlayer().kickPlayer("You have been banned");
            }
            return Result.success("Player " + name + " banned", null);
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid UUID format");
        } catch (Exception e) {
            return Result.error("Ban failed: " + e.getMessage());
        }
    }

    /**
     * 解封玩家（按 UUID 解封）
     */
    public Result<String> unbanPlayer(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            PlayerProfile profile = Bukkit.createProfile(uuid);
            ProfileBanList banList = Bukkit.getBanList(BanList.Type.PROFILE);

            if (!banList.isBanned(profile)) {
                return Result.error("Player is not banned");
            }

            banList.pardon(profile);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName();
            return Result.success("Player " + (name != null ? name : uuidStr) + " unbanned", null);
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid UUID format");
        } catch (Exception e) {
            return Result.error("Unban failed: " + e.getMessage());
        }
    }

    /**
     * 检查玩家是否被封禁（按 UUID 查询）
     */
    public Result<Boolean> isPlayerBanned(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            PlayerProfile profile = Bukkit.createProfile(uuid);
            ProfileBanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
            boolean banned = banList.isBanned(profile);
            return Result.success("Query success", banned);
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid UUID format");
        } catch (Exception e) {
            return Result.error("Query failed: " + e.getMessage());
        }
    }



    /**
     * 重置所有玩家数据（插件启动/关闭时调用，将数据库中的在线状态全部置为离线）
     */
    public Result<String> resetAllPlayers() {
        try {
            DatabaseManager dbManager = plugin.getDbManager();
            if (dbManager == null) {
                return Result.error("Database not connected");
            }

            String sql = "UPDATE gameplayer SET is_online = 0 WHERE is_online = 1";
            try (java.sql.Connection conn = dbManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                int updated = ps.executeUpdate();
                plugin.getLogger().info("Reset " + updated + " players to offline");
                return Result.success("All player data reset (" + updated + " players)", null);
            }
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("Failed to reset player data: " + e.getMessage());
            return Result.error("Reset failed: " + e.getMessage());
        }
    }


}
