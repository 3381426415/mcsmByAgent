package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限实体 - 定义系统权限项（如 server:manage、user:edit），关联到角色
 */
@Data
@TableName("permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private int permId;
    private String name;
    private String description;
}
