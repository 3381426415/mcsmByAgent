package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.Common.Result;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class OnlinePlayerEditService {

    private final Gson gson = new Gson();

    /**
     * 在线扣除玩家指定槽位的物品数量
     * @param uuid 玩家UUID
     * @param slot 槽位
     * @param reduceCount 要扣除的数量
     */
    public Result<String> updateItemBySlotOnline(UUID uuid, int slot, int reduceCount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return Result.error("Player offline");

        ItemStack item = player.getInventory().getItem(slot);

        if (item == null || item.getType() == Material.AIR) {
            return Result.error("Slot is empty");
        }

        int currentCount = item.getAmount();
        int newCount = currentCount - reduceCount;

        if (newCount <= 0) {
            player.getInventory().setItem(slot, null);
            return Result.successMsg("Item removed");
        } else {
            item.setAmount(newCount);
            return Result.successMsg("Item count updated");
        }
    }

    /**
     * 获取在线玩家的背包 JSON
     */
    public Result<String> getOnlineInventoryJson(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return Result.error("Player offline");

        JsonArray inventoryArray = new JsonArray();

        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("Slot", slot);
                itemJson.addProperty("id", item.getType().getKey().toString());
                itemJson.addProperty("Count", item.getAmount());

                if (item.hasItemMeta()) {
                    JsonObject tag = new JsonObject();
                    ItemMeta meta = item.getItemMeta();

                    if (meta.hasDisplayName()) {
                        JsonObject display = new JsonObject();
                        display.addProperty("Name", meta.getDisplayName());
                        tag.add("display", display);
                    }

                    if (tag.size() > 0) {
                        itemJson.add("tag", tag);
                    }
                }

                inventoryArray.add(itemJson);
            }
        }

        return Result.success("Query success", inventoryArray.toString());
    }
}