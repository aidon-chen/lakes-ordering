package com.siam.system.modular.package_dining.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_weixin_basic.util.WxQrCodeUtils;
import com.siam.system.modular.package_dining.entity.DiningTable;
import com.siam.system.modular.package_dining.entity.TableType;
import com.siam.system.modular.package_dining.entity.TableZone;
import com.siam.system.modular.package_dining.mapper.DiningTableMapper;
import com.siam.system.modular.package_dining.mapper.TableTypeMapper;
import com.siam.system.modular.package_dining.mapper.TableZoneMapper;
import com.siam.system.modular.package_dining.model.param.DiningTableParam;
import com.siam.system.modular.package_dining.service.TableManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 台位档案管理服务实现（A-09）
 * 状态口径：1=空闲 2=占用 3=停用；作废码=清 qr 字段（可重生成），停用=status=3
 */
@Slf4j
@Service
public class TableManageServiceImpl implements TableManageService {

    /** 扫码开台页（getwxacodeunlimit 的 page 参数，需客户端已发布） */
    private static final String QR_PAGE = "pages/menu/scan-table/scan-table";

    /**
     * 桌码图内存缓存（tableNo → PNG 字节）
     * 不落对象存储（本期未采购 OSS）：微信小程序码永久有效、可无限重建，容器重启后按 qr_scene 现场重建
     */
    private static final Map<String, byte[]> QR_CACHE = new ConcurrentHashMap<>();

    @Autowired
    private DiningTableMapper diningTableMapper;

    @Autowired
    private TableTypeMapper tableTypeMapper;

    @Autowired
    private TableZoneMapper tableZoneMapper;

    @Autowired
    private WxQrCodeUtils wxQrCodeUtils;

    /* ==================== 台位档案 ==================== */

    @Override
    public Page<Map<String, Object>> listTable(Integer shopId, DiningTableParam param) {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                // 停用台位仅管理端可见（收银端由 list 接口过滤）
                .orderByAsc(DiningTable::getTableNo);
        if (param.getTableNo() != null && !param.getTableNo().isEmpty()) {
            wrapper.like(DiningTable::getTableNo, param.getTableNo());
        }
        if (param.getTableName() != null && !param.getTableName().isEmpty()) {
            wrapper.like(DiningTable::getTableName, param.getTableName());
        }
        if (param.getTypeId() != null) {
            wrapper.eq(DiningTable::getTypeId, param.getTypeId());
        }
        if (param.getZoneId() != null) {
            wrapper.eq(DiningTable::getZoneId, param.getZoneId());
        }
        if (param.getStatus() != null) {
            wrapper.eq(DiningTable::getStatus, param.getStatus());
        }
        Page<DiningTable> page = diningTableMapper.selectPage(
                new Page<>(param.getPageNo(), param.getPageSize()), wrapper);

        // 类型/区域映射（补充展示字段）
        Map<Integer, TableType> typeMap = tableTypeMapper.selectList(
                        new LambdaQueryWrapper<TableType>().eq(TableType::getShopId, shopId))
                .stream().collect(Collectors.toMap(TableType::getId, Function.identity(), (a, b) -> a));
        Map<Integer, TableZone> zoneMap = tableZoneMapper.selectList(
                        new LambdaQueryWrapper<TableZone>().eq(TableZone::getShopId, shopId))
                .stream().collect(Collectors.toMap(TableZone::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> records = page.getRecords().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("tableNo", t.getTableNo());
            m.put("tableName", t.getTableName());
            m.put("typeId", t.getTypeId());
            m.put("zoneId", t.getZoneId());
            m.put("capacity", t.getCapacity());
            m.put("status", t.getStatus());
            m.put("qrScene", t.getQrScene());
            m.put("qrCodeUrl", t.getQrCodeUrl());
            TableType type = typeMap.get(t.getTypeId());
            if (type != null) {
                m.put("typeName", type.getName());
                // 低消仅展示（V7 取消校验）
                m.put("minConsume", type.getMinConsume());
            }
            TableZone zone = zoneMap.get(t.getZoneId());
            if (zone != null) {
                m.put("zoneName", zone.getName());
            }
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public void addTable(Integer shopId, DiningTable table) {
        // 必填校验
        if (table.getTableNo() == null || table.getTableNo().trim().isEmpty()
                || table.getTableName() == null || table.getTableName().trim().isEmpty()) {
            throw new StoneCustomerException("桌号与桌名必填");
        }
        if (table.getTypeId() == null || table.getZoneId() == null) {
            throw new StoneCustomerException("台位类型与区域必选");
        }
        if (table.getCapacity() == null || table.getCapacity() < 1) {
            throw new StoneCustomerException("容纳人数需 ≥1");
        }
        // 桌号店内唯一（uk_shop_table_no）
        Integer exists = diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .eq(DiningTable::getTableNo, table.getTableNo().trim()));
        if (exists != null && exists > 0) {
            throw new StoneCustomerException("桌号 " + table.getTableNo() + " 已存在");
        }
        DiningTable insert = new DiningTable();
        insert.setShopId(shopId);
        insert.setTableNo(table.getTableNo().trim());
        insert.setTableName(table.getTableName().trim());
        insert.setTypeId(table.getTypeId());
        insert.setZoneId(table.getZoneId());
        insert.setCapacity(table.getCapacity());
        insert.setStatus(1); // 新建默认空闲
        insert.setCreateTime(new Date());
        insert.setUpdateTime(new Date());
        diningTableMapper.insert(insert);
    }

    @Override
    public void updateTable(Integer shopId, DiningTable table) {
        DiningTable exist = diningTableMapper.selectById(table.getId());
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        // 换桌号时校验唯一
        if (table.getTableNo() != null && !table.getTableNo().trim().isEmpty()
                && !table.getTableNo().trim().equals(exist.getTableNo())) {
            Integer cnt = diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                    .eq(DiningTable::getShopId, shopId)
                    .eq(DiningTable::getTableNo, table.getTableNo().trim()));
            if (cnt != null && cnt > 0) {
                throw new StoneCustomerException("桌号 " + table.getTableNo() + " 已存在");
            }
        }
        DiningTable upd = new DiningTable();
        upd.setId(exist.getId());
        if (table.getTableNo() != null && !table.getTableNo().trim().isEmpty()) {
            upd.setTableNo(table.getTableNo().trim());
        }
        if (table.getTableName() != null && !table.getTableName().trim().isEmpty()) {
            upd.setTableName(table.getTableName().trim());
        }
        if (table.getTypeId() != null) {
            upd.setTypeId(table.getTypeId());
        }
        if (table.getZoneId() != null) {
            upd.setZoneId(table.getZoneId());
        }
        if (table.getCapacity() != null && table.getCapacity() >= 1) {
            upd.setCapacity(table.getCapacity());
        }
        upd.setUpdateTime(new Date());
        diningTableMapper.updateById(upd);
    }

    @Override
    public void disableTable(Integer shopId, Long id) {
        DiningTable exist = diningTableMapper.selectById(id);
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        // 占用中不允许停用（收银端进行中会话保护）
        if (exist.getStatus() != null && exist.getStatus() == 2) {
            throw new StoneCustomerException("台位占用中，请先清台再停用");
        }
        DiningTable upd = new DiningTable();
        upd.setId(id);
        upd.setStatus(3); // 停用：扫码失效、收银端不显示
        upd.setUpdateTime(new Date());
        diningTableMapper.updateById(upd);
    }

    @Override
    public void enableTable(Integer shopId, Long id) {
        DiningTable exist = diningTableMapper.selectById(id);
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        DiningTable upd = new DiningTable();
        upd.setId(id);
        upd.setStatus(1);
        upd.setUpdateTime(new Date());
        diningTableMapper.updateById(upd);
    }

    /* ==================== 桌面二维码 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateQrcode(Integer shopId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new StoneCustomerException("请选择台位");
        }
        for (Long id : ids) {
            DiningTable table = diningTableMapper.selectById(id);
            if (table == null || !shopId.equals(table.getShopId())) {
                throw new StoneCustomerException("台位不存在：id=" + id);
            }
            // 停用台位不允许生成（作废/撤除语义）
            if (table.getStatus() != null && table.getStatus() == 3) {
                throw new StoneCustomerException("台位 " + table.getTableNo() + " 已停用，请先启用");
            }
            generateOneQrcode(shopId, table);
        }
    }

    /**
     * 单个台位码生成：微信码 → 白底叠加桌号文字 → 缓存内存 → 回写 qr_scene
     * 说明：不落对象存储（本期未采购 OSS）。微信小程序码永久有效、数量不限，可随时按需重建，
     * 故内存缓存即可满足缩略图/下载；容器重启缓存丢失后，首次取图自动重建。
     */
    private void generateOneQrcode(Integer shopId, DiningTable table) {
        String scene = "tid=" + table.getTableNo();
        try {
            byte[] png = fetchAndCompose(table.getTableNo(), scene);
            QR_CACHE.put(table.getTableNo(), png);
            // qr_scene 即"已生成且有效"标记；同时清掉历史 OSS 地址（用 UpdateWrapper 以支持显式 set null）
            diningTableMapper.update(null, new LambdaUpdateWrapper<DiningTable>()
                    .eq(DiningTable::getId, table.getId())
                    .set(DiningTable::getQrScene, scene)
                    .set(DiningTable::getQrCodeUrl, null)
                    .set(DiningTable::getUpdateTime, new Date()));
        } catch (Exception e) {
            log.error("生成台位二维码失败 tableNo={}", table.getTableNo(), e);
            throw new StoneCustomerException("桌码生成失败（" + table.getTableNo() + "）：" + e.getMessage());
        }
    }

    /** 拉取微信小程序码并合成桌号文字，返回 PNG 字节 */
    private byte[] fetchAndCompose(String tableNo, String scene) throws Exception {
        InputStream qrIn = fetchWxSunCode(QR_PAGE, scene);
        BufferedImage composed = composeWithTableNo(qrIn, tableNo);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(composed, "png", os);
        return os.toByteArray();
    }

    /**
     * 取微信 access_token（GET cgi-bin/token）
     * 与 WxQrCodeUtils.getToken 的区别：失败时抛出微信原始返回/网络异常，便于定位 TLS、凭据、白名单问题（此前静默返回 null 难以排查）
     */
    private String fetchAccessToken() throws Exception {
        String appId = wxQrCodeUtils.getAppId();
        String secret = wxQrCodeUtils.getSecret();
        if (appId == null || appId.trim().isEmpty() || secret == null || secret.trim().isEmpty()) {
            throw new StoneCustomerException("wxlogin 配置为空，请检查 WXLOGIN_APPID / WXLOGIN_SECRET 环境变量");
        }
        String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appId.trim() + "&secret=" + secret.trim();
        String body;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(tokenUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            body = readBody(conn);
        } catch (Exception e) {
            // 网络/证书类异常（如云托管云调用自签名证书导致的 PKIX 失败）在这里显性化
            throw new StoneCustomerException("调用微信接口失败（网络或证书）：" + e.getMessage());
        }
        com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSONObject.parseObject(body);
        String token = json == null ? null : json.getString("access_token");
        if (token == null || token.isEmpty()) {
            throw new StoneCustomerException("微信 access_token 获取失败，微信返回：" + body);
        }
        return token;
    }

    /** 读取 HTTP 响应体（4xx/5xx 走 errorStream） */
    private String readBody(HttpURLConnection conn) throws Exception {
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 调微信接口获取小程序码图片流（page 须为已发布页面，未发布会返回错误 JSON）
     */
    private InputStream fetchWxSunCode(String page, String scene) throws Exception {
        String token = fetchAccessToken();
        URL url = new URL("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + token);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        com.alibaba.fastjson.JSONObject paramJson = new com.alibaba.fastjson.JSONObject();
        paramJson.put("scene", scene);
        paramJson.put("page", page);
        paramJson.put("width", 430);
        // check_path=false：允许 page 页面尚未发布时生成码（联调期必需，否则报 41030）
        // 注意：扫码要真正打开该页面，仍需小程序发布含该页面的版本
        paramJson.put("check_path", false);
        // env_version：码指向的小程序版本（release/trial/develop）。
        // 小程序还没发正式版时必须配 trial，否则码能生成但扫了打不开页面；见 WxQrCodeUtils.qrcodeEnvVersion
        paramJson.put("env_version", wxQrCodeUtils.getQrcodeEnvVersion());
        try (OutputStream os = conn.getOutputStream()) {
            os.write(paramJson.toJSONString().getBytes("UTF-8"));
        }
        String contentType = conn.getContentType();
        if (contentType != null && contentType.contains("json")) {
            // 微信错误返回 JSON（如 page 未发布 41030）
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            throw new StoneCustomerException("微信接口返回错误：" + sb);
        }
        return conn.getInputStream();
    }

    /**
     * 白底合成：码图缩放居中 + 下方叠加桌号文字
     */
    private BufferedImage composeWithTableNo(InputStream qrIn, String tableNo) throws Exception {
        BufferedImage qr = ImageIO.read(qrIn);
        int W = 600, H = 720;
        int qrSize = 440;
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // 码居中偏上
        g.drawImage(qr, (W - qrSize) / 2, 50, qrSize, qrSize, null);
        // 桌号文字（居中）；用逻辑字体 SANS_SERIF 保证跨平台（本地 macOS / 容器 Linux 经 fontconfig 映射）
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        g.setColor(Color.BLACK);
        String text = tableNo;
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (W - tw) / 2, 640);
        g.dispose();
        return canvas;
    }

    @Override
    public void invalidateQrcode(Integer shopId, Long id) {
        DiningTable exist = diningTableMapper.selectById(id);
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位不存在");
        }
        // 必须用 UpdateWrapper 显式 set null：MyBatis-Plus updateById 默认忽略 null 字段（FieldStrategy.NOT_NULL），
        // 直接用实体 setQrScene(null) 不会清空旧值
        diningTableMapper.update(null, new LambdaUpdateWrapper<DiningTable>()
                .eq(DiningTable::getId, id)
                .set(DiningTable::getQrScene, null)
                .set(DiningTable::getQrCodeUrl, null)
                .set(DiningTable::getUpdateTime, new Date()));
        // 同步清缓存，避免作废后仍能取到旧码图
        QR_CACHE.remove(exist.getTableNo());
    }

    /**
     * 取桌码图（base64 data URL，供管理端缩略图/下载）
     * 缓存命中直出；容器重启后缓存丢失时按库中 qr_scene 现场重建（微信码永久有效，可重复获取）
     */
    @Override
    public String getQrcodeImageBase64(Integer shopId, String tableNo) {
        DiningTable table = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .eq(DiningTable::getTableNo, tableNo));
        if (table == null) {
            throw new StoneCustomerException("台位不存在");
        }
        if (table.getQrScene() == null || table.getQrScene().isEmpty()) {
            throw new StoneCustomerException("该台位尚未生成桌码");
        }
        byte[] png = QR_CACHE.get(table.getTableNo());
        if (png == null) {
            try {
                png = fetchAndCompose(table.getTableNo(), table.getQrScene());
                QR_CACHE.put(table.getTableNo(), png);
            } catch (Exception e) {
                log.error("台位桌码重建失败 tableNo={}", table.getTableNo(), e);
                throw new StoneCustomerException("桌码获取失败：" + e.getMessage());
            }
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    /* ==================== 台位类型 ==================== */

    @Override
    public List<TableType> listType(Integer shopId) {
        return tableTypeMapper.selectList(new LambdaQueryWrapper<TableType>()
                .eq(TableType::getShopId, shopId)
                .orderByAsc(TableType::getSortNumber));
    }

    @Override
    public void addType(Integer shopId, TableType type) {
        if (type.getName() == null || type.getName().trim().isEmpty()) {
            throw new StoneCustomerException("类型名称必填");
        }
        TableType insert = new TableType();
        insert.setShopId(shopId);
        insert.setName(type.getName().trim());
        // 低消仅展示（V7 取消校验，默认 0）
        insert.setMinConsume(type.getMinConsume() == null ? java.math.BigDecimal.ZERO : type.getMinConsume());
        insert.setDescription(type.getDescription());
        insert.setSortNumber(type.getSortNumber() == null ? 1 : type.getSortNumber());
        insert.setCreateTime(new Date());
        insert.setUpdateTime(new Date());
        tableTypeMapper.insert(insert);
    }

    @Override
    public void updateType(Integer shopId, TableType type) {
        TableType exist = tableTypeMapper.selectById(type.getId());
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位类型不存在");
        }
        TableType upd = new TableType();
        upd.setId(type.getId());
        if (type.getName() != null && !type.getName().trim().isEmpty()) {
            upd.setName(type.getName().trim());
        }
        if (type.getMinConsume() != null) {
            upd.setMinConsume(type.getMinConsume());
        }
        upd.setDescription(type.getDescription());
        if (type.getSortNumber() != null) {
            upd.setSortNumber(type.getSortNumber());
        }
        upd.setUpdateTime(new Date());
        tableTypeMapper.updateById(upd);
    }

    @Override
    public void deleteType(Integer shopId, Integer id) {
        TableType exist = tableTypeMapper.selectById(id);
        if (exist == null || !shopId.equals(exist.getShopId())) {
            throw new StoneCustomerException("台位类型不存在");
        }
        // 存在挂靠台位时拒绝删除
        Integer cnt = diningTableMapper.selectCount(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .eq(DiningTable::getTypeId, id));
        if (cnt != null && cnt > 0) {
            throw new StoneCustomerException("该类型下存在台位，无法删除");
        }
        tableTypeMapper.deleteById(id);
    }

    /* ==================== 客户端扫码校验（C-04） ==================== */

    @Override
    public Map<String, Object> detailByTableNo(String tableNo) {
        if (tableNo == null || tableNo.trim().isEmpty()) {
            throw new StoneCustomerException("桌号为空");
        }
        DiningTable table = diningTableMapper.selectOne(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getTableNo, tableNo.trim())
                .last("limit 1"));
        if (table == null) {
            throw new StoneCustomerException("未找到对应台位");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", table.getId());
        m.put("shopId", table.getShopId());
        m.put("tableNo", table.getTableNo());
        m.put("tableName", table.getTableName());
        m.put("capacity", table.getCapacity());
        m.put("status", table.getStatus()); // 3=停用 → 扫码失效
        // 当前有效 scene（客户端比对自己 scene，不匹配/为空即旧码）
        m.put("qrScene", table.getQrScene());
        TableType type = tableTypeMapper.selectById(table.getTypeId());
        if (type != null) {
            m.put("typeName", type.getName());
            m.put("minConsume", type.getMinConsume());
        }
        TableZone zone = tableZoneMapper.selectById(table.getZoneId());
        if (zone != null) {
            m.put("zoneName", zone.getName());
        }
        return m;
    }
}
