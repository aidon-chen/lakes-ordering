package com.siam.system.modular.package_dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_dining.entity.DiningTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 台位 Mapper（含并发安全原子抢台/释台）
 */
@Mapper
public interface DiningTableMapper extends BaseMapper<DiningTable> {

    /** 原子抢台：空闲(1)→占用(2)，返回影响行数=1 才成功（并发兜底） */
    @Update("UPDATE tb_dining_table SET status = 2, update_time = NOW() " +
            "WHERE id = #{id} AND shop_id = #{shopId} AND status = 1")
    int occupyById(@Param("id") Long id, @Param("shopId") Integer shopId);

    /** 原子释台：占用(2)→空闲(1) */
    @Update("UPDATE tb_dining_table SET status = 1, update_time = NOW() " +
            "WHERE id = #{id} AND shop_id = #{shopId} AND status = 2")
    int releaseById(@Param("id") Long id, @Param("shopId") Integer shopId);
}
