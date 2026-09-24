package com.siam.system.modular.package_dining.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_dining.entity.DiningSession;
import com.siam.system.modular.package_dining.entity.DiningTable;
import com.siam.system.modular.package_dining.entity.TableType;
import com.siam.system.modular.package_dining.entity.TableZone;
import com.siam.system.modular.package_dining.mapper.DiningSessionMapper;
import com.siam.system.modular.package_dining.mapper.DiningTableMapper;
import com.siam.system.modular.package_dining.mapper.TableTypeMapper;
import com.siam.system.modular.package_dining.mapper.TableZoneMapper;
import com.siam.system.modular.package_dining.model.vo.TableFloorVo;
import com.siam.system.modular.package_dining.service.DiningTableService;
import com.siam.system.modular.package_dining.service.WxScanPayService;
import com.siam.system.modular.package_order.entity.Order;
import com.siam.system.modular.package_order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiningTableServiceImpl implements DiningTableService {

    /** 收款方式：微信收款码（tb_order.payment_mode=1，与 WxPayController 的「微信支付」同档） */
    private static final int PAY_MODE_WX_QRCODE = 1;

    /** 收款方式：现金 → 到店支付（tb_order.payment_mode=4；对齐 A-07 营收看板「为空视为 4」口径） */
    private static final int PAY_MODE_OFFLINE = 4;

    @Autowired
    private DiningTableMapper diningTableMapper;

    @Autowired
    private TableTypeMapper tableTypeMapper;

    @Autowired
    private TableZoneMapper tableZoneMapper;

    @Autowired
    private DiningSessionMapper diningSessionMapper;

    @Autowired
    private OrderMapper orderMapper;

    /** 付款码支付（被扫）—— 收银台扫顾客付款码真扣款 */
    @Autowired
    private WxScanPayService wxScanPayService;

    @Override
    public Map<String, Object> list(Integer shopId) {
        // 台位全量（不分页，单店桌数小；Round 9）；停用台位（status=3）收银端不显示（A-09 作废/撤除语义）
        List<DiningTable> tables = diningTableMapper.selectList(
                new LambdaQueryWrapper<DiningTable>()
                        .eq(DiningTable::getShopId, shopId)
                        .ne(DiningTable::getStatus, 3));

        // 类型/区域映射表
        Map<Integer, TableType> typeMap = tableTypeMapper.selectList(
                        new LambdaQueryWrapper<TableType>().eq(TableType::getShopId, shopId))
                .stream().collect(Collectors.toMap(TableType::getId, Function.identity(), (a, b) -> a));
        Map<Integer, TableZone> zoneMap = tableZoneMapper.selectList(
                        new LambdaQueryWrapper<TableZone>().eq(TableZone::getShopId, shopId))
                .stream().collect(Collectors.toMap(TableZone::getId, Function.identity(), (a, b) -> a));

        // 会话按 tableId 分组：**进行中(1) + 已结台(2) 都要**，用于派生「待清台」（批次 C 2026-09-19）
        Map<Long, List<DiningSession>> sessionMap = diningSessionMapper.selectList(
                        new LambdaQueryWrapper<DiningSession>()
                                .eq(DiningSession::getShopId, shopId)
                                .in(DiningSession::getStatus, 1, 2))
                .stream().collect(Collectors.groupingBy(DiningSession::getTableId));

        long now = System.currentTimeMillis();
        List<TableFloorVo> voList = tables.stream().map(t -> {
            TableFloorVo vo = new TableFloorVo();
            vo.setId(t.getId());
            vo.setTableNo(t.getTableNo());
            vo.setTableName(t.getTableName());
            vo.setCapacity(t.getCapacity());
            vo.setStatus(t.getStatus());
            vo.setAwaitingClean(false);
            TableType type = typeMap.get(t.getTypeId());
            if (type != null) {
                vo.setTypeName(type.getName());
                vo.setMinSpend(type.getMinConsume());
            }
            TableZone zone = zoneMap.get(t.getZoneId());
            if (zone != null) {
                vo.setZone(zone.getName());
            }
            // 代表会话：优先「进行中」，否则最近一次「已结台」
            DiningSession s = pickSession(sessionMap.get(t.getId()));
            if (s != null) {
                vo.setSessionId(s.getId());
                vo.setSessionStatus(s.getStatus());
                vo.setMemberNum(s.getMemberNum());
                if (s.getOpenTime() != null) {
                    vo.setOpenMinutes(Math.max(0, (now - s.getOpenTime().getTime()) / 60000));
                }
                if (s.getCloseTime() != null) {
                    vo.setCloseTime(s.getCloseTime());
                }
            }
            // 待清台 = 台位仍占用(2) 且 最近会话已结台(2)
            vo.setAwaitingClean(t.getStatus() != null && t.getStatus() == 2
                    && s != null && s.getStatus() != null && s.getStatus() == 2);
            return vo;
        }).collect(Collectors.toList());

        List<TableZone> zoneList = tableZoneMapper.selectList(
                new LambdaQueryWrapper<TableZone>().eq(TableZone::getShopId, shopId));

        Map<String, Object> data = new HashMap<>();
        data.put("tables", voList);
        data.put("zoneList", zoneList);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> openTable(Integer shopId, Long tableId, Integer memberNum) {
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null || !shopId.equals(table.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        // 人数校验：1 ~ capacity（capacity 由管理端 A-09 档案设置，Round 10）
        if (memberNum == null || memberNum < 1 || memberNum > table.getCapacity()) {
            throw new StoneCustomerException("就餐人数需在 1~" + table.getCapacity() + " 之间");
        }
        // 原子抢台：空闲→占用，返回 0 说明已被并发抢占
        if (diningTableMapper.occupyById(tableId, shopId) != 1) {
            throw new StoneCustomerException("该台位已被占用");
        }
        // 建会话（收银开台无会员）
        DiningSession session = new DiningSession();
        session.setShopId(shopId);
        session.setTableId(tableId);
        session.setMemberId(null);
        session.setMemberNum(memberNum);
        session.setStatus(1);
        session.setOpenTime(new Date());
        diningSessionMapper.insert(session);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getId());
        data.put("tableId", tableId);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTable(Integer shopId, Long tableId, Long targetTableId) {
        if (tableId == null || targetTableId == null || tableId.equals(targetTableId)) {
            throw new StoneCustomerException("转台参数错误");
        }
        // ① 源台须有进行中会话
        DiningSession session = diningSessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 1));
        if (session == null) {
            throw new StoneCustomerException("源台无进行中会话");
        }
        // ② 原子抢目标台（空闲→占用），失败即抛，事务回滚
        if (diningTableMapper.occupyById(targetTableId, shopId) != 1) {
            throw new StoneCustomerException("目标台位不可用或已被占用");
        }
        DiningTable target = diningTableMapper.selectById(targetTableId);
        // ③ 会话迁移到目标台
        DiningSession upd = new DiningSession();
        upd.setId(session.getId());
        upd.setTableId(targetTableId);
        diningSessionMapper.updateById(upd);
        // ④ 迁移会话下全部堂食订单快照列（Round 2：只改快照，不动支付流水）
        orderMapper.migrateSessionTables(session.getId(), targetTableId, target.getTableNo(), target.getTableName());
        // ⑤ 释放源台（占用→空闲）；异常时事务整体回滚，目标台自动还原
        diningTableMapper.releaseById(tableId, shopId);
    }

    /**
     * 结账（批次 C 2026-09-19 新增）：只**闭台**，**台位保持占用** → 进入「待清台」
     *
     * <p>与 {@link #clearTable} 的分工（对齐 W-04 画板底注
     * 「收款后：订单→已完成 · 台位→待清台 · 自动打印结账小票（P-03）」）：
     * <ol>
     *   <li>收款完成 → 调本方法：会话置「已结台」+ 结台时间，台位**不动**（= 待清台）</li>
     *   <li>清台 → 调 {@link #clearTable}：仅释放台位</li>
     * </ol>
     * ⚠️ W-04 结算栏的「确认收款」最终应串起「下单 → 收款 → settle → 待清台」这条链路
     * （真实支付属批次 H，商户号就绪前该按钮先只做下单）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleTable(Integer shopId, Long tableId) {
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null || !shopId.equals(table.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        DiningSession session = inProgressSession(tableId);
        if (session == null) {
            throw new StoneCustomerException("该台位无进行中会话");
        }
        assertNoUnpaidOrder(session.getId());
        DiningSession upd = new DiningSession();
        upd.setId(session.getId());
        upd.setStatus(2);
        upd.setCloseTime(new Date());
        diningSessionMapper.updateById(upd);
        // 注意：此处**不调用 releaseById** —— 台位保持占用即「待清台」
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmPayment(Integer shopId, Long tableId, String orderNo, Integer payMode) {
        // ① 收款方式校验：画板 W-04 结算栏仅两项（92:3 微信收款码 / 92:7 现金）
        if (payMode == null || (payMode != PAY_MODE_WX_QRCODE && payMode != PAY_MODE_OFFLINE)) {
            throw new StoneCustomerException("收款方式不正确");
        }

        int paidCount;
        BigDecimal paidAmount;

        if (tableId != null) {
            // 堂食：按台位进行中会话收全桌未付订单（含之前漏收的、以及本次加酒）
            DiningTable table = diningTableMapper.selectById(tableId);
            if (table == null || !shopId.equals(table.getShopId())) {
                throw new StoneCustomerException("台位不存在");
            }
            DiningSession session = inProgressSession(tableId);
            if (session == null) {
                throw new StoneCustomerException("该台位无进行中会话，无法收款");
            }
            // 金额须在 UPDATE 之前统计（更新后 is_payment 已置真，就查不到了）
            paidAmount = sumUnpaidBySession(session.getId());
            paidCount = orderMapper.payUnpaidBySession(session.getId(), payMode);
        } else if (orderNo != null && !orderNo.trim().isEmpty()) {
            // 外带自提/散客：无台位无会话，按订单号收
            Order dbOrder = orderMapper.selectByOrderNo(orderNo.trim());
            if (dbOrder == null || !shopId.equals(dbOrder.getShopId())) {
                throw new StoneCustomerException("订单不存在");
            }
            paidAmount = dbOrder.getActualPrice() == null ? BigDecimal.ZERO : dbOrder.getActualPrice();
            boolean alreadyPaid = Boolean.TRUE.equals(dbOrder.getIsPayment());
            paidCount = orderMapper.payUnpaidByOrderNo(orderNo.trim(), shopId, payMode);
            if (paidCount == 0 || alreadyPaid) {
                paidAmount = BigDecimal.ZERO;
            }
        } else {
            throw new StoneCustomerException("请提供台位id或订单号");
        }

        // 幂等：重复点「确认收款」不报错，paidCount=0 由前端提示「已收过」
        if (paidCount == 0) {
            paidAmount = BigDecimal.ZERO;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paidCount", paidCount);
        data.put("paidAmount", paidAmount);
        data.put("payMode", payMode);
        return data;
    }

    // ==================== 扫码收款（付款码支付 · 被扫） ====================

    /**
     * 扫码收款：扫顾客微信付款码 → 微信 /pay/micropay 真扣款 → 落库
     * 详见 {@link DiningTableService#scanPay} 的三态语义与「为何不开事务」。
     * ⚠️ 刻意不加 @Transactional：内部有 HTTP 调用与 USERPAYING 轮询（最长数秒），
     *    持事务会白占连接池；落库只有一条原子 UPDATE，本身无需事务。
     */
    @Override
    public Map<String, Object> scanPay(Integer shopId, Long tableId, String orderNo, String authCode, String clientIp) {
        // ① 付款码弱校验：微信付款码为纯数字（常见 18 位，以 10~15 开头）。
        //    这里只挡明显不是码的输入，真正的有效性由微信判定（避免本地过严误拦）
        if (authCode == null || authCode.trim().isEmpty()) {
            throw new StoneCustomerException("请扫描顾客付款码");
        }
        String code = authCode.trim();
        if (!code.matches("\\d{10,24}")) {
            throw new StoneCustomerException("付款码格式不正确（应为纯数字），请重新扫描");
        }

        // ② 定位待收订单
        Order target = pickUnpaidOrder(shopId, tableId, orderNo);
        if (target == null) {
            throw new StoneCustomerException("该台位没有待收款的订单");
        }
        if (Boolean.TRUE.equals(target.getIsPayment())) {
            throw new StoneCustomerException("该订单已收款，无需重复收款");
        }
        BigDecimal amount = target.getActualPrice() == null ? BigDecimal.ZERO : target.getActualPrice();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoneCustomerException("订单金额为 0，无法发起付款码支付");
        }

        // ③ 发起付款码支付
        String body = "堂食消费-" + (target.getTableNo() == null ? target.getOrderNo() : target.getTableNo());
        Map<String, String> wx = wxScanPayService.micropay(target.getOrderNo(), amount, body, code, clientIp);

        if (!"SUCCESS".equals(wx.get("return_code"))) {
            // 通信层失败（签名错误 / 商户号不存在 / IP 非法 等）—— 原文透传便于定位
            throw new StoneCustomerException("微信支付通信失败：" + nvl(wx.get("return_msg"), "未知原因"));
        }

        if ("SUCCESS".equals(wx.get("result_code"))) {
            return markPaidByScan(target, wx, amount);
        }

        // ④ USERPAYING（顾客还在输密码）→ 主动查单兜底，避免收银员干等
        String errCode = wx.get("err_code");
        if ("USERPAYING".equals(errCode)) {
            for (int i = 0; i < 2; i++) {
                sleepQuietly(1500L);
                Map<String, String> q = wxScanPayService.orderQuery(target.getOrderNo());
                if ("SUCCESS".equals(q.get("trade_state"))) {
                    return markPaidByScan(target, q, amount);
                }
                String state = q.get("trade_state");
                if (state != null && !"USERPAYING".equals(state) && !"NOTPAY".equals(state)) {
                    // 明确的终态（CLOSED / REVOKED / PAYERROR ...）
                    throw new StoneCustomerException("支付未完成：" + nvl(q.get("trade_state_desc"), state));
                }
            }
            // 仍在支付中：**不改库**，让收银员再扫一次（微信对同单号同参数幂等，重扫安全）
            Map<String, Object> data = new HashMap<>();
            data.put("status", "USERPAYING");
            data.put("outTradeNo", target.getOrderNo());
            data.put("message", "顾客正在输入支付密码，请稍候片刻后再次扫描同一付款码确认");
            return data;
        }

        // ⑤ 其他失败：透传微信文案（「付款码已过期」/「余额不足」/「商户号未开通付款码支付」...）
        throw new StoneCustomerException("收款失败：" + nvl(wx.get("err_code_des"), nvl(errCode, "未知原因")));
    }

    /** 扫码支付成功后落库（payment_mode=1 微信） */
    private Map<String, Object> markPaidByScan(Order target, Map<String, String> wx, BigDecimal amount) {
        int rows = orderMapper.payUnpaidByOrderNo(target.getOrderNo(), target.getShopId(), PAY_MODE_WX_QRCODE);
        Map<String, Object> data = new HashMap<>();
        data.put("status", "SUCCESS");
        data.put("outTradeNo", target.getOrderNo());
        data.put("transactionId", wx.get("transaction_id"));
        data.put("paidAmount", rows > 0 ? amount : BigDecimal.ZERO);
        return data;
    }

    /**
     * 取待收订单：优先 orderNo（本次刚下的单 / 外带单），否则取该台位会话下**最早一笔**未付堂食单
     * 注：付款码支付是「按订单号 + 金额」发起的，无法把多笔合并成一笔向微信下单，
     * 故一桌有多笔未付时一次只收一笔，由前端提示剩余笔数。
     */
    private Order pickUnpaidOrder(Integer shopId, Long tableId, String orderNo) {
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            Order o = orderMapper.selectByOrderNo(orderNo.trim());
            if (o == null || !shopId.equals(o.getShopId())) {
                throw new StoneCustomerException("订单不存在");
            }
            return o;
        }
        if (tableId == null) {
            throw new StoneCustomerException("请提供台位id或订单号");
        }
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null || !shopId.equals(table.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        DiningSession session = inProgressSession(tableId);
        if (session == null) {
            throw new StoneCustomerException("该台位无进行中会话，无法收款");
        }
        List<Order> unpaid = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getDiningSessionId, session.getId().intValue())
                .eq(Order::getOrderScene, 1)
                .eq(Order::getIsPayment, false)
                .orderByAsc(Order::getId));
        return unpaid.isEmpty() ? null : unpaid.get(0);
    }

    private String nvl(String v, String fallback) {
        return (v == null || v.trim().isEmpty()) ? fallback : v;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 统计会话下未付堂食订单金额合计（confirmPayment 用于回显收款金额） */
    private BigDecimal sumUnpaidBySession(Long sessionId) {
        List<Order> list = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getDiningSessionId, sessionId.intValue())
                .eq(Order::getOrderScene, 1)
                .eq(Order::getIsPayment, false));
        BigDecimal sum = BigDecimal.ZERO;
        for (Order o : list) {
            if (o.getActualPrice() != null) {
                sum = sum.add(o.getActualPrice());
            }
        }
        return sum;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearTable(Integer shopId, Long tableId) {
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null || !shopId.equals(table.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        DiningSession session = inProgressSession(tableId);
        if (session != null) {
            // 常规路径：仍有进行中会话 → 清台一并结台（拦截未结账订单，Round 3 拍板）
            assertNoUnpaidOrder(session.getId());
            DiningSession upd = new DiningSession();
            upd.setId(session.getId());
            upd.setStatus(2);
            upd.setCloseTime(new Date());
            diningSessionMapper.updateById(upd);
        }
        // 待清台路径：会话已由 settleTable 置为「已结台」→ 本次只放台。
        // 同时覆盖「台位占用却无任何会话」的脏数据，避免台位永久卡死。
        diningTableMapper.releaseById(tableId, shopId);
    }

    /** 取该台位进行中的会话（无则 null） */
    private DiningSession inProgressSession(Long tableId) {
        return diningSessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 1));
    }

    /** 未结账堂食订单拦截（settle 与 clear 同口径） */
    private void assertNoUnpaidOrder(Long sessionId) {
        Integer unpaid = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getDiningSessionId, sessionId.intValue())
                .eq(Order::getOrderScene, 1)
                .eq(Order::getIsPayment, false));
        if (unpaid != null && unpaid > 0) {
            throw new StoneCustomerException("该台位存在未结账订单，请先结账");
        }
    }

    /**
     * 取台位的代表会话：优先「进行中」，否则取最近一次「已结台」
     * 取不到进行中但存在已结台 ⇒ 该台位已结账未清台（待清台）
     */
    private DiningSession pickSession(List<DiningSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        for (DiningSession s : sessions) {
            if (s.getStatus() != null && s.getStatus() == 1) {
                return s;
            }
        }
        return sessions.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 2)
                .max(Comparator.comparing(DiningSession::getCloseTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }
}
