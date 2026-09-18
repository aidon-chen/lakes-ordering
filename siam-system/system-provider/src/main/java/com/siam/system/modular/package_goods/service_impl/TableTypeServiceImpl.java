package com.siam.system.modular.package_goods.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siam.system.modular.package_goods.entity.TableType;
import com.siam.system.modular.package_goods.mapper.TableTypeMapper;
import com.siam.system.modular.package_goods.service.TableTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableTypeServiceImpl extends ServiceImpl<TableTypeMapper, TableType> implements TableTypeService {

    @Override
    public List<TableType> selectByShopId(Integer shopId) {
        LambdaQueryWrapper<TableType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableType::getShopId, shopId);
        wrapper.orderByAsc(TableType::getSortNumber);
        return this.list(wrapper);
    }
}
