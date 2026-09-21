package com.siam.system.modular.package_goods.service_impl.internal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.internal.PrintTask;
import com.siam.system.modular.package_goods.entity.internal.PrintTemplate;
import com.siam.system.modular.package_goods.mapper.internal.PrintTaskMapper;
import com.siam.system.modular.package_goods.mapper.internal.PrintTemplateMapper;
import com.siam.system.modular.package_goods.service.internal.PrintService;
import com.siam.system.modular.package_goods.entity.internal.Printer;
import com.siam.system.modular.package_goods.mapper.internal.PrinterMapper;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.modular.package_user.mapper.MerchantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印服务实现 · 批次 G · W-21 / W-25
 *
 * <p>设计取舍：列表**不写自定义 SQL/XML**，而是用 MP 的 {@code selectPage} 取实体、
 * 再在 Java 里组装 Map 并补打印机名/操作人名。原因：
 * ① 本项目自定义分页查询要配 @ResultMap + Page&lt;Map&gt;，出错点多（本季已踩过列名坑）；
 * ② 需要补的两个外键都在同库小表上，一次批量查即可，无 N+1。
 */
@Service
public class PrintServiceImpl implements PrintService {

    @Autowired
    private PrintTemplateMapper printTemplateMapper;

    @Autowired
    private PrintTaskMapper printTaskMapper;

    @Autowired
    private PrinterMapper printerMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    /* ==================== 模板（W-21） ==================== */

    @Override
    public List<PrintTemplate> listTemplates(Integer shopId, Integer scene) {
        QueryWrapper<PrintTemplate> qw = new QueryWrapper<>();
        qw.eq("shop_id", shopId);
        if (scene != null) {
            qw.eq("scene", scene);
        }
        qw.orderByDesc("is_default").orderByAsc("id");
        return printTemplateMapper.selectList(qw);
    }

    @Override
    public PrintTemplate saveTemplate(Integer shopId, PrintTemplate t) {
        if (t == null || t.getScene() == null) {
            throw new StoneCustomerException("模板类型不能为空");
        }
        int width = t.getPaperWidth() == null ? 58 : t.getPaperWidth();
        if (width != 58 && width != 80) {
            throw new StoneCustomerException("小票规格只支持 58 或 80mm");
        }
        if (t.getId() == null) {
            t.setId(null);
            t.setShopId(shopId);
            t.setCopies(t.getCopies() == null || t.getCopies() < 1 ? 1 : t.getCopies());
            t.setShowQr(t.getShowQr() == null ? Boolean.TRUE : t.getShowQr());
            t.setIsDefault(Boolean.FALSE);
            t.setCreateTime(new Date());
            t.setUpdateTime(new Date());
            printTemplateMapper.insert(t);
            return t;
        }
        PrintTemplate db = printTemplateMapper.selectById(t.getId());
        if (db == null || !shopId.equals(db.getShopId())) {
            throw new StoneCustomerException("该模板不存在");
        }
        t.setShopId(shopId);
        t.setUpdateTime(new Date());
        printTemplateMapper.updateById(t);
        return printTemplateMapper.selectById(t.getId());
    }

    @Override
    public void setDefaultTemplate(Integer shopId, Integer id) {
        PrintTemplate db = printTemplateMapper.selectById(id);
        if (db == null || !shopId.equals(db.getShopId())) {
            throw new StoneCustomerException("该模板不存在");
        }
        // 同 shop + scene 下互斥：先把同类全部置非默认，再置本条为默认
        List<PrintTemplate> same = listTemplates(shopId, db.getScene());
        for (PrintTemplate t : same) {
            PrintTemplate update = new PrintTemplate();
            update.setId(t.getId());
            update.setIsDefault(Boolean.FALSE);
            update.setUpdateTime(new Date());
            printTemplateMapper.updateById(update);
        }
        PrintTemplate set = new PrintTemplate();
        set.setId(id);
        set.setIsDefault(Boolean.TRUE);
        set.setUpdateTime(new Date());
        printTemplateMapper.updateById(set);
    }

    @Override
    public void deleteTemplate(Integer shopId, Integer id) {
        PrintTemplate db = printTemplateMapper.selectById(id);
        if (db == null || !shopId.equals(db.getShopId())) {
            throw new StoneCustomerException("该模板不存在");
        }
        if (Boolean.TRUE.equals(db.getIsDefault())) {
            throw new StoneCustomerException("默认模板不可删除，请先把其它模板设为默认");
        }
        printTemplateMapper.deleteById(id);
    }

    /* ==================== 任务（W-25 / W-21 补打记录） ==================== */

    @Override
    public Page<Map<String, Object>> listTasks(Integer shopId, Integer status, String orderNo,
                                               Boolean onlyPendingOrFail, Boolean isReprint,
                                               int pageNo, int pageSize) {
        QueryWrapper<PrintTask> qw = new QueryWrapper<>();
        qw.eq("shop_id", shopId);
        if (status != null) {
            qw.eq("status", status);
        }
        if (onlyPendingOrFail != null && onlyPendingOrFail) {
            qw.in("status", PrintTask.STATUS_WAIT, PrintTask.STATUS_FAIL);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            qw.like("order_no", orderNo.trim());
        }
        if (isReprint != null) {
            qw.eq("is_reprint", isReprint);
        }
        qw.orderByDesc("id");

        Page<PrintTask> src = printTaskMapper.selectPage(new Page<>(pageNo, pageSize), qw);

        // 组装：实体 → Map（驼峰键），并补打印机名 / 操作人名
        List<PrintTask> records = src.getRecords() == null ? new ArrayList<>() : src.getRecords();
        Map<Integer, String> printerNames = printerNameMap(records);
        Map<Integer, String> operatorNames = operatorNameMap(records);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (PrintTask t : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("orderNo", t.getOrderNo());
            m.put("ticketType", t.getTicketType());
            m.put("printerId", t.getPrinterId());
            m.put("printerName", t.getPrinterId() == null ? null : printerNames.get(t.getPrinterId()));
            m.put("copies", t.getCopies());
            m.put("status", t.getStatus());
            m.put("retryCount", t.getRetryCount());
            m.put("maxRetry", t.getMaxRetry());
            m.put("failReason", t.getFailReason());
            m.put("dispatchTime", t.getDispatchTime());
            m.put("nextRetryTime", t.getNextRetryTime());
            m.put("printTime", t.getPrintTime());
            m.put("isReprint", t.getIsReprint());
            m.put("operatorId", t.getOperatorId());
            m.put("operatorName", t.getOperatorId() == null ? null : operatorNames.get(t.getOperatorId()));
            rows.add(m);
        }

        Page<Map<String, Object>> out = new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        out.setRecords(rows);
        return out;
    }

    @Override
    public Map<String, Object> taskSummary(Integer shopId) {
        Map<String, Object> m = new HashMap<>();
        QueryWrapper<PrintTask> pending = new QueryWrapper<>();
        pending.eq("shop_id", shopId).in("status", PrintTask.STATUS_WAIT, PrintTask.STATUS_FAIL);
        long waitRetry = printTaskMapper.selectCount(pending);

        // 今日失败 / 今日成功：用 create_time 的日期部分（数据库侧当天）
        long todayFail = printTaskMapper.selectCount(new QueryWrapper<PrintTask>()
                .eq("shop_id", shopId).eq("status", PrintTask.STATUS_FAIL)
                .apply("date(create_time) = curdate()"));
        long todaySuccess = printTaskMapper.selectCount(new QueryWrapper<PrintTask>()
                .eq("shop_id", shopId).eq("status", PrintTask.STATUS_SUCCESS)
                .apply("date(create_time) = curdate()"));

        m.put("waitRetry", waitRetry);
        m.put("todayFail", todayFail);
        m.put("todaySuccess", todaySuccess);
        return m;
    }

    @Override
    public boolean retryTask(Integer shopId, Integer taskId) {
        PrintTask db = printTaskMapper.selectById(taskId);
        if (db == null || !shopId.equals(db.getShopId())) {
            throw new StoneCustomerException("该打印任务不存在");
        }
        if (db.getStatus() != null && db.getStatus() == PrintTask.STATUS_SUCCESS) {
            throw new StoneCustomerException("该任务已打印成功，无需重试");
        }
        if (db.getStatus() != null && db.getStatus() == PrintTask.STATUS_IGNORED) {
            throw new StoneCustomerException("该任务已被忽略，如需打印请使用「补打」");
        }
        PrintTask update = new PrintTask();
        update.setId(taskId);
        // 置回「待打印」，把下次重试时间提前到现在，由打印执行器（待云打印账号就绪后接入）消费
        update.setStatus(PrintTask.STATUS_WAIT);
        update.setNextRetryTime(new Date());
        update.setFailReason(null);
        update.setUpdateTime(new Date());
        return printTaskMapper.updateById(update) > 0;
    }

    @Override
    public boolean ignoreTask(Integer shopId, Integer taskId) {
        PrintTask db = printTaskMapper.selectById(taskId);
        if (db == null || !shopId.equals(db.getShopId())) {
            throw new StoneCustomerException("该打印任务不存在");
        }
        PrintTask update = new PrintTask();
        update.setId(taskId);
        update.setStatus(PrintTask.STATUS_IGNORED);
        update.setUpdateTime(new Date());
        return printTaskMapper.updateById(update) > 0;
    }

    @Override
    public PrintTask createReprint(Integer shopId, Integer operatorId, Integer sourceTaskId) {
        PrintTask src = printTaskMapper.selectById(sourceTaskId);
        if (src == null || !shopId.equals(src.getShopId())) {
            throw new StoneCustomerException("原打印任务不存在，无法补打");
        }
        PrintTask t = new PrintTask();
        t.setShopId(shopId);
        t.setOrderNo(src.getOrderNo());
        t.setTicketType(src.getTicketType());
        t.setPrinterId(src.getPrinterId());
        t.setTemplateId(src.getTemplateId());
        t.setCopies(src.getCopies() == null ? 1 : src.getCopies());
        t.setContent(src.getContent());
        t.setStatus(PrintTask.STATUS_WAIT);
        t.setRetryCount(0);
        t.setMaxRetry(src.getMaxRetry() == null ? 3 : src.getMaxRetry());
        t.setDispatchTime(new Date());
        t.setNextRetryTime(new Date());
        t.setOperatorId(operatorId);
        t.setIsReprint(Boolean.TRUE);
        t.setCreateTime(new Date());
        t.setUpdateTime(new Date());
        printTaskMapper.insert(t);
        return t;
    }

    /* ==================== 私有：批量补名 ==================== */

    private Map<Integer, String> printerNameMap(List<PrintTask> records) {
        List<Integer> ids = new ArrayList<>();
        for (PrintTask t : records) {
            if (t.getPrinterId() != null && !ids.contains(t.getPrinterId())) {
                ids.add(t.getPrinterId());
            }
        }
        Map<Integer, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Printer p : printerMapper.selectBatchIds(ids)) {
            map.put(p.getId(), p.getName());
        }
        return map;
    }

    private Map<Integer, String> operatorNameMap(List<PrintTask> records) {
        List<Integer> ids = new ArrayList<>();
        for (PrintTask t : records) {
            if (t.getOperatorId() != null && !ids.contains(t.getOperatorId())) {
                ids.add(t.getOperatorId());
            }
        }
        Map<Integer, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Merchant m : merchantMapper.selectBatchIds(ids)) {
            map.put(m.getId(), m.getNickname() != null && !m.getNickname().isEmpty() ? m.getNickname() : m.getRealName());
        }
        return map;
    }
}
