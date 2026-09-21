package com.siam.system.modular.package_goods.controller.merchant;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_goods.entity.internal.PrintTemplate;
import com.siam.system.modular.package_goods.service.internal.PrintService;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 打印模板 · 批次 G · 收银端 W-21（P-05 · 58 / 80mm 双规格） */
@RestController
@RequestMapping(value = "/rest/merchant/printTemplate")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "打印模板接口", description = "MerchantPrintTemplateController")
public class MerchantPrintTemplateController {

    @Autowired
    private PrintService printService;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    private Merchant loginMerchant() {
        return merchantSessionManager.getSession(TokenUtil.getToken());
    }

    /** 模板列表；可选按 scene 过滤 */
    @ApiOperation(value = "打印模板列表")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) Map<String, Object> body) {
        Integer scene = null;
        if (body != null && body.get("scene") != null && !String.valueOf(body.get("scene")).isEmpty()) {
            scene = Integer.valueOf(String.valueOf(body.get("scene")));
        }
        return BasicResult.success(printService.listTemplates(loginMerchant().getShopId(), scene));
    }

    /** 新增或修改模板（带 id 即修改）；校验纸宽 58/80 与所属门店 */
    @ApiOperation(value = "保存打印模板")
    @PostMapping(value = "/save")
    public BasicResult save(@RequestBody PrintTemplate template) {
        return BasicResult.success(printService.saveTemplate(loginMerchant().getShopId(), template));
    }

    /** 设为该小票类型的默认模板（同类型互斥） */
    @ApiOperation(value = "设为默认模板")
    @PostMapping(value = "/setDefault")
    public BasicResult setDefault(@RequestBody Map<String, Object> body) {
        Integer id = body.get("id") == null ? null : Integer.valueOf(String.valueOf(body.get("id")));
        if (id == null) {
            return BasicResult.error("模板id不能为空");
        }
        printService.setDefaultTemplate(loginMerchant().getShopId(), id);
        return BasicResult.success();
    }

    /** 删除模板（默认模板不允许删除） */
    @ApiOperation(value = "删除打印模板")
    @PostMapping(value = "/delete")
    public BasicResult delete(@RequestBody Map<String, Object> body) {
        Integer id = body.get("id") == null ? null : Integer.valueOf(String.valueOf(body.get("id")));
        if (id == null) {
            return BasicResult.error("模板id不能为空");
        }
        printService.deleteTemplate(loginMerchant().getShopId(), id);
        return BasicResult.success();
    }
}
