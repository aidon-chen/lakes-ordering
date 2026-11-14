package com.siam.system.modular.package_goods.mapper.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_goods.entity.internal.GuessYouLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface GuessYouLikeMapper extends BaseMapper<GuessYouLike> {

    /**
     * 顾客端展示：关联商品（仅已上架），按 sort_number 升序
     * 列名下划线由 map-underscore-to-camel-case 映射为驼峰键，与既有 getListByLatelyMonthlySalesTopNumber 口径一致
     */
    @Select("select g.* from tb_guess_you_like gyg " +
            "left join tb_points_mall_goods g on g.id = gyg.goods_id " +
            "where g.status = 2 " +
            "order by gyg.sort_number asc, gyg.id asc " +
            "limit #{limit}")
    List<Map<String, Object>> selectDisplayGoodsList(@Param("limit") Integer limit);

    /**
     * 商家端管理：全部关联商品（不过滤上架状态，便于商家发现已下架商品并处理）
     */
    @Select("select gyg.id, gyg.goods_id AS goodsId, gyg.sort_number AS sortNumber, gyg.create_time AS createTime, " +
            "g.name, g.main_image AS mainImage, g.price, g.status, " +
            "ifnull(g.total_sales, 0) AS totalSales, ifnull(g.sales_offset, 0) AS salesOffset, " +
            "(ifnull(g.total_sales, 0) + ifnull(g.sales_offset, 0)) AS displaySales " +
            "from tb_guess_you_like gyg " +
            "left join tb_points_mall_goods g on g.id = gyg.goods_id " +
            "order by gyg.sort_number asc, gyg.id asc")
    List<Map<String, Object>> selectManagedList();
}
