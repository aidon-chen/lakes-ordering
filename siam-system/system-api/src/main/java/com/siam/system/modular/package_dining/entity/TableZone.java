package com.siam.system.modular.package_dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 台位区域表（tb_table_zone）：桌台图分区
 */
@Data
@TableName("tb_table_zone")
public class TableZone {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer shopId;

    /** 区域名 A区/K区/V区 */
    private String name;

    private String description;

    private Integer sortNumber;

    private Date createTime;

    private Date updateTime;
}
