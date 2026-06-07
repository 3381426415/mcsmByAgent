package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体 - 定义系统角色（如管理员、普通用户），用于权限控制
 */
@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.AUTO)
    private long roleId;
    private String roleName;
    private String roleDescription;
}
