package com.siam.system.modular.package_service.model.vo;

import lombok.Data;

/**
 * 公告 VO · C-14
 *
 * 列表：只带 summary（服务端剥 HTML 标签截断），**不把富文本原文发给列表**。
 * 详情：带 content 富文本原文，前端用 rich-text 渲染。
 */
@Data
public class NoticeVo {

    private Long id;

    private String title;

    /** 公告内容富文本原文（仅详情接口填充；列表为 null） */
    private String content;

    /** 纯文本摘要（服务端由 content 剥离 HTML 标签后截断） */
    private String summary;

    /** 是否置顶（前端据此渲染「置顶」标签） */
    private Boolean isTop;

    /** 发布时间（yyyy-MM-dd，前端直接展示） */
    private String publishDate;

    /** 门店名（详情页副标题「发布于 xx · 门店名」） */
    private String shopName;
}
