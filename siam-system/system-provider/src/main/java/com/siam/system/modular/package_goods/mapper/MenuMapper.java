package com.siam.system.modular.package_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_goods.entity.Menu;
import com.siam.system.modular.package_goods.model.example.MenuExample;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Map;

public interface MenuMapper extends BaseMapper<Menu> {
    int countByExample(MenuExample example);

    int deleteByExample(MenuExample example);

    int deleteByPrimaryKey(Integer id);

    List<Menu> selectByExample(MenuExample example);

    Menu selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") Menu record, @Param("example") MenuExample example);

    int updateByExample(@Param("record") Menu record, @Param("example") MenuExample example);

    int updateByPrimaryKeySelective(Menu record);

    int updateByPrimaryKey(Menu record);

    @ResultMap("BaseResultMap")
    @Select("<script>select m.* from tb_menu m" +
            "<where> 1=1 " +
            "<if test=\"menu.id != null\"> AND m.id = #{menu.id} </if>" +
            "<if test=\"menu.shopId != null\"> AND m.shop_id = #{menu.shopId} </if>" +
            "<if test=\"menu.name != null and menu.name !=''\"> AND m.name like '%${menu.name}%' </if>" +
            "<if test=\"menu.sortNumber != null\"> AND m.sort_number = #{menu.sortNumber} </if>" +
            "<if test=\"menu.description != null and menu.description != ''\"> AND m.description like '%${menu.description}' </if>" +
            //A-05 分类「可否点单」：C 端菜单仅返回可点单分类（NULL 视为可点单，兼容历史数据）
            " AND (m.is_can_order IS NULL OR m.is_can_order = 1)" +
            "</where> order by m.id desc" +
            "</script>")
    List<Map<String, Object>> getList(@Param("menu") Menu menu);

    @ResultMap("CustomResultMap")
    @Select("<script>select m.*, g.id as goodsId, g.name as goodsName, g.main_image as mainImage, g.price as goodsPrice, g.sale_price as salePrice, g.stock, g.status as goodsStatus, g.is_sale as isSale, g.packing_charges as packingCharges " +
            "from tb_menu m " +
            "left join tb_menu_goods_relation mgr on mgr.menu_id = m.id " +
            "left join tb_goods g on g.id = mgr.goods_id " +
            "<where> 1=1 " +
            "<if test=\"menu.id != null\"> AND m.id = #{menu.id} </if>" +
            "<if test=\"menu.shopId != null\"> AND m.shop_id = #{menu.shopId} </if>" +
            "<if test=\"menu.name != null and menu.name !=''\"> AND m.name like '%${menu.name}%' </if>" +
            "<if test=\"menu.sortNumber != null\"> AND m.sort_number = #{menu.sortNumber} </if>" +
            "<if test=\"menu.description != null and menu.description != ''\"> AND m.description like '%${menu.description}' </if>" +
            //A-05 分类「可否点单」：C 端菜单仅返回可点单分类（NULL 视为可点单，兼容历史数据）
            " AND (m.is_can_order IS NULL OR m.is_can_order = 1)" +
            "</where> order by m.id desc" +
            "</script>")
    List<Map<String, Object>> getListJoinGoods(@Param("menu") Menu menu);

    @ResultMap("BaseResultMap")
    @Select("<script>select m.* from tb_menu m" +
            "<where> 1=1 " +
            "<if test=\"menu.id != null\"> AND m.id = #{menu.id} </if>" +
            "<if test=\"menu.shopId != null\"> AND m.shop_id = #{menu.shopId} </if>" +
            "<if test=\"menu.name != null and menu.name !=''\"> AND m.name like '%${menu.name}%' </if>" +
            "<if test=\"menu.sortNumber != null\"> AND m.sort_number = #{menu.sortNumber} </if>" +
            "<if test=\"menu.description != null and menu.description != ''\"> AND m.description like '%${menu.description}' </if>" +
            //营业时段（叠加设计）：管理端「白天/夜晚」切换按时段筛选（NULL/all 视为全天可售，与 C 端 matchPeriod 同义）
            "<if test=\"menu.salePeriod != null and menu.salePeriod != ''\"> AND (m.sale_period IS NULL OR m.sale_period = 'all' OR m.sale_period = #{menu.salePeriod}) </if>" +
            "</where> order by m.id desc" +
            "</script>")
    Page<Menu> getListByAdmin(@Param("page") Page page, @Param("menu") Menu menu);
}