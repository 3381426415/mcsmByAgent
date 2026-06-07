package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.Announcement;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理接口 - 发布、编辑、删除、查询系统公告
 */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 分页查询公告（管理端）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin:announcement')")
    public Result<Page<Announcement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type) {
        Page<Announcement> data = announcementService.getAnnouncementPage(page, size, type);
        return Result.success("获取成功", data);
    }

    /**
     * 获取最新网站公告（首页展示）
     */
    @GetMapping("/latest")
    public Result<List<Announcement>> latest() {
        List<Announcement> list = announcementService.getLatestWebAnnouncements();
        return Result.success("获取成功", list);
    }


    /**
     * 发布公告
     */
    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('admin:announcement')")
    @LogRecord(
            module = "公告管理",
            action = "发布公告",
            description = "发布公告：#{#announcement.title}"
    )
    public Result<String> publish(@RequestBody Announcement announcement) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return announcementService.publishAnnouncement(announcement, currentUser.getId());
    }

    /**
     * 保存草稿
     */
    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('admin:announcement')")
    @LogRecord(
            module = "公告管理",
            action = "保存草稿",
            description = "保存公告草稿：#{#announcement.title}"
    )
    public Result<String> saveDraft(@RequestBody Announcement announcement) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return announcementService.saveDraft(announcement, currentUser.getId());
    }

    /**
     * 更新公告（保留编辑功能）
     */
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('admin:announcement')")
    @LogRecord(
            module = "公告管理",
            action = "更新公告",
            description = "更新公告：#{#announcement.title}"
    )
    public Result<String> update(@RequestBody Announcement announcement) {
        return announcementService.updateAnnouncement(announcement);
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('admin:announcement')")
    @LogRecord(
            module = "公告管理",
            action = "删除公告",
            description = "删除公告，ID：#{#id}"
    )
    public Result<String> delete(@PathVariable Long id) {
        return announcementService.deleteAnnouncement(id);
    }
}