package com.siam.system.modular.package_dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 台位类型表（tb_table_type）：散台/卡座/包间
 */
@Data
@TableName("tb_table_type")
public class TableType {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    private String name;

    /** 低消(元)，一期仅展示不校验 */
    private BigDecimal minConsume;

    private String description;

    private Integer sortNumber;

    private Date createTime;

    private Date updateTime;
}
