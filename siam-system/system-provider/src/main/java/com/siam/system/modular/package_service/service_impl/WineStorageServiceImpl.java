package com.siam.system.modular.package_service.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_goods.mapper.ShopMapper;
import com.siam.system.modular.package_service.entity.WineStorage;
import com.siam.system.modular.package_service.entity.WineStorageRecord;
import com.siam.system.modular.package_service.entity.WineTakeApply;
import com.siam.system.modular.package_service.mapper.WineStorageMapper;
import com.siam.system.modular.package_service.mapper.WineStorageRecordMapper;
import com.siam.system.modular.package_service.mapper.WineTakeApplyMapper;
import com.siam.system.modular.package_service.model.param.WineStorageParam;
import com.siam.system.modular.package_service.model.vo.WineStorageVo;
import com.siam.system.modular.package_service.model.vo.WineTakeApplyVo;
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
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存酒服务实现 · C-16 / C-01 存酒区 ＋ 取酒申请（方案 v1.1）
 *
 * <p>取酒并发安全：WineStorageMapper.atomicTakeById 用「条件式 UPDATE + DB 行锁」，
 * 绝不「先查后改」。扣到 0 瓶由 SQL 内 CASE 自动置 status=2（已取完）。
 *
 * <p>取酒单一模型：申请 → 取酒码 → 核销。同一存酒同时最多 1 笔有效待核销申请，
 * 超订被结构性消除；申请与核销两处都显式拦截已过期存酒。
 */
@Service
public class WineStorageServiceImpl implements WineStorageService {

    /** 临期阈值（批次 5 决策 D-H：距到期 ≤30 天算将过期） */
    private static final int EXPIRE_WARNING_DAYS = 30;

    /** 默认存酒有效期（天），原型提示条「存酒有效期 90 天」 */
    private static final int DEFAULT_EXPIRE_DAYS = 90;

    /** 取酒码有效期（小时） */
    private static final int TAKE_CODE_TTL_HOURS = 24;

    /** 存酒展示状态码 */
    private static final int CODE_ACTIVE = 1;
    private static final int CODE_TAKEN = 2;
    private static final int CODE_EXPIRED = 3;
    private static final int CODE_WARNING = 4;

    /** 取酒申请状态码（1 起步，方案 v1.1 §11.2-9） */
    private static final int APPLY_PENDING = 1;
    private static final int APPLY_CONFIRMED = 2;
    private static final int APPLY_CANCELLED = 3;
    private static final int APPLY_EXPIRED = 4;

    /** 取酒申请来源 */
    private static final int SOURCE_MEMBER = 1;
    private static final int SOURCE_SHOP = 2;

    private static final Map<Integer, String> STATUS_TEXT = new HashMap<Integer, String>() {{
        put(CODE_ACTIVE, "存续中");
        put(CODE_TAKEN, "已取完");
        put(CODE_EXPIRED, "已过期");
        put(CODE_WARNING, "将过期");
    }};

    private static final Map<Integer, String> APPLY_STATUS_TEXT = new HashMap<Integer, String>() {{
        put(APPLY_PENDING, "待核销");
        put(APPLY_CONFIRMED, "已核销");
        put(APPLY_CANCELLED, "已取消");
        put(APPLY_EXPIRED, "已过期");
    }};

    /**
     * 列表展示顺序（原型 298:53 自上而下）：存续中 → 将过期 → 已过期 → 已取完。
     */
    private static int displayRank(int statusCode) {
        switch (statusCode) {
            case CODE_ACTIVE:
                return 1;
            case CODE_WARNING:
                return 2;
            case CODE_EXPIRED:
                return 3;
            case CODE_TAKEN:
                return 4;
            default:
                return 99;
        }
    }

    @Autowired
    private WineStorageMapper wineStorageMapper;

    @Autowired
    private WineStorageRecordMapper wineStorageRecordMapper;

    @Autowired
    private WineTakeApplyMapper wineTakeApplyMapper;

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
        // 该会员当前是否有「有效待核销」取酒申请（供顾客端展示/禁用申请）
        Map<Long, WineTakeApply> pendingByStorage = pendingApplyMapByMember(memberId);

        List<WineStorageVo> vos = new ArrayList<>();
        for (WineStorage ws : list) {
            WineStorageVo vo = toVo(ws, shopNames);
            if (pendingByStorage.containsKey(ws.getId())) {
                vo.setPendingApply(true);
            }
            vos.add(vo);
        }
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

    /* ==================== 商家侧（W-17 登记 / W-18 存酒列表） ==================== */

    @Override
    public List<WineStorageVo> listByShop(Integer shopId, WineStorageParam param) {
        if (shopId == null) {
            throw new StoneCustomerException("门店信息缺失");
        }
        wineStorageMapper.expireOutdated();

        QueryWrapper<WineStorage> qw = new QueryWrapper<>();
        qw.eq("shop_id", shopId);
        if (param != null) {
            String phone = param.getMemberPhone() == null ? null : param.getMemberPhone().trim();
            if (phone != null && !phone.isEmpty()) {
                Member m = memberMapper.selectOne(new QueryWrapper<Member>()
                        .eq("mobile", phone).last("limit 1"));
                if (m == null) {
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
        ws.setLocation(param.getLocation() == null ? null : param.getLocation().trim());
        ws.setStorageDate(today);
        ws.setExpireDate(addDays(today, expireDays));
        ws.setStatus(CODE_ACTIVE);
        ws.setCreateTime(new Date());
        ws.setUpdateTime(new Date());
        wineStorageMapper.insert(ws);
        // W-17 凭证预览需要展示有效期（取酒码改为「申请后生成」，登记阶段不再出码）
        return ws;
    }

    /* ==================== 取酒申请（方案 v1.1） ==================== */

    @Override
    public WineTakeApplyVo applyTake(Integer memberId, WineStorageParam param) {
        return createApply(memberId, param.getStorageId(), param.getTakeCount(),
                SOURCE_MEMBER, null, param.getRemark());
    }

    @Override
    public WineTakeApplyVo createApplyByShop(Integer operatorId, Integer shopId, WineStorageParam param) {
        return createApply(null, param.getStorageId(), param.getTakeCount(),
                SOURCE_SHOP, operatorId, param.getRemark());
    }

    /**
     * 生成取酒申请（顾客自助 / 商家代发起共用）。
     *
     * @param ownerMemberId 顾客自助时用于校验归属；商家代发起传 null（不校验归属）
     */
    private WineTakeApplyVo createApply(Integer ownerMemberId, Long storageId, Integer takeCount,
                                        int applySource, Integer applyBy, String remark) {
        if (storageId == null) {
            throw new StoneCustomerException("存酒记录不能为空");
        }
        // ① 先清过期存酒（副作用兜底）
        wineStorageMapper.expireOutdated();
        // ② storage 存在
        WineStorage st = wineStorageMapper.selectById(storageId);
        if (st == null) {
            throw new StoneCustomerException("存酒记录不存在");
        }
        // ③ 顾客自助：校验归属（他人记录按「不存在」处理，避免暴露）
        if (applySource == SOURCE_MEMBER) {
            if (ownerMemberId == null || !ownerMemberId.equals(st.getMemberId())) {
                throw new StoneCustomerException("存酒记录不存在");
            }
        }
        // ④ 在存
        if (st.getStatus() == null || st.getStatus() != CODE_ACTIVE) {
            throw new StoneCustomerException("该存酒已取完或已过期，无法申请");
        }
        // ⑤ 显式判期（不依赖 ① 的副作用）
        if (st.getExpireDate() != null && st.getExpireDate().before(today())) {
            throw new StoneCustomerException("该存酒已过有效期，请联系门店");
        }
        // ⑥ 瓶数合法且不超余量
        int cnt = takeCount == null || takeCount < 1 ? 1 : takeCount;
        Integer remain = st.getRemainCount() == null ? 0 : st.getRemainCount();
        if (cnt > remain) {
            throw new StoneCustomerException("取酒瓶数不能超过剩余瓶数");
        }
        // ⑦ 无有效待核销申请
        Integer pending = wineTakeApplyMapper.selectCount(new QueryWrapper<WineTakeApply>()
                .eq("storage_id", storageId)
                .eq("status", APPLY_PENDING)
                .gt("expire_time", new Date()));
        if (pending != null && pending > 0) {
            throw new StoneCustomerException("该存酒已有待核销的取酒申请");
        }
        // ⑧ 生成唯一 6 位取酒码
        Date now = new Date();
        WineTakeApply ap = new WineTakeApply();
        ap.setMemberId(st.getMemberId());
        ap.setShopId(st.getShopId());
        ap.setStorageId(storageId);
        ap.setTakeCount(cnt);
        ap.setTakeCode(generateUniqueTakeCode());
        ap.setStatus(APPLY_PENDING);
        ap.setApplySource(applySource);
        ap.setApplyBy(applyBy);
        ap.setApplyTime(now);
        ap.setExpireTime(addHours(now, TAKE_CODE_TTL_HOURS));
        ap.setRemark(remark);
        ap.setCreateTime(now);
        ap.setUpdateTime(now);
        wineTakeApplyMapper.insert(ap);
        return toApplyVo(ap, st, null);
    }

    @Override
    public List<WineTakeApplyVo> listMyApplies(Integer memberId) {
        wineTakeApplyMapper.expireOutdated();
        List<WineTakeApply> list = wineTakeApplyMapper.selectList(
                new QueryWrapper<WineTakeApply>().eq("member_id", memberId).orderByDesc("apply_time"));
        Map<Long, WineStorage> storages = storageMapByIds(
                list.stream().map(WineTakeApply::getStorageId).collect(Collectors.toList()));
        List<WineTakeApplyVo> vos = new ArrayList<>();
        for (WineTakeApply ap : list) {
            vos.add(toApplyVo(ap, storages.get(ap.getStorageId()), null));
        }
        return vos;
    }

    @Override
    public List<WineTakeApplyVo> listPendingApplies(Integer shopId) {
        if (shopId == null) {
            throw new StoneCustomerException("门店信息缺失");
        }
        wineTakeApplyMapper.expireOutdated();
        List<WineTakeApply> list = wineTakeApplyMapper.selectList(new QueryWrapper<WineTakeApply>()
                .eq("shop_id", shopId)
                .eq("status", APPLY_PENDING)
                .orderByAsc("apply_time"));
        Map<Long, WineStorage> storages = storageMapByIds(
                list.stream().map(WineTakeApply::getStorageId).collect(Collectors.toList()));
        Map<Integer, Member> members = memberMap2(
                list.stream().map(WineTakeApply::getMemberId).collect(Collectors.toList()));
        List<WineTakeApplyVo> vos = new ArrayList<>();
        for (WineTakeApply ap : list) {
            vos.add(toApplyVo(ap, storages.get(ap.getStorageId()), members.get(ap.getMemberId())));
        }
        return vos;
    }

    @Override
    public WineTakeApplyVo previewTake(Integer shopId, String takeCode) {
        WineTakeApply ap = loadValidApplyByCode(takeCode);
        WineStorage st = wineStorageMapper.selectById(ap.getStorageId());
        Member m = ap.getMemberId() == null ? null : memberMapper.selectById(ap.getMemberId());
        return toApplyVo(ap, st, m);
    }

    @Override
    public void cancelApply(Integer memberId, Long applyId) {
        if (applyId == null) {
            throw new StoneCustomerException("取酒申请不能为空");
        }
        wineTakeApplyMapper.expireOutdated();
        WineTakeApply ap = wineTakeApplyMapper.selectById(applyId);
        if (ap == null || !Objects.equals(memberId, ap.getMemberId())) {
            throw new StoneCustomerException("取酒申请不存在");
        }
        boolean expired = ap.getExpireTime() != null && ap.getExpireTime().before(new Date());
        if (ap.getStatus() == null || ap.getStatus() != APPLY_PENDING || expired) {
            throw new StoneCustomerException("该申请已核销/已过期，不可取消");
        }
        ap.setStatus(APPLY_CANCELLED);
        ap.setCancelTime(new Date());
        ap.setUpdateTime(new Date());
        wineTakeApplyMapper.updateById(ap);
    }

    @Override
    public WineTakeApplyVo confirmTake(Integer operatorId, String takeCode) {
        WineTakeApply ap = loadValidApplyByCode(takeCode);
        // 原子扣减：影响行数=1 才成功
        int rows = wineStorageMapper.atomicTakeById(ap.getStorageId(), ap.getTakeCount());
        if (rows != 1) {
            throw new StoneCustomerException("余量不足、已取完或该存酒已过有效期");
        }
        // 写取酒记录（含 apply_id，供对账）
        Date now = new Date();
        WineStorageRecord record = new WineStorageRecord();
        record.setStorageId(ap.getStorageId());
        record.setApplyId(ap.getId());
        record.setTakeCount(ap.getTakeCount());
        record.setOperatorId(operatorId);
        record.setRemark(ap.getRemark());
        record.setTakeTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        wineStorageRecordMapper.insert(record);
        // 置申请已核销
        ap.setStatus(APPLY_CONFIRMED);
        ap.setConfirmTime(now);
        ap.setConfirmBy(operatorId);
        ap.setUpdateTime(now);
        wineTakeApplyMapper.updateById(ap);

        WineStorage st = wineStorageMapper.selectById(ap.getStorageId());
        Member m = ap.getMemberId() == null ? null : memberMapper.selectById(ap.getMemberId());
        return toApplyVo(ap, st, m);
    }

    /** 按取酒码取「有效待核销」申请；分档文案（不存在 / 已核销 / 已取消 / 已过期） */
    private WineTakeApply loadValidApplyByCode(String takeCode) {
        if (takeCode == null || takeCode.trim().isEmpty()) {
            throw new StoneCustomerException("取酒码不能为空");
        }
        wineTakeApplyMapper.expireOutdated();
        WineTakeApply ap = wineTakeApplyMapper.selectOne(new QueryWrapper<WineTakeApply>()
                .eq("take_code", takeCode.trim()).last("LIMIT 1"));
        if (ap == null) {
            throw new StoneCustomerException("取酒码不存在");
        }
        if (ap.getStatus() != null && ap.getStatus() == APPLY_CONFIRMED) {
            throw new StoneCustomerException("该取酒码已核销");
        }
        if (ap.getStatus() != null && ap.getStatus() == APPLY_CANCELLED) {
            throw new StoneCustomerException("该申请已取消");
        }
        boolean expired = ap.getStatus() != null && ap.getStatus() == APPLY_EXPIRED
                || (ap.getExpireTime() != null && ap.getExpireTime().before(new Date()));
        if (expired) {
            throw new StoneCustomerException("该取酒码已过期");
        }
        return ap;
    }

    /* ==================== 私有 ==================== */

    /** 该会员「有效待核销」取酒申请，按 storageId 建索引 */
    private Map<Long, WineTakeApply> pendingApplyMapByMember(Integer memberId) {
        List<WineTakeApply> list = wineTakeApplyMapper.selectList(new QueryWrapper<WineTakeApply>()
                .eq("member_id", memberId)
                .eq("status", APPLY_PENDING)
                .gt("expire_time", new Date()));
        Map<Long, WineTakeApply> map = new HashMap<>();
        for (WineTakeApply ap : list) {
            map.put(ap.getStorageId(), ap);
        }
        return map;
    }

    private Map<Long, WineStorage> storageMapByIds(List<Long> ids) {
        Map<Long, WineStorage> map = new HashMap<>();
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return map;
        }
        for (WineStorage ws : wineStorageMapper.selectBatchIds(distinct)) {
            map.put(ws.getId(), ws);
        }
        return map;
    }

    private Map<Integer, Member> memberMap2(List<Integer> ids) {
        Map<Integer, Member> map = new HashMap<>();
        List<Integer> distinct = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return map;
        }
        for (Member m : memberMapper.selectBatchIds(distinct)) {
            map.put(m.getId(), m);
        }
        return map;
    }

    private Map<Integer, String> shopNameMap(List<WineStorage> list) {
        Set<Integer> shopIds = list.stream()
                .map(WineStorage::getShopId)
                .filter(Objects::nonNull)
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
        vo.setLocation(ws.getLocation());
        vo.setStorageCode(ws.getStorageCode()); // 批次F · W-19：临期提醒列表要展示存酒码
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

        // 展示行：B 区 03 号柜 · 存入 08-20（柜位可空）
        StringBuilder line = new StringBuilder();
        if (ws.getLocation() != null && !ws.getLocation().trim().isEmpty()) {
            line.append(ws.getLocation().trim());
        }
        if (ws.getStorageDate() != null) {
            if (line.length() > 0) {
                line.append(" · ");
            }
            line.append("存入 ").append(new SimpleDateFormat("MM-dd").format(ws.getStorageDate()));
        }
        vo.setSummaryLine(line.toString());
        return vo;
    }

    private WineTakeApplyVo toApplyVo(WineTakeApply ap, WineStorage st, Member m) {
        WineTakeApplyVo vo = new WineTakeApplyVo();
        vo.setId(ap.getId());
        vo.setStorageId(ap.getStorageId());
        vo.setMemberId(ap.getMemberId());
        if (st != null) {
            vo.setGoodsName(st.getGoodsName());
            vo.setLocation(st.getLocation());
        }
        vo.setTakeCount(ap.getTakeCount());
        int status = ap.getStatus() == null ? APPLY_PENDING : ap.getStatus();
        vo.setStatusCode(status);
        vo.setStatusText(APPLY_STATUS_TEXT.get(status));
        // 取酒码仅「待核销」时对外展示
        vo.setTakeCode(status == APPLY_PENDING ? ap.getTakeCode() : null);
        vo.setApplySource(ap.getApplySource());
        vo.setApplySourceText(ap.getApplySource() != null && ap.getApplySource() == SOURCE_SHOP ? "商家代发起" : "顾客自助");
        vo.setApplyTime(fmtDateTime(ap.getApplyTime()));
        vo.setExpireTime(fmtDateTime(ap.getExpireTime()));
        vo.setConfirmTime(fmtDateTime(ap.getConfirmTime()));
        if (m != null) {
            vo.setMemberName(m.getNickname());
            vo.setMemberPhone(maskMobile(m.getMobile()));
        }
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

    private Date addHours(Date base, int hours) {
        Calendar c = Calendar.getInstance();
        c.setTime(base);
        c.add(Calendar.HOUR_OF_DAY, hours);
        return c.getTime();
    }

    private String fmtDate(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private String fmtDateTime(Date d) {
        return d == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d);
    }

    /** 生成唯一 6 位取酒码（100000–999999，首位非 0，撞码重试；uk_take_code 为 DB 层兜底） */
    private String generateUniqueTakeCode() {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            String code = String.valueOf(100000 + random.nextInt(900000));
            Integer count = wineTakeApplyMapper.selectCount(
                    new QueryWrapper<WineTakeApply>().eq("take_code", code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new StoneCustomerException("取酒码生成失败，请稍后重试");
    }
}
