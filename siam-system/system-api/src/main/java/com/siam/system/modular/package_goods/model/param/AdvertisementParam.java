package com.siam.system.modular.package_goods.model.param;

import com.siam.system.modular.package_goods.entity.Advertisement;
import com.siam.system.modular.package_goods.entity.Advertisement;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "广告轮播图表")
public class AdvertisementParam extends Advertisement {

    //开始日期
    private Date startCreateTime;

    //结束日期
    private Date endCreateTime;

    //页码
    private Integer pageNo = 1;

    //页面大小
    private Integer pageSize = 20;

    /**
     * 是否只取当前生效中的记录（A-10 首页海报/活动弹窗）：生效时间窗过滤开关。
     * 仅客户端读取接口传 true；管理端列表不传（看全量，便于运营编辑过期数据）。
     * 规则：start_time 为空视为不限开始；end_time 为空视为不限结束。
     */
    private Boolean onlyValid;
}