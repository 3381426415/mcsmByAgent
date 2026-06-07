package com.gitcode.mcsm_backend.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.OrderRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 订单记录数据访问 - 订单 CRUD 及按玩家查询买入/卖出记录
 */
@Mapper
public interface OrderRecordMapper extends BaseMapper<OrderRecord> {

    /**
     * 查询玩家的买入记录
     */
    @Select("SELECT * FROM mcsm_order_record WHERE buyer_id = #{uuid} ORDER BY complete_time DESC")
    List<OrderRecord> getPurchaseHistory(String uuid);

    /**
     * 查询玩家的卖出记录
     */
    @Select("SELECT * FROM mcsm_order_record WHERE seller_id = #{uuid} ORDER BY complete_time DESC")
    List<OrderRecord> getSalesHistory(String uuid);
}