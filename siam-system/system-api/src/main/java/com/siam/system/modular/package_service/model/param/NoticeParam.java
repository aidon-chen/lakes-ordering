package com.siam.system.modular.package_service.model.param;

import lombok.Data;

/**
 * 公告查询入参 · C-14
 */
@Data
public class NoticeParam {

    /** 门店id（可选，NULL 表示不过滤门店，全店通用公告一并返回） */
    private Integer shopId;

    /** 公告id（详情用） */
    private Long id;

    /** 状态过滤（可选，默认只取已发布 status=1） */
    private Integer status;

    private Integer pageNo;

    private Integer pageSize;
}
