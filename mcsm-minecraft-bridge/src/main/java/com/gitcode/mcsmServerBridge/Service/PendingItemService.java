package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.Common.Result;
import com.gitcode.mcsmServerBridge.Entity.PendingItem;
import com.gitcode.mcsmServerBridge.Manager.DatabaseManager;
import com.gitcode.mcsmServerBridge.McsmBridge;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PendingItemService {

    private final McsmBridge plugin;
    private final DatabaseManager dbManager;
    private final Gson gson = new Gson();

    public PendingItemService(McsmBridge plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    /**
     * 添加暂存物品（供 HTTP API 调用）
     */
    public Result<String> addPendingItem(String playerUuid, String itemKey, int amount,
                                         String displayName, String nbtData,
                                         String source, String sourceId) {
        String sql = "INSERT INTO mcsm_pending_items " +
                "(player_uuid, item_key, amount, display_name, nbt_data, source, source_id, create_time, claimed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerUuid);
            ps.setString(2, itemKey);
            ps.setInt(3, amount);
            ps.setString(4, displayName);
            ps.setString(5, nbtData);
            ps.setString(6, source);
            ps.setString(7, sourceId);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                plugin.getLogger().info("Added pending item for player " + playerUuid + ": " + displayName + " x" + amount);
                return Result.successMsg("Item stored");
            } else {
                return Result.error("Store failed");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add pending item: " + e.getMessage());
            return Result.error("Database error: " + e.getMessage());
        }
    }

    /**
     * 获取玩家的所有未领取物品
     */
    public List<PendingItem> getUnclaimedItems(String playerUuid) {
        List<PendingItem> items = new ArrayList<>();
        String sql = "SELECT * FROM mcsm_pending_items WHERE player_uuid = ? AND claimed = 0 ORDER BY create_time";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PendingItem item = new PendingItem();
                    item.setId(rs.getLong("id"));
                    item.setPlayerUuid(rs.getString("player_uuid"));
                    item.setItemKey(rs.getString("item_key"));
                    item.setAmount(rs.getInt("amount"));
                    item.setDisplayName(rs.getString("display_name"));
                    item.setNbtData(rs.getString("nbt_data"));
                    item.setSource(rs.getString("source"));
                    item.setSourceId(rs.getString("source_id"));
                    item.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    item.setClaimed(rs.getBoolean("claimed"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to query pending items: " + e.getMessage());
        }
        return items;
    }

    /**
     * 标记物品为已领取
     */
    public void markAsClaimed(Set<Long> ids) {
        if (ids.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "UPDATE mcsm_pending_items SET claimed = 1, claim_time = ? WHERE id IN (" + placeholders + ")";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int idx = 2;
            for (Long id : ids) {
                ps.setLong(idx++, id);
            }
            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update claim status: " + e.getMessage());
        }
    }

    /**
     * 根据 NBT 数据创建 ItemStack
     */
    public ItemStack createItemStack(PendingItem item) {
        Material material = Material.getMaterial(item.getItemKey().toUpperCase());
        if (material == null) {
            String key = item.getItemKey();
            if (key.contains(":")) {
                key = key.split(":")[1];
            }
            material = Material.getMaterial(key.toUpperCase());
        }

        if (material == null) {
            plugin.getLogger().warning("Unknown item type: " + item.getItemKey() + ", using STONE as fallback");
            material = Material.STONE;
        }

        ItemStack itemStack = new ItemStack(material, item.getAmount());

        if (item.getNbtData() != null && !item.getNbtData().isEmpty() && !"{}".equals(item.getNbtData())) {
            try {
                JsonObject nbtJson = JsonParser.parseString(item.getNbtData()).getAsJsonObject();
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    if (nbtJson.has("display")) {
                        JsonObject display = nbtJson.getAsJsonObject("display");
                        if (display.has("Name")) {
                            String name = display.get("Name").getAsString();
                            meta.setDisplayName(name.replaceAll("§", "&"));
                        }
                    }
                    itemStack.setItemMeta(meta);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("NBT restore failed: " + e.getMessage() + ", using base item");
            }
        }

        if ((item.getNbtData() == null || item.getNbtData().isEmpty() || "{}".equals(item.getNbtData()))
                && item.getDisplayName() != null && !item.getDisplayName().isEmpty()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§r" + item.getDisplayName());
                itemStack.setItemMeta(meta);
            }
        }

        return itemStack;
    }

    /**
     * 给玩家发放所有未领取物品
     */
    public int claimAllItems(Player player) {
        String playerUuid = player.getUniqueId().toString();
        List<PendingItem> pendingItems = getUnclaimedItems(playerUuid);

        if (pendingItems.isEmpty()) {
            return 0;
        }

        int totalAmount = 0;
        Set<Long> claimedIds = new HashSet<>();

        for (PendingItem item : pendingItems) {
            ItemStack itemStack = createItemStack(item);

            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);

            if (!leftover.isEmpty()) {
                for (ItemStack leftoverItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                }
            }

            totalAmount += item.getAmount();
            claimedIds.add(item.getId());

            player.sendMessage("§a✔ 领取: §f" + (item.getDisplayName() != null ? item.getDisplayName() : item.getItemKey())
                    + " §7x" + item.getAmount());
        }

        markAsClaimed(claimedIds);
        plugin.getLogger().info("Player " + player.getName() + " claimed " + pendingItems.size() + " item types, total " + totalAmount);

        return totalAmount;
    }
}