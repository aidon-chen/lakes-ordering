package com.siam.system.modular.package_points.service_impl;

import com.siam.system.modular.package_points.entity.MemberLevelSetting;
import com.siam.system.modular.package_points.entity.MemberPointsSetting;
import com.siam.system.modular.package_points.mapper.MemberLevelSettingMapper;
import com.siam.system.modular.package_points.mapper.MemberPointsSettingMapper;
import com.siam.system.modular.package_points.service.MemberPointsSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class MemberPointsSettingServiceImpl implements MemberPointsSettingService {

    @Autowired
    private MemberPointsSettingMapper memberPointsSettingMapper;

    @Autowired
    private MemberLevelSettingMapper memberLevelSettingMapper;

    @Override
    public Integer getInt(String itemKey, Integer defaultVal) {
        MemberPointsSetting db = memberPointsSettingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MemberPointsSetting>()
                        .eq("item_key", itemKey).last("LIMIT 1"));
        return (db == null || db.getItemValue() == null) ? defaultVal : db.getItemValue();
    }

    @Override
    public List<MemberPointsSetting> listAll() {
        return memberPointsSettingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MemberPointsSetting>()
                        .orderByAsc("id"));
    }

    @Override
    public void updateItem(String itemKey, Integer itemValue) {
        MemberPointsSetting db = memberPointsSettingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MemberPointsSetting>()
                        .eq("item_key", itemKey).last("LIMIT 1"));
        if (db == null) {
            // 键不存在则新增，保证后台可扩展新规则项
            MemberPointsSetting insert = new MemberPointsSetting();
            insert.setItemKey(itemKey);
            insert.setItemValue(itemValue);
            insert.setUpdateTime(new Date());
            memberPointsSettingMapper.insert(insert);
            return;
        }
        MemberPointsSetting update = new MemberPointsSetting();
        update.setId(db.getId());
        update.setItemValue(itemValue);
        update.setUpdateTime(new Date());
        memberPointsSettingMapper.updateById(update);
    }

    @Override
    public List<MemberLevelSetting> levelList() {
        List<MemberLevelSetting> list = memberLevelSettingMapper.selectList(null);
        list.sort(Comparator.comparingInt(MemberLevelSetting::getThreshold));
        return list;
    }

    @Override
    public void updateLevel(MemberLevelSetting setting) {
        if (setting.getId() == null) {
            throw new com.siam.package_common.exception.StoneCustomerException("缺少等级配置id");
        }
        MemberLevelSetting update = new MemberLevelSetting();
        update.setId(setting.getId());
        update.setThreshold(setting.getThreshold());
        update.setName(setting.getName());
        update.setBenefits(setting.getBenefits());
        update.setUpdateTime(new Date());
        memberLevelSettingMapper.updateById(update);
    }
}
