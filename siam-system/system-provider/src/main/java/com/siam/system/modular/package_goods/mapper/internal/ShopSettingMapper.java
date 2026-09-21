package com.siam.system.modular.package_goods.mapper.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_goods.entity.internal.ShopSetting;

/**
 * 门店营业设置 Mapper（批次 G · W-23）
 * 查询用 QueryWrapper 即可，无需 Example 与 XML —— 与 PrinterMapper 的「一堆 Example 方法」不同，
 * 本表是纯键值对，没有复杂条件查询需求。
 */
public interface ShopSettingMapper extends BaseMapper<ShopSetting> {
}
