package com.siam.system.modular.package_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.model.dto.CouponsDto;
import com.siam.system.modular.package_goods.entity.Coupons;

import java.util.List;
import java.util.Map;

public interface CouponsService {

    /**
     * 领券中心券池：未删除且发行方式=自行领取(2)的券列表（member 侧，2026-09-24）
     */
    List<Coupons> listIssuable();

    void deleteByPrimaryKey(Integer id);

    void insertSelective(Coupons coupons);

    Map selectCouponsAndGoodsByPrimaryKey(Integer id);

    Coupons selectByPrimaryKey(Integer id);

    void updateByPrimaryKeySelective(Coupons coupons);

    Page<Coupons> getListByPage(int pageNo, int pageSize, Coupons coupons);

    Page<Map<String, Object>> getMapListByPage(int pageNo, int pageSize, Coupons coupons);

    Page<Map<String, Object>> getListJoinCouponsShopRelationByPage(int pageNo, int pageSize, CouponsDto couponsDto);

    void updateCouponsEndTime(Coupons coupons);

}
