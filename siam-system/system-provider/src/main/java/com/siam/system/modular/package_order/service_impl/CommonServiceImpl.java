package com.siam.system.modular.package_order.service_impl;

import com.google.gson.internal.LinkedTreeMap;
import com.siam.package_common.constant.BusinessType;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.BaiduMapUtils;
import com.siam.system.modular.package_goods.entity.Setting;
import com.siam.system.modular.package_goods.service.SettingService;
import com.siam.system.modular.package_goods.service.ShopService;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_order.entity.TravelingDistanceVo;
import com.siam.system.modular.package_order.service.CommonService;
import com.siam.system.modular.package_order.service.DeliveryAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class CommonServiceImpl implements CommonService {

    @Autowired
    private DeliveryAddressService deliveryAddressService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private BaiduMapUtils baiduMapUtils;

    @Autowired
    private SettingService settingService;

    @Override
    public BigDecimal selectDeliveryFee(BigDecimal lngA, BigDecimal latA, Integer shopId) {
        Shop dbShop = shopService.getById(shopId);
        if(dbShop == null){
            throw new StoneCustomerException("门店数据异常");
        }

        Setting setting = settingService.selectCurrent();

        //拼接省、市、区、街道
        /*String addressA = dbDeliveryAddress.getProvince() + dbDeliveryAddress.getCity() + dbDeliveryAddress.getArea() + dbDeliveryAddress.getStreet();*/
        /*String addressB = dbShop.getProvince() + dbShop.getCity() + dbShop.getArea() + dbShop.getStreet();*/

        //获取收货地址定位 与 门店的行车距离
        LinkedTreeMap map = baiduMapUtils.getTravelingDistanceFromCoordinate(lngA, latA, dbShop.getLongitude(), dbShop.getLatitude(), BusinessType.BAIDU_TRAVELING_PLAN_RIDING);
        LinkedTreeMap distanceMap = (LinkedTreeMap) map.get("distance");
        LinkedTreeMap durationMap = (LinkedTreeMap) map.get("duration");
        //路线距离 得保留三位小数，精确到1~9米
        BigDecimal distanceValue = BigDecimal.valueOf((double) distanceMap.get("value"));
        distanceValue = distanceValue.divide(BigDecimal.valueOf(1000), 3, BigDecimal.ROUND_HALF_UP);

        //如果距离为0，则代表百度地图没有计算结果
        //还有一种情况会造成距离为0，那就是起点和终点相等--这种情况也算作地址填写错误
        if(distanceValue.compareTo(BigDecimal.ZERO) == 0){
            throw new StoneCustomerException("您的收货地址超出配送范围");
        }
        //超出5.5公里则不予配送
        if(distanceValue.compareTo(setting.getDeliveryDistanceLimit()) > 0){
            throw new StoneCustomerException("您的配送距离超出" + setting.getDeliveryDistanceLimit() + "公里，不予配送");
        }

        //配送费计算规则：起送价1.5元(0~1KM)，每增加1KM加1元
        //得按照行车距离-骑行来计算，不能按照直线距离

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if(distanceValue.compareTo(BigDecimal.ZERO)>=0 && distanceValue.compareTo(BigDecimal.ONE)<=0){
            deliveryFee = setting.getStartDeliveryPrice();
        }else{
            //增加的价格要按照天花板取整
            BigDecimal sumIncreasedPrice = distanceValue.subtract(BigDecimal.ONE).multiply(setting.getDeliveryKilometerPrice()).setScale(0, BigDecimal.ROUND_CEILING);
            deliveryFee = setting.getStartDeliveryPrice().add(sumIncreasedPrice);
        }
        return deliveryFee;
    }

    @Override
    public BigDecimal selectDeliveryFee(String addressStr, Integer shopId) {
        return null;
    }

    @Override
    public TravelingDistanceVo selectTravelingDistance(BigDecimal lngA, BigDecimal latA, BigDecimal lngB, BigDecimal latB) {
        TravelingDistanceVo travelingDistanceVo = new TravelingDistanceVo();

        //容错：云托管容器可能访问不了百度接口（网络受限），此时降级为直线距离估算，
        //保证店铺列表/详情/搜索流程可用（见 04-deploy-cost/mvp-migration/云托管部署工作日志.md）
        try {
            //获取收货地址定位 与 门店的行车距离
            LinkedTreeMap map = baiduMapUtils.getTravelingDistanceFromPoints(lngA, latA, lngB, latB, BusinessType.BAIDU_TRAVELING_PLAN_RIDING);
            LinkedTreeMap distanceMap = (LinkedTreeMap) map.get("distance");
            LinkedTreeMap durationMap = (LinkedTreeMap) map.get("duration");
            if (distanceMap == null || durationMap == null || distanceMap.get("value") == null) {
                throw new IllegalStateException("百度路线规划返回缺少 distance/duration");
            }
            //路线距离 得保留三位小数，精确到1~9米
            BigDecimal distanceValue = BigDecimal.valueOf((double) distanceMap.get("value"));
            distanceValue = distanceValue.divide(BigDecimal.valueOf(1000), 3, BigDecimal.ROUND_HALF_UP);

            travelingDistanceVo.setDistanceValue(distanceValue);
            travelingDistanceVo.setDurationText((String) durationMap.get("text"));
            travelingDistanceVo.setDistanceText((String) distanceMap.get("text"));
            travelingDistanceVo.setDurationValue(BigDecimal.valueOf((double) durationMap.get("value")));
        } catch (Exception e) {
            BigDecimal km = this.straightLineKm(lngA, latA, lngB, latB);
            travelingDistanceVo.setDistanceValue(km);
            if (km.compareTo(BigDecimal.ONE) < 0) {
                travelingDistanceVo.setDistanceText(km.multiply(BigDecimal.valueOf(1000)).setScale(0, BigDecimal.ROUND_HALF_UP) + "m");
            } else {
                travelingDistanceVo.setDistanceText(km + "km");
            }
            int minutes = Math.max(1, km.multiply(BigDecimal.valueOf(3)).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
            travelingDistanceVo.setDurationText("约" + minutes + "分钟");
            travelingDistanceVo.setDurationValue(BigDecimal.valueOf(minutes * 60));
            log.warn("百度路线规划调用失败，降级为直线距离：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return travelingDistanceVo;
    }

    /**
     * 两点间直线距离（haversine，返回公里，保留 3 位小数）
     */
    private BigDecimal straightLineKm(BigDecimal lngA, BigDecimal latA, BigDecimal lngB, BigDecimal latB) {
        double R = 6371000;
        double dLat = Math.toRadians(latB.doubleValue() - latA.doubleValue());
        double dLng = Math.toRadians(lngB.doubleValue() - lngA.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latA.doubleValue())) * Math.cos(Math.toRadians(latB.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(R * c / 1000).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public Boolean selectIsOperatingOfShop(Integer shopId) {
        Shop shop = shopService.getById(shopId);
        if(shop == null) throw new StoneCustomerException("该店铺不存在");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        //返回是否营业时间
        Boolean result = false;

        Date date = new Date();
        String time = sdf.format(date);
        String[] timeArray = time.split(":");
        String[] startTimeArray = shop.getStartTime().split(":");
        String[] endTimeArray = shop.getEndTime().split(":");

        Integer timeArrayHour = Integer.valueOf(timeArray[0]);
        Integer timeArrayMinute = Integer.valueOf(timeArray[1]);
        Integer startTimeArrayHour = Integer.valueOf(startTimeArray[0]);
        Integer startTimeArrayMinute = Integer.valueOf(startTimeArray[1]);
        Integer endTimeArrayHour = Integer.valueOf(endTimeArray[0]);
        Integer endTimeArrayMinute = Integer.valueOf(endTimeArray[1]);

        if(endTimeArrayHour>startTimeArrayHour||(endTimeArrayHour==startTimeArrayHour&&endTimeArrayMinute>startTimeArrayMinute)){
            //不跨天
            if (timeArrayHour>startTimeArrayHour||(timeArrayHour==startTimeArrayHour&&timeArrayMinute>startTimeArrayMinute)) {
                if(timeArrayHour<endTimeArrayHour||(timeArrayHour==endTimeArrayHour&&timeArrayMinute<endTimeArrayMinute)){
                    result = true;
                }
            }
        }else{
            //跨天
            if(timeArrayHour>startTimeArrayHour||(timeArrayHour==startTimeArrayHour&&timeArrayMinute>startTimeArrayMinute)){
                //跨天1-当天大于开始时间
                result = true;
            }else if(timeArrayHour<endTimeArrayHour||(timeArrayHour==endTimeArrayHour&&timeArrayMinute<startTimeArrayMinute)){
                //跨天2-第二天小于结束时间
                result = true;
            }
        }
        return result;
    }
}