package com.siam.system.modular.package_log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_log.entity.LoginLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 登录日志Mapper（A-08）
 */
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    /**
     * 分页查询：account 模糊、result 精确、create_time 区间（DATE_FORMAT 比较，与其他页面一致）
     */
    @Select("<script>select o.* from tb_login_log o" +
            "<where> 1=1 " +
            "<if test=\"log.account != null and log.account !=''\"> AND o.account like '%${log.account}%' </if>" +
            "<if test=\"log.result != null\"> AND o.result = #{log.result} </if>" +
            "<if test=\"log.startCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &gt;= #{log.startCreateTime} </if>" +
            "<if test=\"log.endCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &lt;= #{log.endCreateTime} </if>" +
            "</where> order by o.id desc" +
            "</script>")
    Page<LoginLog> getListByPage(@Param("page") Page page, @Param("log") LoginLog param);

    /**
     * 全量查询（不分页），用于导出
     */
    @Select("<script>select o.* from tb_login_log o" +
            "<where> 1=1 " +
            "<if test=\"log.account != null and log.account !=''\"> AND o.account like '%${log.account}%' </if>" +
            "<if test=\"log.result != null\"> AND o.result = #{log.result} </if>" +
            "<if test=\"log.startCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &gt;= #{log.startCreateTime} </if>" +
            "<if test=\"log.endCreateTime != null\"> AND DATE_FORMAT(o.create_time, '%Y/%m/%d') &lt;= #{log.endCreateTime} </if>" +
            "</where> order by o.id desc" +
            "</script>")
    List<LoginLog> exportList(@Param("log") LoginLog param);
}