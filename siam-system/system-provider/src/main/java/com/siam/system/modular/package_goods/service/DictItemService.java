package com.siam.system.modular.package_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictItem;

/**
 * 业务字典项服务（A-02）
 */
public interface DictItemService {

    /**
     * 分页列表：typeCode 精确 + label 模糊，按 sort 升序
     */
    Page<DictItem> getListByPage(DictItem dictItem);

    void insert(DictItem dictItem);

    void update(DictItem dictItem);

    void delete(Integer id);

    /**
     * 按 typeCode + value 查询（唯一校验用）
     */
    DictItem selectByTypeCodeAndValue(String typeCode, String value);

    /**
     * 统计某类型下的字典项数量（删除类型前校验）
     */
    Integer countByTypeCode(String typeCode);
}