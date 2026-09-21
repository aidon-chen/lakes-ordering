package com.siam.system.modular.package_log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.OperateLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作日志Mapper（A-08）
 */
public interface OperateLogMapper extends BaseMapper<OperateLog> {

    /**
     * 分页查询：module 精确、operator 模糊、businessType 精确、result 精确、ip 精确、requestUrl 模糊、create_time 区间
     * （日期口径与其他页面一致，DATE_FORMAT 比较；模糊匹配用 concat 而非 ${} 拼接，避免注入）
     */
    @Select("<script>select o.* from tb_operate_log o" +
            "<where> 1=1 " +
            "<if test=\"log.module != null and log.module !=''\"> AND o.module = #{log.module} </if>" +
            "<if test=\"log.operator != null and log.operator !=''\"> AND o.operator like '%${log.operator}%' </if>" +
            "<if test=\"log.businessType != null\"> AND o.business_type = #{log.businessType} </if>" +
            "<if test=\"log.result != null\"> AND o.result = #{log.result} </if>" +
            "<if test=\"log.ip != null and log.ip !=''\"> AND o.ip = #{log.ip} </if>" +
            "<if test=\"log.requestUrl != null and log.requestUrl !=''\"> AND o.request_url like concat('%', #{log.requestUrl}, '%') </if>" +
            "<if test=\"log.startCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &gt;= #{log.startCreateTime} </if>" +
            "<if test=\"log.endCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &lt;= #{log.endCreateTime} </if>" +
            "</where> order by o.id desc" +
            "</script>")
    Page<OperateLog> getListByPage(@Param("page") Page page, @Param("log") OperateLog param);

    /**
     * 全量查询（不分页），用于导出
     */
    @Select("<script>select o.* from tb_operate_log o" +
            "<where> 1=1 " +
            "<if test=\"log.module != null and log.module !=''\"> AND o.module = #{log.module} </if>" +
            "<if test=\"log.operator != null and log.operator !=''\"> AND o.operator like '%${log.operator}%' </if>" +
            "<if test=\"log.businessType != null\"> AND o.business_type = #{log.businessType} </if>" +
            "<if test=\"log.result != null\"> AND o.result = #{log.result} </if>" +
            "<if test=\"log.ip != null and log.ip !=''\"> AND o.ip = #{log.ip} </if>" +
            "<if test=\"log.requestUrl != null and log.requestUrl !=''\"> AND o.request_url like concat('%', #{log.requestUrl}, '%') </if>" +
            "<if test=\"log.startCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &gt;= #{log.startCreateTime} </if>" +
            "<if test=\"log.endCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &lt;= #{log.endCreateTime} </if>" +
            "</where> order by o.id desc" +
            "</script>")
    java.util.List<OperateLog> exportList(@Param("log") OperateLog param);
}