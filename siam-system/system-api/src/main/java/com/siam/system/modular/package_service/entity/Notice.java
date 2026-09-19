package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 公告表（tb_notice）· 对应原型 C-14（列表 + 详情）
 *
 * 注意：本表属本批次新增，执行 update.sql §8 前库中不存在。
 */
@Data
@TableName("tb_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门店id，NULL = 全店通用 */
    private Integer shopId;

    /** 公告标题 */
    private String title;

    /** 公告内容（富文本，rich-text 渲染） */
    private String content;

    /** 是否置顶 0=否 1=是 */
    private Boolean isTop;

    /** 状态 0=下线 1=发布 */
    private Integer status;

    /** 发布时间 */
    private Date publishTime;

    private Date createTime;

    private Date updateTime;

    /* ########## 扩展字段（详情展示用，非表字段） ########## */

    /** 门店名（详情页副标题「发布于 xx · 门店名」） */
    @TableField(exist = false)
    private String shopName;
}
