package com.gitcode.mcsmServerBridge.command;

import com.gitcode.mcsmServerBridge.Manager.DatabaseManager;
import com.gitcode.mcsmServerBridge.McsmBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BindCommand implements CommandExecutor {

    private final McsmBridge plugin;
    private final DatabaseManager dbManager;

    public BindCommand(McsmBridge plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此指令！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            return false;
        }

        String inputUsername = args[0];
        String playerUuid = player.getUniqueId().toString();

        player.sendMessage("§6正在验证绑定信息...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection()) {

                // --- 1. 检查当前 UUID 是否已经绑定过任何账号 ---
                String checkUuidSql = "SELECT username FROM user WHERE bind_id = ?";
                try (PreparedStatement psCheck = conn.prepareStatement(checkUuidSql)) {
                    psCheck.setString(1, playerUuid);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            String boundName = rs.getString("username");
                            syncMsg(player, "§c§l✘ 绑定失败！§7你已经绑定过账号: §f" + boundName);
                            return;
                        }
                    }
                }

                // --- 2. 尝试匹配用户名并更新 bind_id ---
                // 这里增加了一个条件：bind_id IS NULL，确保不会顶替掉别人的绑定
                String updateSql = "UPDATE user SET bind_id = ? WHERE username = ? AND (bind_id IS NULL OR bind_id = '')";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, playerUuid);
                    psUpdate.setString(2, inputUsername);

                    int rows = psUpdate.executeUpdate();

                    if (rows > 0) {
                        syncMsg(player, "§a§l✔ 绑定成功！§7欢迎回来，§f" + inputUsername);
                    } else {
                        // 失败可能有两种情况：用户名不存在，或者该用户名已被别人绑定
                        syncMsg(player, "§c§l✘ 绑定失败！§7用户名不存在或该账号已被占用。");
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
                syncMsg(player, "§c§l✘ 错误！§7网络连接失败，请重试。");
            }
        });

        return true;
    }

    /**
     * 简单的辅助方法，用于快速切换回主线程发送消息
     */
    private void syncMsg(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
    }
}