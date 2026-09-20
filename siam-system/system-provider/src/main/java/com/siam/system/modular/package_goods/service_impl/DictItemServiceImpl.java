package com.siam.system.modular.package_goods.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictItem;
import com.siam.system.modular.package_goods.mapper.DictItemMapper;
import com.siam.system.modular.package_goods.service.DictItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DictItemServiceImpl implements DictItemService {

    @Autowired
    private DictItemMapper dictItemMapper;

    @Override
    public Page<DictItem> getListByPage(DictItem dictItem) {
        return dictItemMapper.getListByPage(new Page<>(dictItem.getPageNo(), dictItem.getPageSize()), dictItem);
    }

    @Override
    public void insert(DictItem dictItem) {
        dictItem.setCreateTime(new Date());
        dictItem.setUpdateTime(new Date());
        dictItemMapper.insert(dictItem);
    }

    @Override
    public void update(DictItem dictItem) {
        dictItem.setUpdateTime(new Date());
        dictItemMapper.updateById(dictItem);
    }

    @Override
    public void delete(Integer id) {
        dictItemMapper.deleteById(id);
    }

    @Override
    public DictItem selectByTypeCodeAndValue(String typeCode, String value) {
        return dictItemMapper.selectByTypeCodeAndValue(typeCode, value);
    }

    @Override
    public Integer countByTypeCode(String typeCode) {
        return dictItemMapper.countByTypeCode(typeCode);
    }
}