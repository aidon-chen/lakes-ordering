package com.siam.system.modular.package_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_service.entity.WineStorage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 存酒 Mapper · C-16
 *
 * 含并发安全的原子取酒（防超扣）。
 */
@Mapper
public interface WineStorageMapper extends BaseMapper<WineStorage> {

    /**
     * 原子取酒：剩余瓶数充足（status=1 且在存）→ 扣减；扣到 0 自动置 status=2（已取完）。
     * 返回影响行数 = 1 才算成功，并发兜底靠 DB 行锁，不用「先查后改」。
     *
     * SET 子句刻意把 status 放在 remain_count 之前：MySQL 的赋值是从左到右，
     * 这样 status 表达式里的 remain_count 取到的仍是**旧值**，语义确定。
     */
    @Update("UPDATE tb_wine_storage SET " +
            "  status = CASE WHEN remain_count - #{takeCount} <= 0 THEN 2 ELSE status END, " +
            "  remain_count = remain_count - #{takeCount}, " +
            "  update_time = NOW() " +
            "WHERE storage_code = #{storageCode} AND status = 1 AND remain_count >= #{takeCount}")
    int atomicTake(@Param("storageCode") String storageCode, @Param("takeCount") Integer takeCount);

    /** 惰性过期：已过有效期但仍标记为在存(1)的记录统一置为已过期(3) */
    @Update("UPDATE tb_wine_storage SET status = 3, update_time = NOW() " +
            "WHERE status = 1 AND expire_date < CURDATE()")
    int expireOutdated();
}
