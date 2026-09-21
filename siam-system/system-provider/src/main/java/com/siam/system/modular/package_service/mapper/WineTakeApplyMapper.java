package com.siam.system.modular.package_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_service.entity.WineTakeApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 取酒申请 Mapper · 方案 v1.1
 */
@Mapper
public interface WineTakeApplyMapper extends BaseMapper<WineTakeApply> {

    /**
     * 惰性过期：待核销(1) 且已过有效期 → 置 4（已过期）。
     * 由核销 / 列表 / 申请前调用，避免为过期行单独跑定时任务。
     */
    @Update("UPDATE tb_wine_take_apply SET status = 4, update_time = NOW() " +
            "WHERE status = 1 AND expire_time IS NOT NULL AND expire_time <= NOW()")
    int expireOutdated();
}
