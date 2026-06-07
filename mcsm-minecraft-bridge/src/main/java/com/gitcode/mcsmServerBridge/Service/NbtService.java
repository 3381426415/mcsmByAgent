package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.Common.Result;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import java.io.File;
import java.util.UUID;

public class NbtService {
    private final File dataFolder;
    private final OnlinePlayerEditService onlineService;

    public NbtService(File worldFolder) {
        this.dataFolder = new File(worldFolder, "playerdata");
        this.onlineService = new OnlinePlayerEditService();
    }

    /**
     * 核心方法：按槽位修改物品数量（自动识别在线/离线）
     */
    public Result<String> updateItemBySlot(UUID uuid, int slot, int reduceCount) {
        if (Bukkit.getPlayer(uuid) != null) {
            return onlineService.updateItemBySlotOnline(uuid, slot, reduceCount);
        }

        try {
            File file = getPlayerDataFile(uuid);
            if (file == null) return Result.error("Save file not found");

            NBTFile nbt = new NBTFile(file);
            NBTCompoundList inventory = nbt.getCompoundList("Inventory");

            boolean found = false;
            java.util.Iterator<ReadWriteNBT> it = inventory.iterator();
            while (it.hasNext()) {
                ReadWriteNBT item = it.next();
                if (item.getByte("Slot") == (byte) slot) {
                    int currentCount = item.getInteger("Count");
                    int newCount = currentCount - reduceCount;

                    if (newCount <= 0) {
                        it.remove();
                    } else {
                        item.setInteger("Count", newCount);
                    }
                    found = true;
                    break;
                }
            }

            if (found) {
                nbt.save();
                return Result.successMsg("Item deducted");
            }
            return Result.error("Item not found in slot");
        } catch (Exception e) {
            return Result.error("Offline operation failed: " + e.getMessage());
        }
    }

    /**
     * 获取背包数据（自动识别在线/离线）
     */
    public Result<String> getInventoryJson(UUID uuid) {
        // 如果在线，返回实时背包数据
        if (Bukkit.getPlayer(uuid) != null) {
            return onlineService.getOnlineInventoryJson(uuid);
        }

        // 离线走 NBT 文件
        try {
            File file = getPlayerDataFile(uuid);
            if (file == null) return Result.error("Save file not found");

            NBTFile nbt = new NBTFile(file);
            String json = nbt.getCompoundList("Inventory").toString();
            return Result.success("Query success", json);
        } catch (Exception e) {
            return Result.error("Failed to get offline inventory: " + e.getMessage());
        }
    }


    private File getPlayerDataFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".dat");
        return file.exists() ? file : null;
    }
}