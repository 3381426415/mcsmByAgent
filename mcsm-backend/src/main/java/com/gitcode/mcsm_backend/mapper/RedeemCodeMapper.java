package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.RedeemCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兑换码数据访问 - 兑换码的 CRUD 操作
 */
@Mapper
public interface RedeemCodeMapper extends BaseMapper<RedeemCode> {
}