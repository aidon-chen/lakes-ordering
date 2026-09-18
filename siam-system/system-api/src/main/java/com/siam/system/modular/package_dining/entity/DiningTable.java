package com.siam.system.modular.package_dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 餐桌/台位表（tb_dining_table）
 */
@Data
@TableName("tb_dining_table")
public class DiningTable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer shopId;

    private String tableNo;

    private String tableName;

    private Integer typeId;

    /** 区域id（tb_table_zone.id，桌台图分区） */
    private Integer zoneId;

    /** 容纳人数 */
    private Integer capacity;

    /** 状态 1=空闲 2=占用 3=停用 */
    private Integer status;

    /** 桌码scene快照 shopId,tableNo */
    private String qrScene;

    /** 桌码图片(OSS) */
    private String qrCodeUrl;

    private Date createTime;

    private Date updateTime;

    /* ########## 扩展字段（list 聚合展示用，非表字段） ########## */

    @TableField(exist = false)
    private String typeName;

    @TableField(exist = false)
    private String zoneName;

    @TableField(exist = false)
    private java.math.BigDecimal minSpend;
}
