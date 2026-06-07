package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.gitcode.mcsm_backend.Entity.MyUser;
import org.apache.ibatis.annotations.Update;


/**
 * 用户数据访问 - 用户 CRUD 及原子扣款等自定义操作
 */
@Mapper
public interface MyUserMapper extends BaseMapper<MyUser>{
   /**
    * 原子扣款操作
    * 通过 SQL 层的 money >= #{amount} 判断，彻底杜绝余额变负数的情况。
    * @param userId 用户自增 ID
    * @param amount 扣除的金额 (Long)
    * @return 影响行数，如果返回 0 表示余额不足或用户不存在
    */
   @Update("UPDATE user SET money = money - #{amount} " +
           "WHERE id = #{userId} AND money >= #{amount}")
   int decreaseMoney(Long userId, Long amount);

   /**
    * 增加余额 (用于卖家收到货款)
    * @param userId 用户自增 ID
    * @param amount 增加的金额 (Long)
    */
   @Update("UPDATE user SET money = money + #{amount} WHERE id = #{userId}")
   int increaseMoney(Long userId, Long amount);


   }





