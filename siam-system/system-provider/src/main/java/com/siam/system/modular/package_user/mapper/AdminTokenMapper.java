package com.siam.system.modular.package_user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_user.entity.AdminToken;
import com.siam.system.modular.package_user.model.example.AdminTokenExample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminTokenMapper extends BaseMapper<AdminToken> {
    /**
     * 会话单语句 upsert：依赖 admin_id 唯一索引，替代「select+循环delete+insert」，
     * 消除并发同账号登录的死锁环（L4 压测实证，见 test-suite/l4-concurrency）
     */
    @Insert("INSERT INTO tb_admin_token (admin_id, username, token, type, create_time) " +
            "VALUES (#{record.adminId}, #{record.username}, #{record.token}, #{record.type}, #{record.createTime}) " +
            "ON DUPLICATE KEY UPDATE token = VALUES(token), username = VALUES(username), type = VALUES(type), create_time = VALUES(create_time)")
    int upsertSession(@Param("record") AdminToken record);

    int countByExample(AdminTokenExample example);

    int deleteByExample(AdminTokenExample example);

    int deleteByPrimaryKey(Integer id);

    int insertSelective(AdminToken record);

    List<AdminToken> selectByExample(AdminTokenExample example);

    AdminToken selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") AdminToken record, @Param("example") AdminTokenExample example);

    int updateByExample(@Param("record") AdminToken record, @Param("example") AdminTokenExample example);

    int updateByPrimaryKeySelective(AdminToken record);

    int updateByPrimaryKey(AdminToken record);

    @ResultMap("BaseResultMap")
    @Select("select * from tb_admin_token where token = #{token} order by id desc limit 1 ")
    AdminToken getLoginAdminInfo(@Param("token") String token);
}