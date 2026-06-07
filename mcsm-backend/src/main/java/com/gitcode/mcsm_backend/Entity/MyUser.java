package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户实体 - 实现 Spring Security UserDetails，存储用户账号、密码、角色等信息
 */
@TableName("user")
@Data
public class MyUser implements UserDetails {
    @TableId(type = IdType.AUTO)
    private Long id; // 建议用包装类 Long
    private String nickname;
    private String username;
    private String password;
    private Boolean baned; // 1:封禁, 0:正常
    @TableField(updateStrategy = FieldStrategy.NEVER)  // ✅ 更新时忽略此字段
    private String creatTime;
    private String email;
    private String bindId;
    private Long money;
    private Boolean banPublish;
    @TableField(exist = false)
    private List<SimpleGrantedAuthority> authorities;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
      
        return baned == null || !baned;
    }

}