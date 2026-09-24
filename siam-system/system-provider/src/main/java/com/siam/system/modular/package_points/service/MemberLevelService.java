package com.siam.system.modular.package_points.service;

import java.util.Map;

/**
 * 会员等级服务 · 等级 = f(累计积分)，查阈值表实时推导，不在 member 主表冗余。
 * 优惠券「会员类型」的等级门槛校验（effective_level >= coupon.min_level）走本服务。
 */
public interface MemberLevelService {

    /**
     * 会员等级信息
     * 返回：totalPoints 累计积分 / level 当前等级 / levelName 等级名 /
     *       nextLevelName 下一等级名(null=已满级) / nextThreshold 下一档阈值 / gap 距下一档差值 /
     *       levels 全部档位列表
     */
    Map<String, Object> getLevelInfo(Integer memberId);
}
