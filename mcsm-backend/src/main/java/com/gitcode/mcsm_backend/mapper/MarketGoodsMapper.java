package com.gitcode.mcsm_backend.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.MarketGoods;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 市场商品数据访问 - 商品 CRUD 及全文搜索
 */
@Mapper
public interface MarketGoodsMapper extends BaseMapper<MarketGoods> {

    /**
     * 高级模糊查询：使用 MySQL 全文索引 (MATCH AGAINST)
     * 对应你之前在数据库中创建的 FULLTEXT INDEX ... WITH PARSER ngram
     * @param keyword 玩家输入的搜索词
     * @return 匹配的待售商品列表
     */
    @Select("SELECT * FROM mcsm_market_goods " +
            "WHERE is_deleted = 0 AND status = 0 " +
            "AND MATCH(display_name) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY creat_time DESC")
    List<MarketGoods> fullTextSearch(String keyword);

    /**
     * 悲观锁查询：用于购买逻辑，防止并发购买
     * 在事务中使用 selectByIdForUpdate 可以锁住该行直至事务结束
     */
    @Select("SELECT * FROM mcsm_market_goods WHERE id = #{id} FOR UPDATE")
    MarketGoods selectByIdForUpdate(String id);
}