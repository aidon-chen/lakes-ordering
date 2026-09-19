package com.siam.system.modular.package_service.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_service.model.param.NoticeParam;
import com.siam.system.modular.package_service.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告接口 · C-14
 *
 * 鉴权：**公开路径**（批次 5 决策 D-I）。/rest/notice/** 不带 member 前缀，
 * 不落入 WebMvcConfig 的 memberInterceptor（该拦截器只挂 /rest/member/**），
 * 与 /rest/advertisement/list 的既有先例一致 —— 游客也能看公告。
 */
@RestController
@RequestMapping(value = "/rest/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 已发布公告列表（置顶优先，其余发布时间倒序；返回摘要，不含富文本原文）
     */
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody(required = false) NoticeParam param) {
        Integer shopId = param == null ? null : param.getShopId();
        return BasicResult.success(noticeService.list(shopId));
    }

    /**
     * 公告详情（返回富文本原文，前端 rich-text 渲染）
     */
    @PostMapping(value = "/selectById")
    public BasicResult selectById(@RequestBody NoticeParam param) {
        return BasicResult.success(noticeService.detail(param.getId()));
    }
}
