package com.siam.system.modular.package_order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_order.entity.Appraise;

import java.util.Map;

public interface AppraiseService {

    /**
     * 创建
     * @param appraise
     */
    void insertSelective(Appraise appraise);

    /**
     * 删除
     * @param id
     */
    void deleteByPrimaryKey(Integer id);

    /**
     * 评价列表
     * @param pageNo
     * @param pageSize
     * @param appraise
     * @return
     */
    Page<Appraise> getListByPage(int pageNo, int pageSize, Appraise appraise);

    Page<Map<String, Object>> getMapListByPage(int pageNo, int pageSize, Appraise appraise);

    boolean getIsAllowAppraise(Appraise appraise);

    /**
     * A-03-1 实时数据看板 - 待处理评价回复数（尚无商家回复的评价数）
     */
    int countWaitReply(Integer shopId);
}