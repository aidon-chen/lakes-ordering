package com.siam.system.modular.package_user.entity;

import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

@TableName("tb_member_token")
@ApiModel(value = "用户登录token令牌表")
public class MemberToken {

    @ApiModelProperty(notes = "主键id")
    /**必须 IdType.AUTO；IdType.NONE＝跟随全局策略（默认 ASSIGN_ID 雪花 Long），会回填进 Integer id 报 argument type mismatch。实测：NONE 报错、AUTO 正常。见 04-deploy-cost/mvp-migration/02*/
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(notes = "用户id")
    private Integer memberId;

    @ApiModelProperty(notes = "用户名")
    private String username;

    @ApiModelProperty(notes = "登录令牌")
    private String token;

    @ApiModelProperty(notes = "登录方式 wap")
    private String type;

    @ApiModelProperty(notes = "创建时间")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? null : token.trim();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}