package com.siam.system.modular.package_goods.service.internal;

import com.siam.system.modular.package_goods.entity.internal.ShopSetting;

import java.util.Map;

/** 门店营业设置服务（批次 G · 收银端 W-23） */
public interface ShopSettingService {

    /**
     * 读出门店的全部设置（键 → 值）。
     * <p>缺失的键**不会**出现在 Map 中，由调用方决定默认值（避免把默认值写死在服务层）。
     */
    Map<String, String> getSettings(Integer shopId);

    /**
     * 批量保存（upsert）：存在则更新，不存在则插入。
     * <p>只写入传入的键；未传的键保持不变。
     */
    void saveSettings(Integer shopId, Map<String, String> settings);

    /** 删除某个键（用于「恢复默认」） */
    void removeSetting(Integer shopId, String key);

    /** 键常量引用，避免调用方写错字面量 */
    String[] knownKeys();
}
