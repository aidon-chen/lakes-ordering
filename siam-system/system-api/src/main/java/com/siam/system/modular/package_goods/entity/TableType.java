package com.siam.system.modular.package_goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 台位类型表（乐章点餐一期 C-03/C-04：散台/卡座 + 低消）
 * 设计依据：《客户端缺失功能设计方案-C02-03-04-10-14-15-16-v1.0.md》2.2②
 * 注：与 Shop 同模块聚合（门店详情台位类型说明）；C-04 台位实体落位时一并评估是否抽 package_dining 模块。
 */
@Data
@TableName("tb_table_type")
public class TableType {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    /** 类型名 散台/卡座 */
    private String name;

    /** 低消(元) */
    private BigDecimal minConsume;

    /** 说明 */
    private String description;

    /** 排序号 */
    private Integer sortNumber;

    private Date createTime;

    private Date updateTime;

    //页码
    @TableField(exist = false) private Integer pageNo = 1;

    //页面大小
    @TableField(exist = false) private Integer pageSize = 20;
}
