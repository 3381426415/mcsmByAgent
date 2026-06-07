package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role")
/**
 * 用户-角色关联实体 - 维护用户与角色的多对多关系
 */
public class UserRole {
    private long userId;
    private long roleId;
}
