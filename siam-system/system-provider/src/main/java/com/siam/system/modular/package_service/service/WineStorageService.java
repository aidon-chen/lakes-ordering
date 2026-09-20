package com.siam.system.modular.package_service.service;

import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;

import java.util.List;

/**
 * 存酒服务 · C-16 / C-01 存酒区
 *
 * 状态四态口径（D-H，临期阈值 30 天）：
 *   已取完(2) > 已过期(status=3 或 expire_date < today) > 将过期(≤30 天) > 存续中
 * 排序：存续中 → 将过期 → 已过期 → 已取完，同组内 expire_date 升序。
 */
public interface WineStorageService {

    /**
     * 当前会员的存酒列表（含惰性过期判定：先批量把过期未置态的记录置 3）
     */
    List<WineStorageVo> listByMember(Integer memberId);

    /**
     * 存酒详情（校验归属当前会员；本批前端不消费，保留）
     */
    WineStorageVo detailByMember(Integer memberId, Long id);

    /**
     * 商家侧存酒列表（W-18 存酒列表与取酒核销）
     *
     * <p>复用顾客侧的四态口径与排序；**按 shopId 限定本门店**（顾客侧只按 memberId）。
     * 支持筛选：storageCode（精确/模糊）、memberPhone（精确定位会员）。
     * 同时填充 memberName / memberPhone（脱敏），供列表「顾客」列展示。
     */
    List<WineStorageVo> listByShop(Integer shopId, WineStorageParam param);

    /**
     * 商家录入存酒（预留接口，本批无商家端 UI）
     * memberPhone 定位会员 → 生成唯一 4 位存酒码 → storage_date=今天，expire_date=今天+expireDays(默认 90)
     *
     * @param shopId 商家所在门店id（由商家端会话推导）
     */
    void create(WineStorageParam param, Integer shopId);

    /**
     * 商家取酒（预留接口，本批无商家端 UI）
     * 原子扣减（WineStorageMapper.atomicTake），成功后写 tb_wine_storage_record
     */
    void take(WineStorageParam param, Integer operatorId);
}
