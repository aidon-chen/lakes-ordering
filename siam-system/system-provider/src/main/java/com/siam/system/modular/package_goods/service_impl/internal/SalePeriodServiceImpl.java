package com.siam.system.modular.package_goods.service_impl.internal;

import com.siam.system.modular.package_goods.entity.internal.ShopSetting;
import com.siam.system.modular.package_goods.mapper.SalePeriodMapper;
import com.siam.system.modular.package_goods.service.internal.SalePeriodService;
import com.siam.system.modular.package_goods.service.internal.ShopSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 营业时段服务实现（叠加设计）—— 全部"读时判定"，不依赖任何定时任务。
 *
 * <p>数据来源：
 * <ul>
 *   <li>门店级配置：{@code tb_shop_setting} 的 {@code salePeriodEnabled} / {@code salePeriodRules}；</li>
 *   <li>分类级：{@code tb_menu.sale_period}（day/night/all/NULL=全天）；</li>
 *   <li>单品级：{@code tb_goods_sale_period}（未配置=跟随分类）。</li>
 * </ul>
 *
 * <p>时间口径：**服务器时间**（Asia/Shanghai），不信任客户端时间——客户端时钟可改。
 * 跨零点沿用前端 {@code utils/date-helper.js:63 differenceTime()} 的同一套判断
 * （{@code start > end} 时用"或"）；打烊空档（不在任何窗口内）返回 {@link #PERIOD_CLOSED}。
 *
 * <p>降级策略（重要）：结构缺失（目标库未执行 31- 脚本）或配置读取异常时，
 * 一律**降级为"未启用/无覆盖"**并打 WARN 日志，绝不让顾客端菜单接口整体报错。
 */
@Slf4j
@Service
public class SalePeriodServiceImpl implements SalePeriodService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** 配置与映射的进程内缓存时长：30s（改完设置最多半分钟生效，避免每次读菜单都查库） */
    private static final long CACHE_TTL_MS = 30 * 1000L;

    @Autowired
    private ShopSettingService shopSettingService;

    @Autowired
    private SalePeriodMapper salePeriodMapper;

    /** key → [值, 写入时刻]；用 ConcurrentHashMap 承载，key 已含 shopId 维度 */
    private final ConcurrentHashMap<String, Object[]> cache = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------- 对外

    @Override
    public String currentPeriod(Integer shopId) {
        if (shopId == null) {
            return null;
        }
        Map<String, String> settings = settings(shopId);
        //未启用：返回 null，调用方据此完全跳过过滤（行为退回现状）
        if (!"true".equalsIgnoreCase(trim(settings.get(ShopSetting.KEY_SALE_PERIOD_ENABLED)))) {
            return null;
        }
        String rules = trim(settings.get(ShopSetting.KEY_SALE_PERIOD_RULES));
        if (rules.isEmpty()) {
            return null;
        }
        LocalTime now = LocalTime.now(ZONE);
        for (String seg : rules.split(",")) {
            String item = trim(seg);
            int eq = item.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String code = trim(item.substring(eq + 1));
            String window = trim(item.substring(0, eq));
            int dash = window.indexOf('-');
            if (code.isEmpty() || dash <= 0) {
                continue;
            }
            LocalTime start = parseHhMm(window.substring(0, dash));
            LocalTime end = parseHhMm(window.substring(dash + 1));
            if (start == null || end == null) {
                log.warn("[SalePeriod] 时段规则无法解析，已跳过：{}（shopId={}）", item, shopId);
                continue;
            }
            boolean in;
            if (start.equals(end)) {
                //首尾相同视为全天（与前端 differenceTime 一致）
                in = true;
            } else if (start.isBefore(end)) {
                in = !now.isBefore(start) && !now.isAfter(end);
            } else {
                //跨零点：如 17:00-02:00
                in = !now.isBefore(start) || !now.isAfter(end);
            }
            if (in) {
                return code;
            }
        }
        //已启用但不在任何窗口内 = 打烊空档
        return PERIOD_CLOSED;
    }

    @Override
    public boolean matchPeriod(String periodValue, String currentPeriod) {
        if (currentPeriod == null) {
            return true;          // 未启用时段功能 → 不过滤
        }
        if (PERIOD_CLOSED.equalsIgnoreCase(currentPeriod)) {
            return false;         // 打烊空档 → 一律不可售
        }
        String value = trim(periodValue);
        if (value.isEmpty() || PERIOD_ALL.equalsIgnoreCase(value)) {
            return true;          // 未配置 / 显式 all → 全天可售
        }
        return value.equalsIgnoreCase(currentPeriod);
    }

    @Override
    public boolean isGoodsSellable(Integer shopId, Integer goodsId) {
        String current = currentPeriod(shopId);
        if (current == null || goodsId == null) {
            return true;
        }
        //① 单品级覆盖优先
        Set<String> allow = goodsOverrides(shopId).get(goodsId);
        if (allow != null && !allow.isEmpty()) {
            return allow.contains(current);
        }
        //② 回落分类级
        Integer menuId = goodsMenuRelations(shopId).get(goodsId);
        if (menuId == null) {
            return true;          // 未挂分类：不拦（保持既有数据可卖）
        }
        return matchPeriod(menuPeriods(shopId).get(menuId), current);
    }

    @Override
    public Map<Integer, String> menuPeriods(Integer shopId) {
        if (shopId == null) {
            return Collections.emptyMap();
        }
        return cached("menuPeriods:" + shopId, () -> {
            Map<Integer, String> result = new HashMap<>();
            try {
                List<Map<String, Object>> rows = salePeriodMapper.selectMenuPeriods(shopId);
                for (Map<String, Object> row : rows) {
                    Integer menuId = toInteger(row.get("menuId"));
                    if (menuId != null) {
                        Object period = row.get("salePeriod");
                        result.put(menuId, period == null ? null : String.valueOf(period));
                    }
                }
            } catch (Exception e) {
                //结构缺失（未执行 31- 脚本）或库异常 → 降级为"无配置"，菜单不因此整体报错
                log.warn("[SalePeriod] 读取 tb_menu.sale_period 失败，已降级为无时段配置：shopId={}，原因={}",
                        shopId, e.getMessage());
            }
            return result;
        });
    }

    @Override
    public Map<Integer, Set<String>> goodsOverrides(Integer shopId) {
        if (shopId == null) {
            return Collections.emptyMap();
        }
        return cached("goodsOverrides:" + shopId, () -> {
            Map<Integer, Set<String>> result = new HashMap<>();
            try {
                List<Map<String, Object>> rows = salePeriodMapper.selectGoodsOverrides(shopId);
                for (Map<String, Object> row : rows) {
                    Integer goodsId = toInteger(row.get("goodsId"));
                    String periodCode = trim(row.get("periodCode"));
                    if (goodsId != null && !periodCode.isEmpty()) {
                        result.computeIfAbsent(goodsId, k -> new HashSet<>()).add(periodCode.toLowerCase());
                    }
                }
            } catch (Exception e) {
                log.warn("[SalePeriod] 读取 tb_goods_sale_period 失败，已降级为无单品覆盖：shopId={}，原因={}",
                        shopId, e.getMessage());
            }
            return result;
        });
    }

    // ---------------------------------------------------------------- 内部

    private Map<Integer, Integer> goodsMenuRelations(Integer shopId) {
        return cached("goodsMenu:" + shopId, () -> {
            Map<Integer, Integer> result = new HashMap<>();
            try {
                List<Map<String, Object>> rows = salePeriodMapper.selectGoodsMenuRelations(shopId);
                for (Map<String, Object> row : rows) {
                    Integer goodsId = toInteger(row.get("goodsId"));
                    Integer menuId = toInteger(row.get("menuId"));
                    if (goodsId != null && menuId != null) {
                        //一个商品只挂一个分类；若历史数据出现多挂，保留首个（与 C 端"商品只属一个分类"的既有口径一致）
                        result.putIfAbsent(goodsId, menuId);
                    }
                }
            } catch (Exception e) {
                log.warn("[SalePeriod] 读取 tb_menu_goods_relation 失败，已降级为空映射：shopId={}，原因={}",
                        shopId, e.getMessage());
            }
            return result;
        });
    }

    private Map<String, String> settings(Integer shopId) {
        return cached("settings:" + shopId, () -> {
            try {
                Map<String, String> map = shopSettingService.getSettings(shopId);
                return map == null ? Collections.<String, String>emptyMap() : map;
            } catch (Exception e) {
                log.warn("[SalePeriod] 读取门店设置失败，已降级为未启用：shopId={}，原因={}", shopId, e.getMessage());
                return Collections.<String, String>emptyMap();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        Object[] hit = cache.get(key);
        if (hit != null && now - (Long) hit[1] < CACHE_TTL_MS) {
            return (T) hit[0];
        }
        T value = loader.get();
        cache.put(key, new Object[]{value, now});
        return value;
    }

    private static LocalTime parseHhMm(String text) {
        try {
            return LocalTime.parse(trim(text), HH_MM);
        } catch (Exception e) {
            return null;
        }
    }

    private static String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
