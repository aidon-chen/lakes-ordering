package com.siam.system.modular.package_points.service;

import com.siam.system.modular.package_points.entity.MemberLevelSetting;
import com.siam.system.modular.package_points.entity.MemberPointsSetting;

import java.util.List;

/**
 * 积分规则配置服务 · 业务侧只读，「积分管理」后台可写。
 * 活动分值等一律走此配置（用户拍板 2026-09-23：不硬编码）。
 */
public interface MemberPointsSettingService {

    /** 读取整型配置，缺失时返回默认值（保证配置表被清空也能兜底运行） */
    Integer getInt(String itemKey, Integer defaultVal);

    List<MemberPointsSetting> listAll();

    void updateItem(String itemKey, Integer itemValue);

    List<MemberLevelSetting> levelList();

    void updateLevel(MemberLevelSetting setting);
}
