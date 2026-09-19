package com.siam.system.modular.package_service.service;

import com.siam.system.modular.package_service.model.vo.NoticeVo;

import java.util.List;

/**
 * 公告服务 · C-14
 *
 * 接口走公开路径 /rest/notice/**（不带 member 前缀），无需登录。
 */
public interface NoticeService {

    /**
     * 已发布公告列表（status=1；置顶优先，其余按发布时间倒序）。
     * 返回摘要（服务端剥 HTML 标签截断），不返回富文本原文。
     *
     * @param shopId 门店id，可为 null —— null 时不过滤门店，全店通用公告一并返回
     */
    List<NoticeVo> list(Integer shopId);

    /**
     * 公告详情（校验 status=1，否则视为不存在）
     */
    NoticeVo detail(Long id);
}
