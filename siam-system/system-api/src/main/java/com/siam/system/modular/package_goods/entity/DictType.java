package com.siam.system.modular.package_goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 业务字典类型表（tb_dict_type）：管理端 A-02
 */
@Data
@TableName("tb_dict_type")
public class DictType {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 类型编码（唯一） */
    private String code;

    /** 类型名称 */
    private String name;

    /** 备注 */
    private String remark;

    private Date createTime;

    private Date updateTime;

    //页码
    @TableField(exist = false) private Integer pageNo = 1;

    //页面大小
    @TableField(exist = false) private Integer pageSize = 20;
}