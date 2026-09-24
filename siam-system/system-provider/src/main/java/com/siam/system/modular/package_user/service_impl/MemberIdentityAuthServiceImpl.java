package com.siam.system.modular.package_user.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siam.package_common.constant.BasicResultCode;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.OSSUtils;
import com.siam.system.modular.package_user.auth.cache.AdminSessionManager;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Admin;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.modular.package_user.entity.MemberIdentityAuth;
import com.siam.system.modular.package_user.mapper.MemberIdentityAuthMapper;
import com.siam.system.modular.package_user.model.param.MemberIdentityAuthParam;
import com.siam.system.modular.package_user.service.MemberIdentityAuthService;
import com.siam.system.modular.package_user.service.MemberService;
import com.siam.system.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会员身份认证申请记录表
 *
 * @author 暹罗
 */
@Slf4j
@Service
public class MemberIdentityAuthServiceImpl extends ServiceImpl<MemberIdentityAuthMapper, MemberIdentityAuth> implements MemberIdentityAuthService {

    //审核入参：1=通过
    private static final int AUDIT_PARAM_PASS = 1;

    //审核入参：2=不通过
    private static final int AUDIT_PARAM_REJECT = 2;

    /** 私有桶对象路径前缀：判断某条证件图属于私有桶还是历史上的默认桶 */
    private static final String PRIVATE_PATH_PREFIX = "data/images/private/";

    /** 上次清理时间（毫秒），用于节流；0=从未清理 */
    private static final AtomicLong LAST_CLEAN_AT = new AtomicLong(0L);

    @Autowired
    private MemberIdentityAuthMapper memberIdentityAuthMapper;

    @Autowired
    private MemberSessionManager memberSessionManager;

    @Autowired
    private AdminSessionManager adminSessionManager;

    @Autowired
    private MemberService memberService;

    @Autowired
    private OSSUtils ossUtils;

    /** 证件材料保留天数（plan §8 要求 4「审核即弃」）。2026-09-23 用户拍板：30 天 */
    @Value("${siam.identityAuth.keepDays:30}")
    private int identityKeepDays;

    /** 清理节流间隔（秒） */
    @Value("${siam.identityAuth.cleanIntervalSeconds:600}")
    private int cleanIntervalSeconds;

    /**
     * 取当前登录会员id
     * <p>
     * ⚠️ 全仓存在「memberSessionManager.getSession 返回 null 后直接解引用」的通病：
     * NPE 被 GlobalExceptionHandler 包成「系统异常」(code 0)，导致前端 http.js 里
     * `code == 2 → deleteToken()` 的失效重登分支永不触发，用户只看到莫名报错。
     * 本模块统一先判空再抛 TOKEN_ERR(2)。
     */
    private Integer getLoginMemberId() {
        String token = TokenUtil.getToken();
        if (StringUtils.isBlank(token) || !memberSessionManager.haveSession(token)) {
            throw new StoneCustomerException(BasicResultCode.TOKEN_ERR, "登录已过期，请重新登录");
        }
        Member loginMember = memberSessionManager.getSession(token);
        if (loginMember == null || loginMember.getId() == null) {
            throw new StoneCustomerException(BasicResultCode.TOKEN_ERR, "登录已过期，请重新登录");
        }
        return loginMember.getId();
    }

    //取某会员某阶段的最新一条记录（驳回后重提 = 新建记录，故按 id 倒序取第一条）
    private MemberIdentityAuth selectLatest(Integer memberId, int authType) {
        MemberIdentityAuthParam query = new MemberIdentityAuthParam();
        query.setMemberId(memberId);
        query.setAuthType(authType);
        List<MemberIdentityAuth> list = memberIdentityAuthMapper.selectListByParam(query);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    //脱敏姓名：张* / 欧**（对外展示用，避免明文姓名外露）
    private String maskName(String realName) {
        if (StringUtils.isBlank(realName)) {
            return null;
        }
        String name = realName.trim();
        if (name.length() <= 1) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> getDetailByLoginMember() {
        Integer memberId = this.getLoginMemberId();

        MemberIdentityAuth realNameAuth = this.selectLatest(memberId, MemberIdentityAuth.AUTH_TYPE_REAL_NAME);
        MemberIdentityAuth identityAuth = this.selectLatest(memberId, MemberIdentityAuth.AUTH_TYPE_IDENTITY);

        boolean stage1Passed = realNameAuth != null
                && realNameAuth.getAuditStatus() != null
                && realNameAuth.getAuditStatus().intValue() == MemberIdentityAuth.AUDIT_STATUS_PASS;

        // 证件图已迁私有桶：补下短时效签名 URL（本地联调返回相对路径），前端不再自己拼 ossUrl
        this.fillImageUrls(realNameAuth);
        this.fillImageUrls(identityAuth);

        Map<String, Object> map = new HashMap<>();
        map.put("realNameAuth", realNameAuth);
        map.put("identityAuth", identityAuth);
        map.put("stage1Passed", stage1Passed);
        //0=未提交 1=待审核 2=通过 3=驳回
        map.put("stage1Status", realNameAuth == null || realNameAuth.getAuditStatus() == null ? 0 : realNameAuth.getAuditStatus());
        map.put("stage2Status", identityAuth == null || identityAuth.getAuditStatus() == null ? 0 : identityAuth.getAuditStatus());
        return map;
    }

    @Override
    public void submitRealName(MemberIdentityAuthParam param) {
        Integer memberId = this.getLoginMemberId();

        if (StringUtils.isBlank(param.getRealName())) {
            throw new StoneCustomerException("请输入真实姓名");
        }
        if (StringUtils.isBlank(param.getIdCardFront())) {
            throw new StoneCustomerException("请上传身份证人像面");
        }
        if (StringUtils.isBlank(param.getIdCardBack())) {
            throw new StoneCustomerException("请上传身份证国徽面");
        }

        //防重复提交：待审核 / 已通过 都不允许再提交（驳回才可重提）
        MemberIdentityAuth latest = this.selectLatest(memberId, MemberIdentityAuth.AUTH_TYPE_REAL_NAME);
        if (latest != null && latest.getAuditStatus() != null) {
            if (latest.getAuditStatus().intValue() == MemberIdentityAuth.AUDIT_STATUS_WAIT) {
                throw new StoneCustomerException("实名认证正在审核中，请勿重复提交");
            }
            if (latest.getAuditStatus().intValue() == MemberIdentityAuth.AUDIT_STATUS_PASS) {
                throw new StoneCustomerException("已完成实名认证，无需重复提交");
            }
        }

        String realName = param.getRealName().trim();
        Date now = new Date();

        MemberIdentityAuth insert = new MemberIdentityAuth();
        insert.setMemberId(memberId);
        insert.setAuthType(MemberIdentityAuth.AUTH_TYPE_REAL_NAME);
        insert.setRealName(realName);
        insert.setNameMask(this.maskName(realName));
        insert.setIdCardFront(param.getIdCardFront());
        insert.setIdCardBack(param.getIdCardBack());
        insert.setAuditStatus(MemberIdentityAuth.AUDIT_STATUS_WAIT);
        insert.setCreateTime(now);
        insert.setUpdateTime(now);
        memberIdentityAuthMapper.insertSelective(insert);

        // 同步会员主表姓名：提现流程以 tb_member.real_name 作前置校验（share/withdrawal/index 第 345 行），
        // 而原写入口是「个人资料」页的姓名弹窗 —— 该行已改为进入本认证中心，故在此接续写回，保证提现链路不退化。
        // 注意：只写姓名这一个已有字段，认证材料（身份证/学生证图）一律留在本表，不碰 tb_member 结构。
        Member memberUpdate = new Member();
        memberUpdate.setId(memberId);
        memberUpdate.setRealName(realName);
        memberUpdate.setUpdateTime(now);
        memberService.updateByPrimaryKeySelective(memberUpdate);
    }

    @Override
    public void submitIdentity(MemberIdentityAuthParam param) {
        Integer memberId = this.getLoginMemberId();

        //服务端强校验阶段一：必须存在「实名认证 + 已通过」的记录才解锁阶段二
        MemberIdentityAuth realNameAuth = this.selectLatest(memberId, MemberIdentityAuth.AUTH_TYPE_REAL_NAME);
        if (realNameAuth == null
                || realNameAuth.getAuditStatus() == null
                || realNameAuth.getAuditStatus().intValue() != MemberIdentityAuth.AUDIT_STATUS_PASS) {
            throw new StoneCustomerException("请先完成实名认证");
        }

        Integer identityType = param.getIdentityType();
        if (identityType == null
                || (identityType.intValue() != MemberIdentityAuth.IDENTITY_TYPE_STUDENT
                && identityType.intValue() != MemberIdentityAuth.IDENTITY_TYPE_TEACHER)) {
            throw new StoneCustomerException("请选择身份类型");
        }

        if (identityType.intValue() == MemberIdentityAuth.IDENTITY_TYPE_STUDENT) {
            if (StringUtils.isBlank(param.getStudentCardImg())) {
                throw new StoneCustomerException("请上传学生证信息页");
            }
            if (StringUtils.isBlank(param.getStudentCardRegImg())) {
                throw new StoneCustomerException("请上传学生证注册章页");
            }
        } else {
            if (StringUtils.isBlank(param.getTeacherCertImg())) {
                throw new StoneCustomerException("请上传教师资格证");
            }
        }

        //防重复提交
        MemberIdentityAuth latest = this.selectLatest(memberId, MemberIdentityAuth.AUTH_TYPE_IDENTITY);
        if (latest != null && latest.getAuditStatus() != null) {
            if (latest.getAuditStatus().intValue() == MemberIdentityAuth.AUDIT_STATUS_WAIT) {
                throw new StoneCustomerException("身份认证正在审核中，请勿重复提交");
            }
            if (latest.getAuditStatus().intValue() == MemberIdentityAuth.AUDIT_STATUS_PASS) {
                throw new StoneCustomerException("已完成身份认证，无需重复提交");
            }
        }

        //姓名沿用阶段一已通过的真实姓名，保证两阶段可人工比对
        String realName = realNameAuth.getRealName();
        Date now = new Date();

        MemberIdentityAuth insert = new MemberIdentityAuth();
        insert.setMemberId(memberId);
        insert.setAuthType(MemberIdentityAuth.AUTH_TYPE_IDENTITY);
        insert.setIdentityType(identityType);
        insert.setRealName(realName);
        insert.setNameMask(this.maskName(realName));
        insert.setStudentCardImg(param.getStudentCardImg());
        insert.setStudentCardRegImg(param.getStudentCardRegImg());
        insert.setTeacherCertImg(param.getTeacherCertImg());
        insert.setSchoolName(param.getSchoolName());
        insert.setStudentNo(param.getStudentNo());
        insert.setCertNo(param.getCertNo());
        insert.setAuditStatus(MemberIdentityAuth.AUDIT_STATUS_WAIT);
        insert.setCreateTime(now);
        insert.setUpdateTime(now);
        memberIdentityAuthMapper.insertSelective(insert);
    }

    @Override
    public Page<Map<String, Object>> getListByPageJoinMember(MemberIdentityAuthParam param) {
        // 「审核即弃」顺带清理：审核员每次打开列表就是一次天然的触发时机（带节流，不会每次都全表扫）
        this.cleanExpiredImagesThrottled();

        Page page = memberIdentityAuthMapper.getListByPageJoinMember(
                new Page(param.getPageNo(), param.getPageSize()), param);
        return page;
    }

    @Override
    public List<Map<String, Object>> getDetailByMemberId(MemberIdentityAuthParam param) {
        if (param.getQueryMemberId() == null) {
            throw new StoneCustomerException("会员id不能为空");
        }
        List<Map<String, Object>> list = memberIdentityAuthMapper.selectJoinMemberList(param);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        // 审核弹窗是唯一需要看图的地方，逐个补签名 URL
        for (Map<String, Object> row : list) {
            this.fillImageUrls(row);
        }
        return list;
    }

    @Override
    public void audit(MemberIdentityAuthParam param) {
        //入参 status：1=通过 2=不通过（照抄 AdminShopChangeRecordController/AdminMemberWithdrawRecordController 的口径，别搞反）
        if (param.getStatus() == null) {
            throw new StoneCustomerException("审核状态不能为空");
        }
        int status = param.getStatus().intValue();
        if (status != AUDIT_PARAM_PASS && status != AUDIT_PARAM_REJECT) {
            throw new StoneCustomerException("审核状态不合法");
        }
        if (status == AUDIT_PARAM_REJECT && StringUtils.isBlank(param.getOpinion())) {
            throw new StoneCustomerException("审核不通过时，审核意见不能为空");
        }

        MemberIdentityAuth dbRecord = memberIdentityAuthMapper.selectByPrimaryKey(param.getId());
        if (dbRecord == null) {
            throw new StoneCustomerException("认证记录不存在");
        }
        if (dbRecord.getAuditStatus() == null
                || dbRecord.getAuditStatus().intValue() != MemberIdentityAuth.AUDIT_STATUS_WAIT) {
            throw new StoneCustomerException("该记录已经审核过，不允许重复审核");
        }

        //审核人留痕：取不到管理员会话时不阻断审核，仅置空
        Integer adminId = null;
        try {
            String token = TokenUtil.getToken();
            if (StringUtils.isNotBlank(token) && adminSessionManager.haveSession(token)) {
                Admin admin = adminSessionManager.getSession(token);
                if (admin != null) {
                    adminId = admin.getId();
                }
            }
        } catch (Exception e) {
            log.warn("获取审核人id失败，本单审核人留痕置空：{}", e.getMessage());
        }

        MemberIdentityAuth update = new MemberIdentityAuth();
        update.setId(param.getId());
        update.setAuditStatus(status == AUDIT_PARAM_PASS
                ? MemberIdentityAuth.AUDIT_STATUS_PASS : MemberIdentityAuth.AUDIT_STATUS_REJECT);
        if (status == AUDIT_PARAM_REJECT) {
            update.setAuditReason(param.getOpinion());
        }
        update.setAuditAdminId(adminId);
        update.setAuditTime(new Date());
        update.setUpdateTime(new Date());
        memberIdentityAuthMapper.updateByPrimaryKeySelective(update);

        // 「审核即弃」顺带清理：每出一份结论就顺手扫一遍超期材料（带节流）
        this.cleanExpiredImagesThrottled();
    }

    /* ##################################### 证件图访问地址 #################################### */

    /**
     * 填充单条记录的证件图签名 URL 与「材料是否已删除」标记。
     * <p>
     * 证件材料在**私有桶**，浏览器 {@code <img>} 与小程序 {@code <image>} 都无法携带自定义
     * token 头，所以不能沿用「前端拼 ossUrl + 路径」的老办法——那种方式在私有桶下必然 403。
     * 这里统一由后端下发短时效签名 URL（plan §8 要求 3）。
     */
    private void fillImageUrls(MemberIdentityAuth auth) {
        if (auth == null) {
            return;
        }
        auth.setIdCardFrontUrl(ossUtils.generatePrivateUrl(auth.getIdCardFront()));
        auth.setIdCardBackUrl(ossUtils.generatePrivateUrl(auth.getIdCardBack()));
        auth.setStudentCardImgUrl(ossUtils.generatePrivateUrl(auth.getStudentCardImg()));
        auth.setStudentCardRegImgUrl(ossUtils.generatePrivateUrl(auth.getStudentCardRegImg()));
        auth.setTeacherCertImgUrl(ossUtils.generatePrivateUrl(auth.getTeacherCertImg()));
        auth.setMaterialPurged(this.isMaterialPurged(auth.getAuditStatus(),
                auth.getIdCardFront(), auth.getIdCardBack(),
                auth.getStudentCardImg(), auth.getStudentCardRegImg(), auth.getTeacherCertImg()));
    }

    /**
     * 同上，针对管理端联查返回的 Map（key 为 camelCase，见 Mapper XML 的 CustomResultMap）
     */
    private void fillImageUrls(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        String front = asString(row.get("idCardFront"));
        String back = asString(row.get("idCardBack"));
        String student = asString(row.get("studentCardImg"));
        String studentReg = asString(row.get("studentCardRegImg"));
        String teacher = asString(row.get("teacherCertImg"));
        row.put("idCardFrontUrl", ossUtils.generatePrivateUrl(front));
        row.put("idCardBackUrl", ossUtils.generatePrivateUrl(back));
        row.put("studentCardImgUrl", ossUtils.generatePrivateUrl(student));
        row.put("studentCardRegImgUrl", ossUtils.generatePrivateUrl(studentReg));
        row.put("teacherCertImgUrl", ossUtils.generatePrivateUrl(teacher));
        row.put("materialPurged", this.isMaterialPurged(asInteger(row.get("auditStatus")),
                front, back, student, studentReg, teacher));
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /**
     * 材料是否已被清理：已出结论（通过/驳回）且图片路径全部为空。
     * 审核页据此显示「材料已按保留期限删除」，而不是显示一个坏掉的图片框。
     */
    private boolean isMaterialPurged(Integer auditStatus, String... paths) {
        if (auditStatus == null) {
            return false;
        }
        int status = auditStatus.intValue();
        if (status != MemberIdentityAuth.AUDIT_STATUS_PASS && status != MemberIdentityAuth.AUDIT_STATUS_REJECT) {
            return false;
        }
        for (String path : paths) {
            if (StringUtils.isNotBlank(path)) {
                return false;
            }
        }
        return true;
    }

    /* ##################################### 审核即弃：超期证件图清理 #################################### */

    @Override
    public void cleanExpiredImagesThrottled() {
        long now = System.currentTimeMillis();
        long intervalMs = Math.max(0, cleanIntervalSeconds) * 1000L;
        long last = LAST_CLEAN_AT.get();
        if (last != 0L && now - last < intervalMs) {
            return;
        }
        // CAS 抢占，避免并发请求同时扫全表
        if (!LAST_CLEAN_AT.compareAndSet(last, now)) {
            return;
        }
        try {
            this.cleanExpiredImages();
        } catch (Exception e) {
            // 清理失败绝不能影响主流程（列表查询/审核）
            log.warn("超期证件图清理异常，已忽略：{}", e.getMessage());
        }
    }

    @Override
    public int cleanExpiredImages() {
        if (identityKeepDays <= 0) {
            return 0;
        }
        Date deadline = new Date(System.currentTimeMillis() - identityKeepDays * 24L * 3600L * 1000L);

        // 只挑「已出结论 + 已有审核时间 + 仍在保留期外 + 图片路径还没清空」的记录
        QueryWrapper<MemberIdentityAuth> wrapper = new QueryWrapper<>();
        wrapper.in("audit_status", MemberIdentityAuth.AUDIT_STATUS_PASS, MemberIdentityAuth.AUDIT_STATUS_REJECT)
                .isNotNull("audit_time")
                .le("audit_time", deadline)
                .and(w -> w.isNotNull("id_card_front")
                        .or().isNotNull("id_card_back")
                        .or().isNotNull("student_card_img")
                        .or().isNotNull("student_card_reg_img")
                        .or().isNotNull("teacher_cert_img"));

        List<MemberIdentityAuth> expiredList = this.list(wrapper);
        if (expiredList == null || expiredList.isEmpty()) {
            return 0;
        }

        int cleaned = 0;
        for (MemberIdentityAuth record : expiredList) {
            // 用非短路 & ：五个对象都要尝试删，任何一个失败就整条跳过（保留路径，下次重试）
            boolean allDeleted = deleteObjectByPath(record.getIdCardFront())
                    & deleteObjectByPath(record.getIdCardBack())
                    & deleteObjectByPath(record.getStudentCardImg())
                    & deleteObjectByPath(record.getStudentCardRegImg())
                    & deleteObjectByPath(record.getTeacherCertImg());
            if (!allDeleted) {
                log.warn("证件图删除未全部成功，保留路径待下次重试：id={}", record.getId());
                continue;
            }

            // 只清图片路径，**保留审核痕迹**（状态/理由/审核人/时间/脱敏姓名）
            UpdateWrapper<MemberIdentityAuth> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", record.getId())
                    .set("id_card_front", null)
                    .set("id_card_back", null)
                    .set("student_card_img", null)
                    .set("student_card_reg_img", null)
                    .set("teacher_cert_img", null)
                    .set("update_time", new Date());
            this.update(null, updateWrapper);
            cleaned++;
        }

        if (cleaned > 0) {
            log.info("审核即弃清理完成：删除 {} 条记录的证件图（保留 {} 天）", cleaned, identityKeepDays);
        }
        return cleaned;
    }

    /**
     * 按路径前缀分流到正确的桶：私有桶上线后新增的走私有桶，更早的历史证件图仍在默认桶。
     * 不分流的话历史数据会永远删不掉（在私有桶里找不到该对象 → 永远返回失败 → 路径永远不清空）。
     */
    private boolean deleteObjectByPath(String objectName) {
        if (StringUtils.isBlank(objectName)) {
            return true;
        }
        String path = objectName.trim();
        if (path.startsWith(PRIVATE_PATH_PREFIX)) {
            return ossUtils.deletePrivateImage(path);
        }
        return ossUtils.deleteImage(path);
    }
}
