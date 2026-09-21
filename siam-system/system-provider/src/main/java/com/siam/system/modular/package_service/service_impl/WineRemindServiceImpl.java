package com.siam.system.modular.package_service.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.internal.ShopSetting;
import com.siam.system.modular.package_goods.service.internal.ShopSettingService;
import com.siam.system.modular.package_service.entity.WineRemindLog;
import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.mapper.WineRemindLogMapper;
import com.siam.system.modular.package_service.mapper.WineStorageMapper;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;
import com.siam.system.modular.package_service.service.WineRemindService;
import com.siam.system.modular.package_service.service.WineStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 存酒临期提醒实现 · 批次 F · W-19
 *
 * <p>复用 {@link WineStorageService#listByShop} / {@link WineStorageService#listByMember}
 * （它们已做**惰性过期**与会员姓名/脱敏手机号填充），本类只做：
 * 剩余天数计算、临期/过期过滤、通知状态（读 tb_wine_remind_log）。
 * 不写自定义 SQL —— 同批次 G 的取舍：MP 查实体 + Java 组装，避免 @ResultMap 列名坑。
 */
@Service
public class WineRemindServiceImpl implements WineRemindService {

    /** 提前提醒天数默认值（门店可在 W-19 页改，存 tb_shop_setting.wineRemindAheadDays） */
    public static final int DEFAULT_AHEAD_DAYS = 7;

    @Autowired
    private WineStorageService wineStorageService;

    @Autowired
    private WineRemindLogMapper wineRemindLogMapper;

    @Autowired
    private WineStorageMapper wineStorageMapper;

    @Autowired
    private ShopSettingService shopSettingService;

    @Override
    public List<Map<String, Object>> listForMerchant(Integer shopId, Integer aheadDays) {
        List<Map<String, Object>> rows = baseRows(shopId, aheadDays);
        attachNotifyState(rows);
        return rows;
    }

    @Override
    public Map<String, Object> summary(Integer shopId, Integer aheadDays) {
        List<Map<String, Object>> rows = baseRows(shopId, aheadDays);
        long expiring = 0, expired = 0, notified = 0;
        for (Map<String, Object> r : rows) {
            boolean isExpired = Boolean.TRUE.equals(r.get("expired"));
            if (isExpired) {
                expired++;
            } else {
                expiring++;
            }
            if (r.get("lastNotifyTime") != null) {
                notified++;
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("expiring", expiring);
        m.put("expired", expired);
        m.put("notified", notified);
        return m;
    }

    @Override
    public int ack(Integer memberId, List<Integer> storageIds) {
        if (memberId == null || storageIds == null || storageIds.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        for (Integer sid : storageIds) {
            if (sid == null) {
                continue;
            }
            WineStorage ws = wineStorageMapper.selectById(sid.longValue());
            // 归属校验：不能替别人「已阅」
            if (ws == null || !memberId.equals(ws.getMemberId())) {
                throw new StoneCustomerException("存酒记录不存在或不属于当前会员");
            }
            // 幂等：同一存酒同一天只记一条（顾客反复进出页面不会刷屏）
            QueryWrapper<WineRemindLog> qw = new QueryWrapper<>();
            qw.eq("storage_id", sid).eq("member_id", memberId)
              .apply("date(create_time) = {0}", today);
            if (wineRemindLogMapper.selectCount(qw) > 0) {
                continue;
            }
            WineRemindLog log = new WineRemindLog();
            log.setShopId(ws.getShopId());
            log.setMemberId(memberId);
            log.setStorageId(sid);
            log.setStorageCode(ws.getStorageCode());
            log.setChannel(WineRemindLog.CHANNEL_MINI_POPUP);
            log.setExpireDateAtNotify(ws.getExpireDate());
            log.setCreateTime(new Date());
            inserted += wineRemindLogMapper.insert(log);
        }
        return inserted;
    }

    @Override
    public List<Map<String, Object>> listExpiringForMember(Integer memberId, Integer aheadDays) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WineStorageVo v : wineStorageService.listByMember(memberId)) {
            Integer days = daysRemaining(v.getExpireDate());
            if (days == null || days > aheadDays) {
                continue; // 只取临期(0..N)与过期(<0)
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storageId", v.getId() == null ? null : v.getId().intValue());
            m.put("storageCode", v.getStorageCode());
            m.put("goodsName", v.getGoodsName());
            m.put("remainCount", v.getRemainCount());
            m.put("expireDate", v.getExpireDate());
            m.put("daysRemaining", days);
            m.put("expired", days < 0);
            rows.add(m);
        }
        rows.sort(Comparator.comparingInt(r -> (int) r.get("daysRemaining")));
        return rows;
    }

    @Override
    public List<Map<String, Object>> listExpiringForMemberAuto(Integer memberId) {
        // 单店场景：该会员所有存酒属同一门店，取任一条的 shopId 读设置即可
        WineStorage any = wineStorageMapper.selectOne(
                new QueryWrapper<WineStorage>().eq("member_id", memberId).last("LIMIT 1"));
        Integer aheadDays = any == null || any.getShopId() == null
                ? DEFAULT_AHEAD_DAYS : aheadDaysForShop(any.getShopId());
        return listExpiringForMember(memberId, aheadDays);
    }

    /** 顾客侧的 aheadDays：按记录所属门店读设置（单店场景恒为同一门店），读不到用默认 7 */
    @Override
    public Integer aheadDaysForShop(Integer shopId) {
        Map<String, String> s = shopSettingService.getSettings(shopId);
        String v = s.get(ShopSetting.KEY_WINE_REMIND_AHEAD_DAYS);
        try {
            return (v == null || v.isEmpty()) ? DEFAULT_AHEAD_DAYS : Math.max(1, Integer.parseInt(v));
        } catch (NumberFormatException e) {
            return DEFAULT_AHEAD_DAYS;
        }
    }

    /* ==================== 私有 ==================== */

    /** 存续中(1)/已过期(3) 且有效期非空的存酒 → 行（含剩余天数）；已取完(2)不提醒 */
    private List<Map<String, Object>> baseRows(Integer shopId, Integer aheadDays) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WineStorageVo v : wineStorageService.listByShop(shopId, new WineStorageParam())) {
            Integer statusCode = v.getStatusCode();
            if (statusCode == null || (statusCode != 1 && statusCode != 3)) {
                continue; // 1=存续中 3=已过期；2=已取完不提醒
            }
            Integer days = daysRemaining(v.getExpireDate());
            if (days == null || days > aheadDays) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storageId", v.getId() == null ? null : v.getId().intValue());
            m.put("storageCode", v.getStorageCode());
            m.put("customerName", v.getMemberName());
            m.put("customerPhone", v.getMemberPhone());
            m.put("goodsName", v.getGoodsName());
            m.put("remainCount", v.getRemainCount());
            m.put("expireDate", v.getExpireDate());
            m.put("daysRemaining", days);
            m.put("expired", days < 0);
            rows.add(m);
        }
        rows.sort(Comparator.comparingInt(r -> (int) r.get("daysRemaining")));
        return rows;
    }

    /** 批量补「通知状态」：该存酒最近一次弹窗时间（一行一查会 N+1，故一次查出后按 storageId 归组） */
    private void attachNotifyState(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Integer sid = (Integer) r.get("storageId");
            if (sid != null && !ids.contains(sid)) {
                ids.add(sid);
            }
        }
        Map<Integer, Date> latest = new HashMap<>();
        for (Integer sid : ids) {
            QueryWrapper<WineRemindLog> qw = new QueryWrapper<>();
            qw.eq("storage_id", sid).orderByDesc("create_time").last("LIMIT 1");
            WineRemindLog log = wineRemindLogMapper.selectOne(qw);
            if (log != null) {
                latest.put(sid, log.getCreateTime());
            }
        }
        for (Map<String, Object> r : rows) {
            Integer sid = (Integer) r.get("storageId");
            Date d = latest.get(sid);
            r.put("lastNotifyTime", d);
            r.put("notified", d != null);
        }
    }

    /** 剩余天数 = 有效期 - 今天（>0 未到期；0=当天到期；<0 已过期 N 天）。expireDate 是 fmtDate 的字符串 */
    private Integer daysRemaining(String expireDateText) {
        if (expireDateText == null || expireDateText.trim().isEmpty()) {
            return null;
        }
        String s = expireDateText.trim().replace('/', '-');
        try {
            Date expire = new SimpleDateFormat("yyyy-MM-dd").parse(s.length() > 10 ? s.substring(0, 10) : s);
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Date t0 = new SimpleDateFormat("yyyy-MM-dd").parse(today);
            long diff = (expire.getTime() - t0.getTime()) / (24L * 60 * 60 * 1000);
            return (int) diff;
        } catch (Exception e) {
            return null;
        }
    }
}
