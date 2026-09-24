package com.siam.system.modular.package_user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_user.entity.MemberToken;
import com.siam.system.modular.package_user.model.example.MemberTokenExample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MemberTokenMapper extends BaseMapper<MemberToken> {
    /**
     * 会话单语句 upsert：依赖 member_id 唯一索引，替代「select+循环delete+insert」，
     * 消除并发同账号登录的死锁环（L4 压测实证，见 test-suite/l4-concurrency）
     */
    @Insert("INSERT INTO tb_member_token (member_id, username, token, type, create_time) " +
            "VALUES (#{record.memberId}, #{record.username}, #{record.token}, #{record.type}, #{record.createTime}) " +
            "ON DUPLICATE KEY UPDATE token = VALUES(token), username = VALUES(username), type = VALUES(type), create_time = VALUES(create_time)")
    int upsertSession(@Param("record") MemberToken record);

    int countByExample(MemberTokenExample example);

    int deleteByExample(MemberTokenExample example);

    int deleteByPrimaryKey(Integer id);

    int insertSelective(MemberToken record);

    List<MemberToken> selectByExample(MemberTokenExample example);

    MemberToken selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") MemberToken record, @Param("example") MemberTokenExample example);

    int updateByExample(@Param("record") MemberToken record, @Param("example") MemberTokenExample example);

    int updateByPrimaryKeySelective(MemberToken record);

    int updateByPrimaryKey(MemberToken record);

    @ResultMap("BaseResultMap")
    @Select("select * from tb_member_token where token = #{token} order by id desc limit 1 ")
    MemberToken getLoginMemberInfo(@Param("token") String token);
}