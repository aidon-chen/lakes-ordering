package com.siam.system.modular.package_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictType;

/**
 * 业务字典类型服务（A-02）
 */
public interface DictTypeService {

    /**
     * 分页列表：name/code 模糊筛选
     */
    Page<DictType> getListByPage(DictType dictType);

    void insert(DictType dictType);

    void update(DictType dictType);

    void delete(Integer id);

    DictType selectById(Integer id);

    /**
     * 按类型编码查询（唯一校验用）
     */
    DictType selectByCode(String code);
}