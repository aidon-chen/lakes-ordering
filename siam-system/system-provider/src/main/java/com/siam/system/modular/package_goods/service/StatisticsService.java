package com.siam.system.modular.package_goods.service;

import com.siam.system.modular.package_goods.model.param.StatisticsParam;

import java.util.Map;

/**
 *  暹罗
 */
public interface StatisticsService {

    /**
     * 今日数据实时统计 - 管理端
     *
     * @author 暹罗
     */
    Map todayStatisticByAdmin(StatisticsParam param);

    /**
     * 今日数据实时统计 - 商家端
     *
     * @author 暹罗
     */
    Map todayStatisticByMerchant(StatisticsParam param);

    /**
     * A-07 营收看板 - 营收统计（日/周/月维度 + 核心指标 + 趋势 + 支付方式分布 + 订单类型分布 + 商品TOP10）
     *
     * @author 暹罗
     */
    Map revenueStatistic(StatisticsParam param);
}