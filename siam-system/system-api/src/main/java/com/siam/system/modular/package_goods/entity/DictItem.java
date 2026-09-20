package com.siam.system.modular.package_goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 业务字典项表（tb_dict_item）：管理端 A-02
 */
@Data
@TableName("tb_dict_item")
public class DictItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属类型编码（tb_dict_type.code） */
    private String typeCode;

    /** 字典项名称 */
    private String label;

    /** 字典项键值 */
    private String value;

    /** 排序 */
    private Integer sort;

    /** 状态 1=启用 2=停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    private Date createTime;

    private Date updateTime;

    //页码
    @TableField(exist = false) private Integer pageNo = 1;

    //页面大小
    @TableField(exist = false) private Integer pageSize = 20;
}