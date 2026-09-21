package com.siam.system.modular.package_service.service;

import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;
import com.siam.system.modular.package_service.model.vo.WineTakeApplyVo;

import java.util.List;

/**
 * 存酒服务 · C-16 / C-01 存酒区 ＋ 取酒申请（方案 v1.1）
 *
 * <p>存酒四态口径（D-H，临期阈值 30 天）：
 * 已取完(2) &gt; 已过期(status=3 或 expire_date &lt; today) &gt; 将过期(≤30 天) &gt; 存续中
 *
 * <p>取酒单一模型：申请 → 取酒码 → 核销。商家侧能力＝「代发起取酒」，不做直接扣减。
 */
public interface WineStorageService {

    /**
     * 当前会员的存酒列表（含惰性过期判定：先批量把过期未置态的记录置 3）
     */
    List<WineStorageVo> listByMember(Integer memberId);

    /**
     * 存酒详情（校验归属当前会员）
     */
    WineStorageVo detailByMember(Integer memberId, Long id);

    /**
     * 商家侧存酒列表（W-18）
     *
     * <p>复用顾客侧的四态口径与排序；**按 shopId 限定本门店**。
     * 支持筛选：memberPhone（精确定位会员）。
     * 同时填充 memberName / memberPhone（脱敏），供列表「顾客」列展示。
     */
    List<WineStorageVo> listByShop(Integer shopId, WineStorageParam param);

    /**
     * 商家录入存酒：memberPhone 定位会员 → storage_date=今天，expire_date=今天+expireDays(默认 90)
     *
     * <p>注：方案 v1.1 起**登记阶段不再生成存酒码**，取酒码在「申请/代发起」时生成。
     *
     * @param shopId 商家所在门店id（由商家端会话推导）
     */
    WineStorage create(WineStorageParam param, Integer shopId);

    /* ==================== 取酒申请（方案 v1.1） ==================== */

    /**
     * 顾客自助申请取酒：校验归属/在存/未过期/余量/无待核销 → 生成 6 位取酒码（24h 有效）
     */
    WineTakeApplyVo applyTake(Integer memberId, WineStorageParam param);

    /**
     * 商家代发起取酒：不校验归属，其余校验同顾客自助
     */
    WineTakeApplyVo createApplyByShop(Integer operatorId, Integer shopId, WineStorageParam param);

    /**
     * 顾客查看自己的取酒申请（apply_time 倒序，全量）
     */
    List<WineTakeApplyVo> listMyApplies(Integer memberId);

    /**
     * 收银端待核销申请列表（按 shop_id 过滤，apply_time 升序）
     */
    List<WineTakeApplyVo> listPendingApplies(Integer shopId);

    /**
     * 按取酒码预览待核销详情（只读，供收银端「两段式核销」的第一步）
     */
    WineTakeApplyVo previewTake(Integer shopId, String takeCode);

    /**
     * 顾客取消自己的待核销申请（不限来源；已核销/已过期不可取消）
     */
    void cancelApply(Integer memberId, Long applyId);

    /**
     * 取酒核销（事务）：按取酒码 → 校验 → 原子扣减 → 写取酒记录 → 置申请已核销
     */
    WineTakeApplyVo confirmTake(Integer operatorId, String takeCode);
}
