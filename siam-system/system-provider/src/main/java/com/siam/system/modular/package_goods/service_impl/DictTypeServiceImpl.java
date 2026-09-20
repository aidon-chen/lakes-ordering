package com.siam.system.modular.package_goods.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictType;
import com.siam.system.modular.package_goods.mapper.DictTypeMapper;
import com.siam.system.modular.package_goods.service.DictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DictTypeServiceImpl implements DictTypeService {

    @Autowired
    private DictTypeMapper dictTypeMapper;

    @Override
    public Page<DictType> getListByPage(DictType dictType) {
        return dictTypeMapper.getListByPage(new Page<>(dictType.getPageNo(), dictType.getPageSize()), dictType);
    }

    @Override
    public void insert(DictType dictType) {
        dictType.setCreateTime(new Date());
        dictType.setUpdateTime(new Date());
        dictTypeMapper.insert(dictType);
    }

    @Override
    public void update(DictType dictType) {
        dictType.setUpdateTime(new Date());
        dictTypeMapper.updateById(dictType);
    }

    @Override
    public void delete(Integer id) {
        dictTypeMapper.deleteById(id);
    }

    @Override
    public DictType selectById(Integer id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public DictType selectByCode(String code) {
        return dictTypeMapper.selectByCode(code);
    }
}