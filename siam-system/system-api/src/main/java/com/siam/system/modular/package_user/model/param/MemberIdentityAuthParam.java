package com.siam.system.modular.package_user.model.param;

import com.siam.package_common.model.valid_group.ValidGroupOfAudit;
import com.siam.package_common.model.valid_group.ValidGroupOfId;
import com.siam.system.modular.package_user.entity.MemberIdentityAuth;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

/**
 * 会员身份认证申请记录表
 *
 * @author 暹罗
 */
@Data
public class MemberIdentityAuthParam extends MemberIdentityAuth {

    @NotNull(message = "主键id不能为空", groups = {ValidGroupOfId.class})
    private Integer id;

    /**
     * 审核状态(1=通过 2=不通过，与门店变更/提现记录同一口径)
     */
    @NotNull(message = "审核状态不能为空", groups = {ValidGroupOfAudit.class})
    @Range(min = 1, max = 2, message = "审核状态必须介于1~2之间", groups = {ValidGroupOfAudit.class})
    private Integer status;

    /**
     * 审核意见(当审核状态为不通过时必填)
     */
    private String opinion;

    /**
     * 管理端列表筛选：会员手机号
     */
    private String mobileLike;

    /**
     * 管理端审核弹窗入参：按会员id取该会员两阶段记录
     */
    private Integer queryMemberId;

    //页码
    private Integer pageNo = 1;

    //页面大小
    private Integer pageSize = 20;
}
