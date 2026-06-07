package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.Announcement;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.mapper.AnnouncementMapper;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 公告服务 - 公告的发布、编辑、删除和分页查询
 */
@Slf4j
@Service
public class AnnouncementService extends ServiceImpl<AnnouncementMapper, Announcement> {

    @Autowired
    private MyUserMapper myUserMapper;

    @Autowired
    private PluginConnectionManager pluginConnectionManager;

    /**
     * 分页查询公告
     */
    public Page<Announcement> getAnnouncementPage(int page, int size, Integer type) {
        Page<Announcement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Announcement::getType, type);
        }
        wrapper.orderByDesc(Announcement::getCreateTime);

        Page<Announcement> result = this.page(pageParam, wrapper);

        // 填充发布人姓名
        for (Announcement a : result.getRecords()) {
            if (a.getCreateBy() != null) {
                MyUser user = myUserMapper.selectById(a.getCreateBy());
                a.setCreateByName(user != null ? user.getUsername() : "未知");
            }
        }

        return result;
    }

    /**
     * 获取最新网站公告（供前端首页展示）
     */
    public List<Announcement> getLatestWebAnnouncements() {
        return this.lambdaQuery()
                .eq(Announcement::getIsPublished, 1)
                .in(Announcement::getType, 1, 3)  // 网站或双端
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 5")
                .list();
    }

    /**
     * 发布公告（同时推送到游戏）
     */
    public Result<String> publishAnnouncement(Announcement announcement, Long userId) {
        announcement.setCreateBy(userId);
        announcement.setIsPublished(1);   // ← 1 = 已发布

        boolean saved;
        if (announcement.getId() != null) {
            // 已有 id → 草稿转正，或者编辑已发布的公告
            saved = this.updateById(announcement);
        } else {
            // 无 id → 全新发布
            saved = this.save(announcement);
        }

        if (!saved) {
            return Result.error("发布失败");
        }

        // 游戏公告推送（保持原逻辑）
        if (announcement.getType() == 2 || announcement.getType() == 3) {
            pushToGame(announcement);
        }

        return Result.successMsg("发布成功");
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过 RCON 推送公告到游戏
     * 如果公告指定了 serverIds，则推送到对应服务器；否则推送到默认服务器
     */
    private void pushToGame(Announcement announcement) {
        List<String> serverIds = parseServerIds(announcement.getServerIds());

        String titleCmd = String.format(
                "tellraw @a {\"text\":\"[公告] %s\",\"color\":\"gold\"}",
                announcement.getTitle().replace("\\", "\\\\").replace("\"", "\\\"")
        );

        String contentCmd = null;
        if (announcement.getContent() != null && !announcement.getContent().isEmpty()) {
            contentCmd = String.format(
                    "tellraw @a {\"text\":\"%s\",\"color\":\"yellow\"}",
                    announcement.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
            );
        }

        if (serverIds.isEmpty()) {
            // 未指定服务器，推送到默认服务器
            pushCommand(titleCmd, contentCmd, "default");
        } else {
            for (String serverId : serverIds) {
                pushCommand(titleCmd, contentCmd, serverId);
            }
        }
    }

    private void pushCommand(String titleCmd, String contentCmd, String serverId) {
        try {
            pluginConnectionManager.sendCommand(serverId, "executeCommand", Map.of("command", titleCmd));
            if (contentCmd != null) {
                pluginConnectionManager.sendCommand(serverId, "executeCommand", Map.of("command", contentCmd));
            }
        } catch (Exception e) {
            log.error("插件推送公告失败 [{}]: {}", serverId, e.getMessage());
        }
    }

    private List<String> parseServerIds(String serverIdsJson) {
        if (serverIdsJson == null || serverIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(serverIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析 serverIds 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 更新公告
     */
    public Result<String> updateAnnouncement(Announcement announcement) {
        boolean updated = this.updateById(announcement);
        return updated ? Result.successMsg("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除公告
     */
    public Result<String> deleteAnnouncement(Long id) {
        boolean deleted = this.removeById(id);
        return deleted ? Result.successMsg("删除成功") : Result.error("删除失败");
    }

    /**
     * 保存草稿（纯新增，因为草稿不应该有旧 id）
     */
    public Result<String> saveDraft(Announcement announcement, Long userId) {
        announcement.setCreateBy(userId);
        announcement.setIsPublished(0);   // ← 0 = 草稿
        boolean saved = this.save(announcement);
        return saved ? Result.successMsg("草稿保存成功") : Result.error("保存失败");
    }
}