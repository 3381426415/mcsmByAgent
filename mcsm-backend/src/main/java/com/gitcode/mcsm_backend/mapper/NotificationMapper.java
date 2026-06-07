package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知数据访问 - 通知消息的 CRUD 操作
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}