package com.siam.system.modular.package_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 存酒记录表（tb_wine_storage）· 对应原型 C-16 / C-01 存酒区
 *
 * 注意：本表属本批次新增，执行 update.sql §7 前库中不存在。
 * 批次 5 决策：新增 location 列（存放柜位）。
 */
@Data
@TableName("tb_wine_storage")
public class WineStorage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员id */
    private Integer memberId;

    /** 存入门店id */
    private Integer shopId;

    /** 品名 */
    private String goodsName;

    /** 存入瓶数 */
    private Integer bottleCount;

    /** 剩余瓶数 */
    private Integer remainCount;

    /** 存放柜位，如「B 区 03 号柜」（批次 5 新增列） */
    private String location;

    /** 存酒码（唯一；DDL 有此列，实体此前漏映射） */
    private String storageCode;

    /** 存入日期 */
    private Date storageDate;

    /** 有效期截止日期 */
    private Date expireDate;

    /** 状态 1=在存 2=已取完 3=已过期 */
    private Integer status;

    private String remark;

    private Date createTime;

    private Date updateTime;

    /* ########## 扩展字段（聚合展示用，非表字段） ########## */

    /** 门店名（列表展示用） */
    @TableField(exist = false)
    private String shopName;
}
