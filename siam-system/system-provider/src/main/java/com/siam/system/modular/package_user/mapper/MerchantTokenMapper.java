package com.siam.system.modular.package_user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_user.entity.MerchantToken;
import com.siam.system.modular.package_user.model.example.MerchantTokenExample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MerchantTokenMapper extends BaseMapper<MerchantToken> {
    /**
     * 会话单语句 upsert：依赖 merchant_id 唯一索引，替代「select+循环delete+insert」，
     * 消除并发同账号登录的死锁环（L4 压测实证，见 test-suite/l4-concurrency）
     */
    @Insert("INSERT INTO tb_merchant_token (merchant_id, username, token, type, create_time) " +
            "VALUES (#{record.merchantId}, #{record.username}, #{record.token}, #{record.type}, #{record.createTime}) " +
            "ON DUPLICATE KEY UPDATE token = VALUES(token), username = VALUES(username), type = VALUES(type), create_time = VALUES(create_time)")
    int upsertSession(@Param("record") MerchantToken record);

    int countByExample(MerchantTokenExample example);

    int deleteByExample(MerchantTokenExample example);

    int deleteByPrimaryKey(Integer id);

    int insertSelective(MerchantToken record);

    List<MerchantToken> selectByExample(MerchantTokenExample example);

    MerchantToken selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") MerchantToken record, @Param("example") MerchantTokenExample example);

    int updateByExample(@Param("record") MerchantToken record, @Param("example") MerchantTokenExample example);

    int updateByPrimaryKeySelective(MerchantToken record);

    int updateByPrimaryKey(MerchantToken record);

    @ResultMap("BaseResultMap")
    @Select("select * from tb_merchant_token where token = #{token} order by id desc limit 1 ")
    MerchantToken getLoginMerchantInfo(@Param("token") String token);
}