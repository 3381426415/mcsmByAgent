package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告数据访问 - 系统公告的 CRUD 操作
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}