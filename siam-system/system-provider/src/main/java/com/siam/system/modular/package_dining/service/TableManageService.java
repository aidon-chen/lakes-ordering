package com.siam.system.modular.package_dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_dining.entity.DiningTable;
import com.siam.system.modular.package_dining.entity.TableType;
import com.siam.system.modular.package_dining.model.param.DiningTableParam;

import java.util.List;
import java.util.Map;

/**
 * 台位档案管理服务（A-09：台位基础信息 CRUD + 桌面二维码生成/下载/作废 + 台位类型管理 + 客户端扫码校验）
 * 状态口径：1=空闲 2=占用 3=停用（收银端三态；作废码与停用台位分离）
 */
public interface TableManageService {

    /* ==================== 台位档案 ==================== */

    /**
     * 台位分页列表（筛选：关键字/类型/区域/状态），records 含类型名/区域名/低消展示
     */
    Page<Map<String, Object>> listTable(Integer shopId, DiningTableParam param);

    /**
     * 新增台位：校验桌号唯一（uk_shop_table_no）+ 必填，默认空闲
     */
    void addTable(Integer shopId, DiningTable table);

    /**
     * 编辑台位档案
     */
    void updateTable(Integer shopId, DiningTable table);

    /**
     * 停用台位：status=3（停用后扫码失效、收银端桌卡图不显示）
     */
    void disableTable(Integer shopId, Long id);

    /**
     * 启用台位：status=1（二维码若已作废需重新生成）
     */
    void enableTable(Integer shopId, Long id);

    /* ==================== 桌面二维码 ==================== */

    /**
     * 生成桌面二维码（单个/批量）：scene=tid={tableNo}，page=扫码开台页，码图叠加桌号文字，上传 OSS 回写 qr_code_url/qr_scene
     */
    void generateQrcode(Integer shopId, List<Long> ids);

    /**
     * 作废二维码：清空 qr_scene/qr_code_url（台位可重新生成新码，不影响营业状态）
     */
    void invalidateQrcode(Integer shopId, Long id);

    /**
     * 下载二维码：返回 OSS 完整 URL（文件名为 {table_no}.png）
     */
    String getQrcodeUrl(Integer shopId, String tableNo);

    /* ==================== 台位类型 ==================== */

    List<TableType> listType(Integer shopId);

    void addType(Integer shopId, TableType type);

    void updateType(Integer shopId, TableType type);

    /**
     * 删除类型：存在挂靠台位时拒绝
     */
    void deleteType(Integer shopId, Integer id);

    /* ==================== 客户端扫码校验（C-04） ==================== */

    /**
     * 按桌号查台位详情（状态/容量/类型名/区域名/当前有效 qr_scene），供客户端扫码校验与展示
     */
    Map<String, Object> detailByTableNo(String tableNo);
}
