package com.siam.system.modular.package_service.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_goods.mapper.ShopMapper;
import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.entity.WineStorageRecord;
import com.siam.system.modular.package_service.mapper.WineStorageMapper;
import com.siam.system.modular.package_service.mapper.WineStorageRecordMapper;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;
import com.siam.system.modular.package_service.service.WineStorageService;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存酒服务实现 · C-16 / C-01 存酒区
 *
 * 取酒并发安全：WineStorageMapper.atomicTake 用「条件式 UPDATE + DB 行锁」，
 * 绝不「先查后改」。扣到 0 瓶由 SQL 内 CASE 自动置 status=2（已取完）。
 */
@Service
public class WineStorageServiceImpl implements WineStorageService {

    /** 临期阈值（批次 5 决策 D-H：距到期 ≤30 天算将过期） */
    private static final int EXPIRE_WARNING_DAYS = 30;

    /** 默认存酒有效期（天），原型提示条「存酒有效期 90 天」 */
    private static final int DEFAULT_EXPIRE_DAYS = 90;

    /** 状态码 */
    private static final int CODE_ACTIVE = 1;
    private static final int CODE_TAKEN = 2;
    private static final int CODE_EXPIRED = 3;
    private static final int CODE_WARNING = 4;

    private static final Map<Integer, String> STATUS_TEXT = new HashMap<Integer, String>() {{
        put(CODE_ACTIVE, "存续中");
        put(CODE_TAKEN, "已取完");
        put(CODE_EXPIRED, "已过期");
        put(CODE_WARNING, "将过期");
    }};

    /**
     * 列表展示顺序（原型 298:53 自上而下）：存续中 → 将过期 → 已过期 → 已取完。
     * <p>
     * 不能直接按 statusCode 升序排：CODE_EXPIRED=3 小于 CODE_WARNING=4，
     * 升序会把「已过期」排在「将过期」前面，与原型行序正好相反。
     * 状态码是接口契约（前端 wineTagClass 依赖 1/2/3/4），故在此单独映射展示序，不改状态码取值。
     */
    private static int displayRank(int statusCode) {
        switch (statusCode) {
            case CODE_ACTIVE:
                return 1;   // 存续中
            case CODE_WARNING:
                return 2;   // 将过期
            case CODE_EXPIRED:
                return 3;   // 已过期
            case CODE_TAKEN:
                return 4;   // 已取完
            default:
                return 99;
        }
    }

    @Autowired
    private WineStorageMapper wineStorageMapper;

    @Autowired
    private WineStorageRecordMapper wineStorageRecordMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ShopMapper shopMapper;

    /* ==================== 顾客侧 ==================== */

    @Override
    public List<WineStorageVo> listByMember(Integer memberId) {
        // 惰性过期：查询前先把已过有效期但仍标为在存(1)的记录置为已过期(3)
        wineStorageMapper.expireOutdated();

        List<WineStorage> list = wineStorageMapper.selectList(
                new QueryWrapper<WineStorage>().eq("member_id", memberId).orderByAsc("expire_date"));

        Map<Integer, String> shopNames = shopNameMap(list);
        List<WineStorageVo> vos = new ArrayList<>();
        for (WineStorage ws : list) {
            vos.add(toVo(ws, shopNames));
        }
        // 展示序：存续中 → 将过期 → 已过期 → 已取完；同组内保 expire_date 升序
        // （DB 已按 expire_date 排好 + List.sort 稳定排序，故同组保序）
        vos.sort(Comparator.comparingInt((WineStorageVo vo) -> displayRank(vo.getStatusCode())));
        return vos;
    }

    @Override
    public WineStorageVo detailByMember(Integer memberId, Long id) {
        WineStorage ws = wineStorageMapper.selectById(id);
        if (ws == null || !ws.getMemberId().equals(memberId)) {
            throw new StoneCustomerException("存酒记录不存在");
        }
        Map<Integer, String> shopNames = shopNameMap(java.util.Collections.singletonList(ws));
        return toVo(ws, shopNames);
    }

    /* ==================== 商家侧（W-17 登记 / W-18 列表与取酒核销） ==================== */

    @Override
    public List<WineStorageVo> listByShop(Integer shopId, WineStorageParam param) {
        if (shopId == null) {
            throw new StoneCustomerException("门店信息缺失");
        }
        // 惰性过期：与顾客侧同口径，查询前先把过期但仍标为在存(1)的记录置为已过期(3)
        wineStorageMapper.expireOutdated();

        QueryWrapper<WineStorage> qw = new QueryWrapper<>();
        qw.eq("shop_id", shopId);
        if (param != null) {
            String code = param.getStorageCode() == null ? null : param.getStorageCode().trim();
            if (code != null && !code.isEmpty()) {
                qw.like("storage_code", code);
            }
            String phone = param.getMemberPhone() == null ? null : param.getMemberPhone().trim();
            if (phone != null && !phone.isEmpty()) {
                Member m = memberMapper.selectOne(new QueryWrapper<Member>()
                        .eq("mobile", phone).last("limit 1"));
                if (m == null) {
                    // 手机号无对应会员 → 该筛选必然为空，直接返回，不做无谓全扫
                    return new ArrayList<>();
                }
                qw.eq("member_id", m.getId());
            }
        }
        qw.orderByAsc("expire_date");
        List<WineStorage> list = wineStorageMapper.selectList(qw);

        Map<Integer, String> shopNames = shopNameMap(list);
        Map<Integer, Member> members = memberMap(list);
        List<WineStorageVo> vos = new ArrayList<>();
        for (WineStorage ws : list) {
            WineStorageVo vo = toVo(ws, shopNames);
            if (ws.getMemberId() != null) {
                Member m = members.get(ws.getMemberId());
                if (m != null) {
                    vo.setMemberId(m.getId());
                    vo.setMemberName(m.getNickname());
                    vo.setMemberPhone(maskMobile(m.getMobile()));
                }
            }
            vos.add(vo);
        }
        // 与顾客侧同序：存续中 → 将过期 → 已过期 → 已取完
        vos.sort(Comparator.comparingInt((WineStorageVo vo) -> displayRank(vo.getStatusCode())));
        return vos;
    }

    /** 批量取会员（供商家侧列表填「顾客」列，避免 N+1） */
    private Map<Integer, Member> memberMap(List<WineStorage> list) {
        List<Integer> ids = new ArrayList<>();
        for (WineStorage ws : list) {
            if (ws.getMemberId() != null && !ids.contains(ws.getMemberId())) {
                ids.add(ws.getMemberId());
            }
        }
        Map<Integer, Member> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Member m : memberMapper.selectBatchIds(ids)) {
            map.put(m.getId(), m);
        }
        return map;
    }

    /** 手机号脱敏：138****8601；非 11 位原样返回 */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    @Override
    public WineStorage create(WineStorageParam param, Integer shopId) {
        if (param.getMemberPhone() == null || param.getMemberPhone().trim().isEmpty()) {
            throw new StoneCustomerException("会员手机号不能为空");
        }
        if (param.getGoodsName() == null || param.getGoodsName().trim().isEmpty()) {
            throw new StoneCustomerException("品名不能为空");
        }
        if (shopId == null) {
            throw new StoneCustomerException("门店id不能为空");
        }
        Member member = memberMapper.selectOne(
                new QueryWrapper<Member>().eq("mobile", param.getMemberPhone().trim()).last("LIMIT 1"));
        if (member == null) {
            throw new StoneCustomerException("未找到该手机号对应的会员");
        }

        int bottleCount = param.getBottleCount() == null || param.getBottleCount() < 1 ? 1 : param.getBottleCount();
        int expireDays = param.getExpireDays() == null || param.getExpireDays() < 1
                ? DEFAULT_EXPIRE_DAYS : param.getExpireDays();
        Date today = today();

        WineStorage ws = new WineStorage();
        ws.setMemberId(member.getId());
        ws.setShopId(shopId);
        ws.setGoodsName(param.getGoodsName().trim());
        ws.setBottleCount(bottleCount);
        ws.setRemainCount(bottleCount);
        ws.setStorageCode(generateUniqueCode());
        ws.setLocation(param.getLocation() == null ? null : param.getLocation().trim());
        ws.setStorageDate(today);
        ws.setExpireDate(addDays(today, expireDays));
        ws.setStatus(CODE_ACTIVE);
        ws.setCreateTime(new Date());
        ws.setUpdateTime(new Date());
        wineStorageMapper.insert(ws);
        // 返回创建结果：W-17 凭证预览需要展示生成的存酒码与有效期
        return ws;
    }

    @Override
    public void take(WineStorageParam param, Integer operatorId) {
        if (param.getStorageCode() == null || param.getStorageCode().trim().isEmpty()) {
            throw new StoneCustomerException("存酒码不能为空");
        }
        int takeCount = param.getTakeCount() == null || param.getTakeCount() < 1 ? 1 : param.getTakeCount();

        WineStorage db = wineStorageMapper.selectOne(
                new QueryWrapper<WineStorage>().eq("storage_code", param.getStorageCode().trim()).last("LIMIT 1"));
        if (db == null) {
            throw new StoneCustomerException("存酒码不存在");
        }

        // 原子扣减：影响行数=1 才成功（并发兜底靠 DB 行锁）
        int rows = wineStorageMapper.atomicTake(param.getStorageCode().trim(), takeCount);
        if (rows != 1) {
            throw new StoneCustomerException("取酒失败：存酒码无效、已过期或剩余瓶数不足");
        }

        WineStorageRecord record = new WineStorageRecord();
        record.setStorageId(db.getId());
        record.setTakeCount(takeCount);
        record.setOperatorId(operatorId);
        record.setRemark(param.getRemark());
        record.setTakeTime(new Date());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        wineStorageRecordMapper.insert(record);
    }

    /* ==================== 私有 ==================== */

    private Map<Integer, String> shopNameMap(List<WineStorage> list) {
        Set<Integer> shopIds = list.stream()
                .map(WineStorage::getShopId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (shopIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName, (a, b) -> a));
    }

    private WineStorageVo toVo(WineStorage ws, Map<Integer, String> shopNames) {
        WineStorageVo vo = new WineStorageVo();
        vo.setId(ws.getId());
        vo.setGoodsName(ws.getGoodsName());
        vo.setBottleCount(ws.getBottleCount());
        vo.setRemainCount(ws.getRemainCount());
        vo.setStorageCode(ws.getStorageCode());
        vo.setLocation(ws.getLocation());
        if (ws.getShopId() != null) {
            vo.setShopName(shopNames.get(ws.getShopId()));
        }

        int code = statusCode(ws);
        vo.setStatusCode(code);
        vo.setStatusText(STATUS_TEXT.get(code));

        if (ws.getStorageDate() != null) {
            vo.setStorageDate(fmtDate(ws.getStorageDate()));
        }
        if (ws.getExpireDate() != null) {
            vo.setExpireDate(fmtDate(ws.getExpireDate()));
            vo.setExpireLine("有效期至 " + fmtDate(ws.getExpireDate()));
        }

        // 展示行：No.8866 · B 区 03 号柜 · 存入 08-20（柜位可空）
        StringBuilder line = new StringBuilder("No.").append(ws.getStorageCode());
        if (ws.getLocation() != null && !ws.getLocation().trim().isEmpty()) {
            line.append(" · ").append(ws.getLocation().trim());
        }
        if (ws.getStorageDate() != null) {
            line.append(" · 存入 ").append(new SimpleDateFormat("MM-dd").format(ws.getStorageDate()));
        }
        vo.setSummaryLine(line.toString());
        return vo;
    }

    /**
     * 计算展示状态：
     * 表态 status=2（已取完）> 已过期（表态 3 或已过有效期）> 将过期（≤30 天）> 存续中
     */
    private int statusCode(WineStorage ws) {
        if (ws.getStatus() != null && ws.getStatus() == CODE_TAKEN) {
            return CODE_TAKEN;
        }
        Date today = today();
        if (ws.getStatus() != null && ws.getStatus() == CODE_EXPIRED
                || (ws.getExpireDate() != null && ws.getExpireDate().before(today))) {
            return CODE_EXPIRED;
        }
        if (ws.getExpireDate() != null && daysLeft(ws.getExpireDate(), today) <= EXPIRE_WARNING_DAYS) {
            return CODE_WARNING;
        }
        return CODE_ACTIVE;
    }

    private long daysLeft(Date expireDate, Date today) {
        return (expireDate.getTime() - today.getTime()) / (24L * 60 * 60 * 1000);
    }

    /** 归零到当天 00:00:00，保证「按天」比较不受时分秒影响 */
    private Date today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date addDays(Date base, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(base);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    private String fmtDate(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    /** 生成唯一 4 位存酒码（1000-9999，撞码重试；tb_wine_storage.uk_storage_code 为 DB 层兜底） */
    private String generateUniqueCode() {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            String code = String.valueOf(1000 + random.nextInt(9000));
            Integer count = wineStorageMapper.selectCount(
                    new QueryWrapper<WineStorage>().eq("storage_code", code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new StoneCustomerException("存酒码生成失败，请稍后重试");
    }
}
