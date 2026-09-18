package com.siam.system.modular.package_user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

@TableName("tb_admin_token")
@ApiModel(value = "管理员登录token令牌表")
public class AdminToken {
    /**
     * 必须显式 IdType.AUTO（数据库自增）。注意 IdType.NONE 不是「不处理」，而是「跟随全局策略」，
     * 而 MyBatis-Plus 全局默认策略是 ASSIGN_ID（雪花 Long），会把 Long 回填进 Integer id 报
     * `argument type mismatch`——本地实测：NONE 复现该异常，AUTO 正常。见 04-deploy-cost/mvp-migration/02
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(notes = "主键id")
    private Integer id;

    @ApiModelProperty(notes = "管理员id")
    private Integer adminId;

    @ApiModelProperty(notes = "管理员用户名")
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

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
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