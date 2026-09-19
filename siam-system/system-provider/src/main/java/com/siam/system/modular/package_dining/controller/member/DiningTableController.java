package com.siam.system.modular.package_dining.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_dining.service.TableManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端桌台接口（C-04 扫码开台前置依赖）
 * detail：顾客扫码后按桌号校验台位可用性（status=3 停用即失效），并返回容量/类型/区域/有效 scene
 */
@RestController
@RequestMapping(value = "/rest/shop/diningTable")
public class DiningTableController {

    @Autowired
    private TableManageService tableManageService;

    /**
     * 台位详情（扫码校验）
     */
    @PostMapping(value = "/detail")
    public BasicResult detail(@RequestBody Map<String, String> param) {
        String tableNo = param.get("tableNo");
        return BasicResult.success(tableManageService.detailByTableNo(tableNo));
    }
}
