package com.siam.system.modular.package_dining.model.param;

import com.siam.system.modular.package_dining.entity.DiningTable;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 台位参数（open/change/clear 与 A-09 档案管理共用）
 */
@Data
@ApiModel(value = "台位参数")
public class DiningTableParam extends DiningTable {

    /** 开台就餐人数 */
    private Integer memberNum;

    /** 转台目标台id */
    private Long targetTableId;

    /** 操作员id（本期可选） */
    private Integer operatorId;

    /** 批量操作台位id集合（A-09 批量生成桌码） */
    private List<Long> ids;

    private Integer pageNo = 1;

    private Integer pageSize = 20;
}
