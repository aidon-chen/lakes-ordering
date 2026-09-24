package com.siam.system.modular.package_points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_points.entity.MemberPointsLog;
import org.apache.ibatis.annotations.Select;

public interface MemberPointsLogMapper extends BaseMapper<MemberPointsLog> {

    /** 会员累计积分（红冲为负自然抵扣）；无流水返回 0 */
    @Select("SELECT IFNULL(SUM(change_points), 0) FROM tb_member_points_log WHERE member_id = #{memberId}")
    Integer sumPointsByMemberId(Integer memberId);
}
