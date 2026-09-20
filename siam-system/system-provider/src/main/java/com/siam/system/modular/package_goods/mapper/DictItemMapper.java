package com.siam.system.modular.package_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 业务字典项 Mapper（A-02）
 */
public interface DictItemMapper extends BaseMapper<DictItem> {

    /**
     * 分页列表：typeCode 精确 + label 模糊，按 sort 升序
     */
    @Select("<script>select * from tb_dict_item" +
            "<where> 1=1 " +
            "<if test=\"dictItem.typeCode != null and dictItem.typeCode != ''\"> AND type_code = #{dictItem.typeCode} </if>" +
            "<if test=\"dictItem.label != null and dictItem.label != ''\"> AND label like concat('%', #{dictItem.label}, '%') </if>" +
            "</where> order by sort asc, id asc</script>")
    Page<DictItem> getListByPage(@Param("page") Page<DictItem> page, @Param("dictItem") DictItem dictItem);

    /**
     * 按 typeCode + value 查询（新增/修改时的唯一校验用）
     */
    @Select("select * from tb_dict_item where type_code = #{typeCode} and `value` = #{value} limit 1")
    DictItem selectByTypeCodeAndValue(@Param("typeCode") String typeCode, @Param("value") String value);

    /**
     * 统计某类型下的字典项数量（删除类型前校验）
     */
    @Select("select count(*) from tb_dict_item where type_code = #{typeCode}")
    Integer countByTypeCode(@Param("typeCode") String typeCode);
}