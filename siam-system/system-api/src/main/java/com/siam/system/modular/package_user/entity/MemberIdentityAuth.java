package com.siam.system.modular.package_user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会员身份认证申请记录表（顾客端 C-17 / C-17a / C-17b 原型）
 * <p>
 * 两阶段：1=实名认证（姓名 + 身份证人像面/国徽面）；2=身份认证（学生证 或 教师资格证）
 * 图片字段统一存 OSS 相对路径：完整地址 = GlobalConfig.ossUrl + 本字段
 *
 * @author 暹罗
 */
@Data
@TableName("tb_member_identity_auth")
public class MemberIdentityAuth {

    //认证类型：实名认证（身份证）
    public static final int AUTH_TYPE_REAL_NAME = 1;
    //认证类型：身份认证（学生/教师）
    public static final int AUTH_TYPE_IDENTITY = 2;

    //身份类型：学生
    public static final int IDENTITY_TYPE_STUDENT = 1;
    //身份类型：教师
    public static final int IDENTITY_TYPE_TEACHER = 2;

    //审核状态：待审核
    public static final int AUDIT_STATUS_WAIT = 1;
    //审核状态：通过
    public static final int AUDIT_STATUS_PASS = 2;
    //审核状态：驳回
    public static final int AUDIT_STATUS_REJECT = 3;

    /* ##################################### START 扩展字段 #################################### */

    //开始日期
    @TableField(exist = false)
    private Date startCreateTime;

    //结束日期
    @TableField(exist = false)
    private Date endCreateTime;

    //会员手机号（管理端列表联查 tb_member）
    @TableField(exist = false)
    private String mobile;

    //会员昵称（管理端列表联查 tb_member）
    @TableField(exist = false)
    private String nickname;

    /* ---------- 证件图访问地址（不入库） ----------
     * 证件材料已迁到**私有桶**，浏览器 <img> 与小程序 <image> 都无法携带自定义 token 头，
     * 故不能沿用「前端拼 ossUrl + 路径」的老办法，改由后端下发**短时效签名 URL**。
     * 云端=绝对签名地址；本地联调=相对路径（前端拼 GlobalConfig.ossUrl 即可）。
     */
    @TableField(exist = false)
    private String idCardFrontUrl;

    @TableField(exist = false)
    private String idCardBackUrl;

    @TableField(exist = false)
    private String studentCardImgUrl;

    @TableField(exist = false)
    private String studentCardRegImgUrl;

    @TableField(exist = false)
    private String teacherCertImgUrl;

    /**
     * 材料是否已按保留期限删除（审核结束满 N 天后后端删除图片并清空路径列）。
     * 管理端审核页据此显示「材料已按保留期限删除」，而不是显示一个坏掉的图片框。
     */
    @TableField(exist = false)
    private Boolean materialPurged;

    /* ##################################### END 扩展字段 #################################### */

    //主键id
    @TableId(type = IdType.AUTO)
    private Integer id;

    //会员id（tb_member.id）
    private Integer memberId;

    //认证类型 1=实名认证(身份证) 2=身份认证(学生/教师)
    private Integer authType;

    //身份类型 1=学生 2=教师；authType=2 时有值
    private Integer identityType;

    //用户填写的真实姓名
    private String realName;

    //脱敏姓名，对外展示用（如 张*）
    private String nameMask;

    //身份证人像面图片路径
    private String idCardFront;

    //身份证国徽面图片路径
    private String idCardBack;

    //学生证信息页图片路径
    private String studentCardImg;

    //学生证注册章页图片路径
    private String studentCardRegImg;

    //教师资格证图片路径
    private String teacherCertImg;

    //学校名称
    private String schoolName;

    //学号
    private String studentNo;

    //教师资格证证书编号
    private String certNo;

    //审核状态 1=待审核 2=通过 3=驳回
    private Integer auditStatus;

    //驳回理由（驳回时必填）
    private String auditReason;

    //审核人id（tb_admin.id）
    private Integer auditAdminId;

    //审核时间
    private Date auditTime;

    //提交时间
    private Date createTime;

    //更新时间
    private Date updateTime;
}
