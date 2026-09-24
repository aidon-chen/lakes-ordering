package com.siam.system.modular.package_goods.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 营业时段（叠加设计）只读查询
 *
 * <p>背景：白天只卖咖啡、夜间只卖酒水，需要按"当前时段"过滤分类与商品。
 * 设计见 order-project-整理/01-solution/营业时段自动切换-白天咖啡夜间酒水-改造方案-v1.0.md、
 * 结构脚本 sql/mysql/cloudrun/31-营业时段-叠加设计.sql。
 *
 * <p>为什么把三条读放在一个 mapper：本功能是"加法"，读点集中在此便于整体评审/回退；
 * 且全部用显式别名（menuId/salePeriod…），不依赖 `select m.*` 的列名推断。
 *
 * <p>⚠️ 依赖 31- 脚本的两处结构：`tb_menu.sale_period` 与 `tb_goods_sale_period`。
 * 若目标库尚未执行该脚本，调用方（SalePeriodServiceImpl）会捕获异常并降级为"未启用/无覆盖"，
 * 保证菜单接口不因结构缺失而整体报错 —— 见该实现类的 safeXxx 方法。
 */
public interface SalePeriodMapper {

    /** 分类级时段：menuId → sale_period（NULL 表示全天） */
    @Select("select id as menuId, sale_period as salePeriod from tb_menu where shop_id = #{shopId}")
    List<Map<String, Object>> selectMenuPeriods(@Param("shopId") Integer shopId);

    /** 单品级覆盖：goodsId → period_code（同一商品可有多行 = 跨时段） */
    @Select("select r.goods_id as goodsId, r.period_code as periodCode from tb_goods_sale_period r " +
            "join tb_goods g on g.id = r.goods_id " +
            "where g.shop_id = #{shopId}")
    List<Map<String, Object>> selectGoodsOverrides(@Param("shopId") Integer shopId);

    /** 商品 → 所属分类（单品未配置覆盖时，回落到分类时段） */
    @Select("select r.goods_id as goodsId, r.menu_id as menuId from tb_menu_goods_relation r " +
            "join tb_menu m on m.id = r.menu_id " +
            "where m.shop_id = #{shopId}")
    List<Map<String, Object>> selectGoodsMenuRelations(@Param("shopId") Integer shopId);
}
