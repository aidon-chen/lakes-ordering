package com.siam.system.modular.package_user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.system.modular.package_user.entity.MemberIdentityAuth;
import com.siam.system.modular.package_user.model.param.MemberIdentityAuthParam;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 会员身份认证申请记录表
 *
 * @author 暹罗
 */
public interface MemberIdentityAuthMapper extends BaseMapper<MemberIdentityAuth> {

    int insertSelective(MemberIdentityAuth record);

    int updateByPrimaryKeySelective(MemberIdentityAuth record);

    MemberIdentityAuth selectByPrimaryKey(Integer id);

    @ResultMap("BaseResultMap")
    @Select("<script>select mia.* from tb_member_identity_auth mia" +
            "<where> 1=1 " +
            "<if test=\"auth.id != null\"> AND mia.id = #{auth.id} </if>" +
            "<if test=\"auth.memberId != null\"> AND mia.member_id = #{auth.memberId} </if>" +
            "<if test=\"auth.authType != null\"> AND mia.auth_type = #{auth.authType} </if>" +
            "<if test=\"auth.identityType != null\"> AND mia.identity_type = #{auth.identityType} </if>" +
            "<if test=\"auth.auditStatus != null\"> AND mia.audit_status = #{auth.auditStatus} </if>" +
            "</where> order by mia.id desc" +
            "</script>")
    List<MemberIdentityAuth> selectListByParam(@Param("auth") MemberIdentityAuthParam auth);

    @ResultMap("CustomResultMap")
    @Select("<script>select mia.*, m.mobile, m.username, m.nickname from tb_member_identity_auth mia " +
            "left join tb_member m on m.id = mia.member_id " +
            "<where> 1=1 " +
            "<if test=\"auth.id != null\"> AND mia.id = #{auth.id} </if>" +
            "<if test=\"auth.memberId != null\"> AND mia.member_id = #{auth.memberId} </if>" +
            "<if test=\"auth.queryMemberId != null\"> AND mia.member_id = #{auth.queryMemberId} </if>" +
            "<if test=\"auth.authType != null\"> AND mia.auth_type = #{auth.authType} </if>" +
            "<if test=\"auth.identityType != null\"> AND mia.identity_type = #{auth.identityType} </if>" +
            "<if test=\"auth.auditStatus != null and auth.auditStatus != -1\"> AND mia.audit_status = #{auth.auditStatus} </if>" +
            "<if test=\"auth.mobileLike != null and auth.mobileLike != ''\"> AND m.mobile like concat('%', #{auth.mobileLike}, '%') </if>" +
            "<if test=\"auth.realName != null and auth.realName != ''\"> AND mia.real_name like concat('%', #{auth.realName}, '%') </if>" +
            "<if test=\"auth.startCreateTime != null\"> AND mia.create_time &gt;= #{auth.startCreateTime} </if>" +
            "<if test=\"auth.endCreateTime != null\"> AND mia.create_time &lt;= #{auth.endCreateTime} </if>" +
            "</where> order by mia.id desc" +
            "</script>")
    Page<Map<String, Object>> getListByPageJoinMember(@Param("page") Page page, @Param("auth") MemberIdentityAuthParam auth);

    @ResultMap("CustomResultMap")
    @Select("<script>select mia.*, m.mobile, m.username, m.nickname from tb_member_identity_auth mia " +
            "left join tb_member m on m.id = mia.member_id " +
            "<where> 1=1 " +
            "<if test=\"auth.queryMemberId != null\"> AND mia.member_id = #{auth.queryMemberId} </if>" +
            "<if test=\"auth.memberId != null\"> AND mia.member_id = #{auth.memberId} </if>" +
            "<if test=\"auth.authType != null\"> AND mia.auth_type = #{auth.authType} </if>" +
            "</where> order by mia.auth_type asc, mia.id desc" +
            "</script>")
    List<Map<String, Object>> selectJoinMemberList(@Param("auth") MemberIdentityAuthParam auth);
}
