package com.siam.system.modular.package_goods.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.Advertisement;
import com.siam.system.modular.package_goods.mapper.AdvertisementMapper;
import com.siam.system.modular.package_goods.model.param.AdvertisementParam;
import com.siam.system.modular.package_goods.service.AdvertisementService;
import com.siam.system.modular.package_goods.entity.Advertisement;
import com.siam.system.modular.package_goods.mapper.AdvertisementMapper;
import com.siam.system.modular.package_goods.model.param.AdvertisementParam;
import com.siam.system.modular.package_goods.service.AdvertisementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class AdvertisementServiceImpl extends ServiceImpl<AdvertisementMapper, Advertisement> implements AdvertisementService {

    @Autowired
    private AdvertisementMapper advertisementMapper;

    @Override
    public void deleteByPrimaryKey(AdvertisementParam param) {
        Advertisement dbAdvertisement = this.getById(param.getId());
        if(dbAdvertisement == null){
            throw new StoneCustomerException("该广告轮播图不存在，删除失败");
        }

        advertisementMapper.deleteByPrimaryKey(param.getId());
    }

    @Override
    public void insertSelective(AdvertisementParam param) {
        param.setCreateTime(new Date());
        param.setUpdateTime(new Date());
        advertisementMapper.insertSelective(param);
    }

    @Override
    public Advertisement selectByPrimaryKey(AdvertisementParam param) {
        return this.getById(param.getId());
    }

    @Override
    public void updateByPrimaryKeySelective(AdvertisementParam param) {
        BasicResult basicResult = new BasicResult();

        //由于商家端编辑时轮播图展示不出来，所以暂时性做逻辑控制，修改时轮播图数组可为空，代表不修改
        if(param.getImagePath().equals("")){
            param.setImagePath(null);
        }
        param.setUpdateTime(new Date());
        advertisementMapper.updateByPrimaryKeySelective(param);
    }

    @Override
    public Page getListByPage(AdvertisementParam param) {
        // 约定 pageNo<=0 表示不分页（Swagger 注释口径）：MyBatis-Plus 在 size<0 时不拼 LIMIT。
        // 此前直接把 -1 传进 Page，current 被钳成 1、pageSize 生效，导致客户端固定 pageSize:20 成为隐形上限。
        long pageNo = param.getPageNo() == null ? 1L : param.getPageNo();
        long pageSize = param.getPageSize() == null ? 20L : param.getPageSize();
        Page page = pageNo <= 0 ? new Page(1, -1) : new Page(pageNo, pageSize);
        return advertisementMapper.getListByPage(page, param);
    }
}
