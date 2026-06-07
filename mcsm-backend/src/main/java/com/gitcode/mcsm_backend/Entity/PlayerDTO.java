package com.gitcode.mcsm_backend.Entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gameplayer")
/**
 * 游戏玩家实体 - 存储 Minecraft 服务器中的玩家数据（UUID、名称、金币等）
 */
public class PlayerDTO {
    private long id;
    private String uuid;
    private String nickname;     // 原始名称（带点）
    private int isOnline;      // 是否在线，1在线，0不在线
    private long money;
    private long lastPlayed;  // 最后在线时间戳
    private String serverId;  // 所在服务器实例ID


}


