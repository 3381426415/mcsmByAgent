package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.PlayerDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 玩家数据访问 - 按 UUID 查询玩家、更新金币等操作
 */
@Mapper
public interface PlayerMapper extends BaseMapper<PlayerDTO> {

    @Select("SELECT * FROM gameplayer WHERE uuid = #{uuid}")
    PlayerDTO selectByUuid(String uuid);


}
