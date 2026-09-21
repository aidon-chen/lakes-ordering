package com.siam.system.modular.package_goods.service_impl.internal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.system.modular.package_goods.entity.internal.ShopSetting;
import com.siam.system.modular.package_goods.mapper.internal.ShopSettingMapper;
import com.siam.system.modular.package_goods.service.internal.ShopSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 门店营业设置实现（批次 G · W-23） */
@Service
public class ShopSettingServiceImpl implements ShopSettingService {

    @Autowired
    private ShopSettingMapper shopSettingMapper;

    @Override
    public Map<String, String> getSettings(Integer shopId) {
        Map<String, String> map = new HashMap<>();
        if (shopId == null) {
            return map;
        }
        List<ShopSetting> list = shopSettingMapper.selectList(
                new QueryWrapper<ShopSetting>().eq("shop_id", shopId));
        for (ShopSetting s : list) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    @Override
    public void saveSettings(Integer shopId, Map<String, String> settings) {
        if (shopId == null || settings == null || settings.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> e : settings.entrySet()) {
            String key = e.getKey();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            ShopSetting db = shopSettingMapper.selectOne(new QueryWrapper<ShopSetting>()
                    .eq("shop_id", shopId).eq("setting_key", key).last("LIMIT 1"));
            if (db == null) {
                ShopSetting insert = new ShopSetting();
                insert.setShopId(shopId);
                insert.setSettingKey(key);
                insert.setSettingValue(e.getValue());
                insert.setCreateTime(new Date());
                insert.setUpdateTime(new Date());
                shopSettingMapper.insert(insert);
            } else {
                ShopSetting update = new ShopSetting();
                update.setId(db.getId());
                update.setSettingValue(e.getValue());
                update.setUpdateTime(new Date());
                shopSettingMapper.updateById(update);
            }
        }
    }

    @Override
    public void removeSetting(Integer shopId, String key) {
        if (shopId == null || key == null) {
            return;
        }
        shopSettingMapper.delete(new QueryWrapper<ShopSetting>()
                .eq("shop_id", shopId).eq("setting_key", key));
    }

    @Override
    public String[] knownKeys() {
        return new String[]{
                ShopSetting.KEY_BUSINESS_OPEN,
                ShopSetting.KEY_ORDER_VOICE_ALERT,
                ShopSetting.KEY_VOICE_VOLUME,
                ShopSetting.KEY_AUTO_ACCEPT,
                ShopSetting.KEY_PRODUCTION_TIMEOUT_MIN,
                ShopSetting.KEY_CLOUD_PRINT,
                ShopSetting.KEY_LOCAL_PRINT,
                ShopSetting.KEY_PRINT_MODE,
                ShopSetting.KEY_DEFAULT_PRINTER_ID,
                ShopSetting.KEY_PRINT_RETRY_MAX,
                ShopSetting.KEY_PRINT_RETRY_INTERVAL_SEC,
                ShopSetting.KEY_PRINT_FAIL_ALERT,
                ShopSetting.KEY_WINE_REMIND_AHEAD_DAYS
        };
    }
}
