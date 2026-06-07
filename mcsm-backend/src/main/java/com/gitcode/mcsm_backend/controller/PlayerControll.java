package com.gitcode.mcsm_backend.controller;


import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import com.gitcode.mcsm_backend.annotation.AgentTool;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PlayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 玩家管理接口 - 查询、修改、删除游戏玩家数据
 */
@Slf4j
@RestController
@RequestMapping("/api/player")
@PreAuthorize("hasAuthority('admin:player')")
public class PlayerControll {

    @Autowired
    private PlayerService playerService;

    /**
     * 获取所有玩家数据
     * 前端可以根据 isOnline 字段（1在线，0不在线）进行分类显示
     * @return 包含玩家列表的标准返回体
     */
    @AgentTool(description = "[后端·自动化流程] 从数据库查询所有玩家信息（含离线玩家），返回昵称、UUID、在线状态、余额、最后在线时间。数据来源：后端数据库。")
    @GetMapping("/all")
    public Result<List<PlayerDTO>> listAllPlayers() {
        List<PlayerDTO> players = playerService.getAllPlayers();
        if (players != null) {
            return Result.success("获取玩家列表成功", players);
        }
        return Result.error("获取玩家列表失败");
    }


    /**
     * 远程踢出玩家
     * @param uuid 玩家的 UUID
     */
    @PostMapping("/kick")
    @LogRecord(
            module = "玩家管理",
            action = "踢出玩家",
            description = "踢出玩家，UUID：#{#uuid}"
    )

    public Result<String> kickPlayer(@RequestParam String uuid) {
        // 调用 Service 层中封装的 restTemplate 逻辑
        return playerService.kickPlayerRemote(uuid);
    }

    /**
     * 远程封禁玩家
     * @param uuid 玩家的 UUID
     */

    @LogRecord(
            module = "玩家管理",
            action = "封禁玩家",
            description = "封禁玩家，UUID：#{#uuid}"
    )
    @PostMapping("/ban")
    public Result<String> banPlayer(@RequestParam String uuid) {
        Result<String> result = playerService.banPlayerRemote(uuid);
        log.info("[DEBUG] banPlayer 返回: {}", result);
        return result;
    }




    /**
     * 远程解封玩家
     * @param uuid 玩家的 UUID
     */
    @LogRecord(
            module = "玩家管理",
            action = "解封玩家",
            description = "解封玩家，UUID：#{#uuid}"
    )
    @PostMapping("/unban")
    public Result<String> unbanPlayer(@RequestParam String uuid) {
        return playerService.unbanPlayerRemote(uuid);
    }

    /**
     * 检查玩家封禁状态
     * @param uuid 玩家的 UUID
     */
    @GetMapping("/is-banned")
    public Result<Boolean> isPlayerBanned(@RequestParam String uuid) {
        return playerService.isPlayerBannedRemote(uuid);
    }


// --- 新增 NBT 管理接口 ---

    /**
     * 获取玩家背包数据 (NBT 格式转 JSON)
     * 用于在网页展示玩家拥有的物品
     */
    @GetMapping("/inventory")
    public Result<Object> getInventory(@RequestParam String uuid) {
        return playerService.getPlayerInventoryRemote(uuid);
    }



    /**
     * 接口：按槽位修改玩家背包物品
     * 前端 Vue 点击背包格子时调用此接口
     */
    @PostMapping("/inventory/updateBySlot")
    public Result<String> updateBySlot(
            @RequestParam String uuid,
            @RequestParam int slot,
            @RequestParam Integer newCount) {
        return playerService.updateItemBySlotRemote(uuid, slot, newCount);
    }



}
