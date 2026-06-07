package com.gitcode.mcsmServerBridge.command;

import com.gitcode.mcsmServerBridge.McsmBridge;
import com.gitcode.mcsmServerBridge.Service.PendingItemService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand implements CommandExecutor {

    private final McsmBridge plugin;
    private final PendingItemService pendingItemService;

    public ClaimCommand(McsmBridge plugin, PendingItemService pendingItemService) {
        this.plugin = plugin;
        this.pendingItemService = pendingItemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此指令！");
            return true;
        }

        Player player = (Player) sender;

        player.sendMessage("§6正在查询待领取物品...");

        // 异步查询数据库
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = pendingItemService.claimAllItems(player);

            // 切回主线程发送消息
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (count > 0) {
                    player.sendMessage("§a§l✔ 领取完成！§7共领取 " + count + " 个物品。");
                    player.sendMessage("§7如果背包满了，物品会掉落在地上。");
                } else {
                    player.sendMessage("§e§l⚠ §7你当前没有待领取的物品。");
                }
            });
        });

        return true;
    }
}