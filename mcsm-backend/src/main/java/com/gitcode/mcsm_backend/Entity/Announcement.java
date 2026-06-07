package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告实体 - 存储系统公告内容、发布者和有效期
 */
@Data
@TableName("mcsm_announcement")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Integer type;          // 1-网站，2-游戏，3-双端
    private Integer isPublished;   // 0-草稿，1-已发布
    private String serverIds;      // 生效服务器ID列表，JSON数组，如 ["s1","s2"]
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;

    @TableField(exist = false)
    private String createByName;   // 发布人姓名
}