package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("role_permission")

/**
 * 角色-权限关联实体 - 维护角色与权限的多对多关系
 */
public class RolePermission {

    //不需要自增注解！！
    private long roleId;
    private long permId;

}
