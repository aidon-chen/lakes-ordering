package com.siam.system.modular.package_goods.service.internal;

import java.util.Map;
import java.util.Set;

/**
 * 营业时段服务（叠加设计）
 *
 * <p>职责：把"什么钟点算白天/夜间"（门店级配置）与"哪些分类/商品属于哪个时段"（分类级 + 单品级）
 * 合成一个判定入口，供三处调用：
 * <ol>
 *   <li>{@code MenuServiceImpl.getListJoinGoods} —— 顾客端菜单按当前时段过滤（展示口径）；</li>
 *   <li>{@code OrderServiceImpl.insert} —— 下单硬校验（强制口径，挡住"页面没刷新就下单"）；</li>
 *   <li>{@code CommonServiceImpl.selectIsOperatingOfShop} —— 门店营业状态派生（打烊空档=休息中）。</li>
 * </ol>
 *
 * <p><b>为什么不用定时任务</b>：本工程的 {@code @Scheduled} 被 {@code @Profile("!mvp")} 关掉，
 * 本地与云托管都不启动（详见 MEMORY）。因此时段一律"读时判定"，不做到点改库。
 * <b>不要</b>把时段编码进 {@code tb_menu.is_can_order} 或 {@code tb_goods.status}：
 * 前者是人工"可否点单"开关、后者是人工上下架/沽清，混用会互相覆盖。
 */
public interface SalePeriodService {

    /** 打烊空档：不在任何时段窗口内 */
    String PERIOD_CLOSED = "closed";

    /** 全时段（分类/单品显式标记为"不受时段限制"） */
    String PERIOD_ALL = "all";

    /**
     * 当前时段码；未启用时段功能时返回 {@code null}（调用方据此完全跳过过滤，行为退回现状）。
     * 已启用但此刻不在任何窗口内 → 返回 {@link #PERIOD_CLOSED}。
     */
    String currentPeriod(Integer shopId);

    /**
     * 某个分类/单品的时段配置是否命中当前时段。
     * 规则：{@code currentPeriod == null}（未启用）→ 命中；配置为空/NULL/{@code all} → 命中（全天可售）；
     * {@code currentPeriod == closed} → 不命中（打烊空档一律不可售）。
     */
    boolean matchPeriod(String periodValue, String currentPeriod);

    /** 该商品此刻是否可售（单品覆盖优先，其次分类，都未配置=可售）。供下单校验调用。 */
    boolean isGoodsSellable(Integer shopId, Integer goodsId);

    /** 分类级时段：menuId → sale_period（NULL=全天） */
    Map<Integer, String> menuPeriods(Integer shopId);

    /** 单品级覆盖：goodsId → 允许的时段集合（未被覆盖的商品不出现在 Map 中） */
    Map<Integer, Set<String>> goodsOverrides(Integer shopId);
}
