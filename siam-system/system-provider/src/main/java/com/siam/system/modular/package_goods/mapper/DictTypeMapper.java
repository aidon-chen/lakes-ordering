package com.siam.system.modular.package_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_goods.entity.DictType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 业务字典类型 Mapper（A-02）
 */
public interface DictTypeMapper extends BaseMapper<DictType> {

    /**
     * 分页列表：name/code 模糊筛选
     */
    @Select("<script>select * from tb_dict_type" +
            "<where> 1=1 " +
            "<if test=\"dictType.name != null and dictType.name != ''\"> AND name like concat('%', #{dictType.name}, '%') </if>" +
            "<if test=\"dictType.code != null and dictType.code != ''\"> AND code like concat('%', #{dictType.code}, '%') </if>" +
            "</where> order by id asc</script>")
    Page<DictType> getListByPage(@Param("page") Page<DictType> page, @Param("dictType") DictType dictType);

    /**
     * 按类型编码查询（新增/修改时的唯一校验用）
     */
    @Select("select * from tb_dict_type where code = #{code} limit 1")
    DictType selectByCode(@Param("code") String code);
}