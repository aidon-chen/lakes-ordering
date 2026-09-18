package com.siam.system.modular.package_goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.siam.system.modular.package_goods.entity.TableType;

import java.util.List;

public interface TableTypeService extends IService<TableType> {

    /**
     * 按门店查台位类型列表（sort_number 升序）
     */
    List<TableType> selectByShopId(Integer shopId);
}
