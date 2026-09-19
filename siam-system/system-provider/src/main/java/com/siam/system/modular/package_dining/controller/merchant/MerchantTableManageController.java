package com.siam.system.modular.package_dining.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_dining.entity.DiningTable;
import com.siam.system.modular.package_dining.model.param.DiningTableParam;
import com.siam.system.modular.package_dining.service.TableManageService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 台位档案管理接口（A-09 管理端：台位基础信息 + 桌面二维码 生成/下载/作废）
 * 鉴权：/rest/merchant/** 由 merchant 拦截器统一保护；shopId 取自登录商家
 */
@RestController
@RequestMapping(value = "/rest/merchant/tableManage")
@Transactional(rollbackFor = Exception.class)
public class MerchantTableManageController {

    @Autowired
    private TableManageService tableManageService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Integer shopId() {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        return loginMerchant.getShopId();
    }

    /**
     * 台位分页列表（筛选：关键字/类型/区域/状态）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) DiningTableParam param) {
        return BasicResult.success(tableManageService.listTable(shopId(), param));
    }

    /**
     * 新增台位
     */
    @PostMapping(value = "/add")
    public BasicResult add(@RequestBody DiningTable table) {
        tableManageService.addTable(shopId(), table);
        return BasicResult.success();
    }

    /**
     * 编辑台位
     */
    @PostMapping(value = "/update")
    public BasicResult update(@RequestBody DiningTable table) {
        tableManageService.updateTable(shopId(), table);
        return BasicResult.success();
    }

    /**
     * 停用台位（status=3：扫码失效、收银端不显示）
     */
    @PostMapping(value = "/disable")
    public BasicResult disable(@RequestBody DiningTableParam param) {
        tableManageService.disableTable(shopId(), param.getId());
        return BasicResult.success();
    }

    /**
     * 启用台位（status=1）
     */
    @PostMapping(value = "/enable")
    public BasicResult enable(@RequestBody DiningTableParam param) {
        tableManageService.enableTable(shopId(), param.getId());
        return BasicResult.success();
    }

    /**
     * 生成桌面二维码（单个/批量：入参 ids）
     */
    @PostMapping(value = "/generateQrcode")
    public BasicResult generateQrcode(@RequestBody DiningTableParam param) {
        tableManageService.generateQrcode(shopId(), param.getIds());
        return BasicResult.success();
    }

    /**
     * 作废二维码（清空 qr_scene/qr_code_url，台位可重新生成）
     */
    @PostMapping(value = "/invalidateQrcode")
    public BasicResult invalidateQrcode(@RequestBody DiningTableParam param) {
        tableManageService.invalidateQrcode(shopId(), param.getId());
        return BasicResult.success();
    }

    /**
     * 获取桌码图（返回 base64 data URL）：管理端用于预览与下载（文件名 {table_no}.png）
     */
    @PostMapping(value = "/downloadQrcode")
    public BasicResult downloadQrcode(@RequestBody DiningTableParam param) {
        return BasicResult.success(tableManageService.getQrcodeImageBase64(shopId(), param.getTableNo()));
    }
}
