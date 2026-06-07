package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.mapper.PlayerMapper;
import com.gitcode.mcsm_backend.util.NbtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家服务
 *
 * 负责玩家数据的增删改查，以及通过插件代理（ExternalApiClient）
 * 调用游戏服务器内插件接口进行远程操作。
 */
/**
 * 玩家服务 - 查询、修改、删除游戏玩家数据，管理玩家物品和金币
 */
@Service
@SuppressWarnings("unchecked")
public class PlayerService {

    @Autowired
    private ExternalApiClient apiClient;

    @Autowired
    private PlayerMapper playerMapper;

    /**
     * 获取所有玩家列表（从数据库）
     */
    public List<PlayerDTO> getAllPlayers() {
        return playerMapper.selectList(null);
    }

    /**
     * 同步玩家在线状态（由游戏服务器插件回调调用）
     *
     * 群组多服务器逻辑：
     * - isOnline=1（加入）：无条件更新为在线，记录所在 serverId
     * - isOnline=0（离开）：仅当 DB 中 serverId == 上报 serverId 时才置离线
     *   防止玩家从 A 服切到 B 服时，A 服的离线上报误标为离线
     */
    public Result<String> syncPlayerStatus(PlayerDTO dto) {
        try {
            PlayerDTO existing = playerMapper.selectByUuid(dto.getUuid());
            if (existing == null) {
                try {
                    playerMapper.insert(dto);
                } catch (Exception dupEx) {
                    doSyncUpdate(dto, playerMapper.selectByUuid(dto.getUuid()));
                }
            } else {
                doSyncUpdate(dto, existing);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.successMsg("同步成功");
    }

    /**
     * 执行同步更新：isOnline=1 无条件上线；isOnline=0 仅 serverId 匹配时下线
     */
    private void doSyncUpdate(PlayerDTO dto, PlayerDTO existing) {
        if (existing == null) return;
        if (dto.getIsOnline() == 1) {
            playerMapper.update(null, new LambdaUpdateWrapper<PlayerDTO>()
                    .set(PlayerDTO::getIsOnline, 1)
                    .set(PlayerDTO::getNickname, dto.getNickname())
                    .set(PlayerDTO::getServerId, dto.getServerId())
                    .set(PlayerDTO::getLastPlayed, dto.getLastPlayed())
                    .eq(PlayerDTO::getUuid, dto.getUuid())
            );
        } else {
            String dbServerId = existing.getServerId();
            String reportServerId = dto.getServerId();
            if (dbServerId != null && dbServerId.equals(reportServerId)) {
                playerMapper.update(null, new LambdaUpdateWrapper<PlayerDTO>()
                        .set(PlayerDTO::getIsOnline, 0)
                        .set(PlayerDTO::getServerId, (String) null)
                        .set(PlayerDTO::getLastPlayed, dto.getLastPlayed())
                        .eq(PlayerDTO::getUuid, dto.getUuid())
                );
            }
        }
    }

    /**
     * 远程踢出玩家
     * @param uuid 玩家 UUID
     */
    public Result<String> kickPlayerRemote(String uuid) {
        return apiClient.sendToPlugin("default", "kickPlayer", Map.of("uuid", uuid));
    }

    /**
     * 远程封禁玩家
     * @param uuid 玩家 UUID
     */
    public Result<String> banPlayerRemote(String uuid) {
        return apiClient.sendToPlugin("default", "banPlayer", Map.of("uuid", uuid));
    }

    /**
     * 获取玩家背包数据（NBT 格式转 JSON）
     * @param uuid 玩家 UUID
     */
    @SuppressWarnings("unchecked")
    public Result<Object> getPlayerInventoryRemote(String uuid) {
        Result<Object> result = (Result<Object>) (Result<?>) apiClient.sendToPlugin("default", "getInventory", Map.of("uuid", uuid));

        if (result != null && result.getCode() == 2000 && result.getData() != null) {
            try {
                String rawData = result.getData().toString();

                // 清洗 NBT 格式 → 标准 JSON
                String cleanData = cleanNbtJson(rawData);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(cleanData);
                result.setData(jsonNode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 清洗 NBT/SNBT 格式为标准 JSON
     */
    private String cleanNbtJson(String raw) {
        return NbtUtils.clean(raw);
    }


    /**
     * 按物品名称修改数量（远程调用插件）
     */
    public Result<String> updateItemCountRemote(String uuid, String itemName, Integer newCount) {
        return apiClient.sendToPlugin("default", "updateItemCount",
                Map.of("uuid", uuid, "itemName", itemName, "newCount", newCount));
    }

    /**
     * 按槽位修改物品数量（远程调用插件）
     * @param uuid     玩家 UUID
     * @param slot     背包槽位 (0-35)
     * @param newCount 新数量（为 0 时删除物品）
     */
    public Result<String> updateItemBySlotRemote(String uuid, int slot, Integer newCount) {
        return apiClient.sendToPlugin("default", "updateItemSlot",
                Map.of("uuid", uuid, "slot", String.valueOf(slot), "newCount", String.valueOf(newCount)));
    }

    /**
     * 暂存物品到数据库（通过插件接口）
     * 用于市场下架、购买等场景，物品暂存后玩家可在游戏内用 /claim 领取
     */
    public Result<String> addPendingItem(String playerUuid, String itemKey, int amount,
                                         String displayName, String nbtData,
                                         String source, String sourceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("playerUuid", playerUuid);
        body.put("itemKey", itemKey);
        body.put("amount", amount);
        body.put("displayName", displayName);
        body.put("nbtData", nbtData);
        body.put("source", source);
        body.put("sourceId", sourceId);

        return apiClient.sendToPlugin("default", "addPendingChange", body);
    }

    /**
     * 远程解封玩家
     * @param uuid 玩家 UUID
     */
    public Result<String> unbanPlayerRemote(String uuid) {
        return apiClient.sendToPlugin("default", "unbanPlayer", Map.of("uuid", uuid));
    }

    /**
     * 远程检查玩家封禁状态
     * @param uuid 玩家 UUID
     */
    public Result<Boolean> isPlayerBannedRemote(String uuid) {
        return apiClient.sendToPlugin("default", "isBanned", Map.of("uuid", uuid));
    }

}