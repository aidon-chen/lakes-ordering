package com.siam.system.modular.package_user.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_user.model.param.MemberParam;
import com.siam.system.modular.package_user.model.result.MemberResult;
import com.siam.system.modular.package_user.service.MemberService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户模块相关接口
 *
 * @author 暹罗
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/member")
@Transactional(rollbackFor = Exception.class)
public class MemberController {

    @Autowired
    private MemberService memberService;

    /**
     * 读取 callContainer 自动注入的微信用户信息（x-wx-openid 等 header）。
     * 云托管文档：小程序经 callContainer 调用时 header 自动携带 openid/unionid，
     * 无需再用 wx.login 的 code 调 jscode2session 置换（本地/公网调用时无此 header，跳过）。
     */
    private void applyWxHeaders(MemberParam param) {
        try {
            if (param.getOpenId() == null || param.getOpenId().isEmpty()) {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                String openid = request.getHeader("x-wx-openid");
                if (openid != null && !openid.isEmpty()) {
                    param.setOpenId(openid);
                    log.debug("已从 callContainer header 读取 openid：{}", openid);
                }
            }
        } catch (Exception e) {
            log.debug("读取 x-wx-openid 失败（本地调用时正常现象）：{}", e.getMessage());
        }
    }

    /**
     * 登录-账号密码
     *
     * @author 暹罗
     */

    @PostMapping(value = "/login")
    public BasicResult login(@RequestBody @Validated(value = {}) MemberParam param) {
        this.applyWxHeaders(param);
        MemberResult result = memberService.login(param);
        return BasicResult.success(result);
    }

    /**
     * 验证码登录
     *
     * @author 暹罗
     */
    @PostMapping(value = "/verification/login")
    public BasicResult verificationLogin(@RequestBody @Validated(value = {}) MemberParam param) {
        this.applyWxHeaders(param);
        MemberResult result = memberService.verificationLogin(param);
        return BasicResult.success(result);
    }

    /**
     * 微信登录
     *
     * @author 暹罗
     */
    @PostMapping(value = "/weChat/login")
    public BasicResult wxLogin(@RequestBody @Validated(value = {}) MemberParam param) {
        this.applyWxHeaders(param);
        MemberResult result = memberService.wxLogin(param);
        return BasicResult.success(result);
    }

    /**
     * 微信静默登录
     *
     *  @author 暹罗
     */
    @PostMapping(value = "/weChat/silence/login")
    public BasicResult wxSilenceLogin(@RequestBody @Validated(value = {}) MemberParam param) {
        MemberResult result = memberService.wxSilenceLogin(param);
        return BasicResult.success(result);
    }

    /**
     * 获取登录用户信息
     *
     * @author 暹罗
     */
    @PostMapping(value = "/getLoginMemberInfo")
    public BasicResult getLoginMemberInfo(@RequestBody @Validated(value = {}) MemberParam param) {
        MemberResult result = memberService.getLoginMemberInfo(param);
        return BasicResult.success(result);
    }

    /**
     * 解除账号绑定的微信
     *
     * @author 暹罗
     */
    @PostMapping(value = "/removeBindingWx")
    public BasicResult removeBindingWx(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.removeBindingWx(param);
        return BasicResult.success();
    }

    /**
     * 上传头像
     *
     * @author 暹罗
     */
    @PostMapping(value = "/uploadHeadImg")
    public BasicResult uploadHeadImg(@RequestParam("file") MultipartFile file) {
        // ⚠️ 原签名是 @RequestBody MemberParam，但 Service 实现读的是 param.getFile()（MultipartFile）——
        // @RequestBody 只绑定 JSON body，file 字段永远为 null，该接口此前必然抛「图片不能为空」。
        // 改为标准 multipart 接收（与 UploadController.uploadSingleImage 一致），Service 签名保持不变。
        MemberParam param = new MemberParam();
        param.setFile(file);
        memberService.uploadHeadImg(param);
        return BasicResult.success();
    }

    /**
     * 修改登录密码
     *
     * @author 暹罗
     */
    @PostMapping(value = "/updatePassword")
    public BasicResult updatePassword(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.updatePassword(param);
        return BasicResult.success();
    }

    /**
     * 登出
     *
     * @author 暹罗
     */
    @PostMapping(value = "/logout")
    public BasicResult logout(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.logout(param);
        return BasicResult.success();
    }

    /**
     * 修改个人资料(邮箱地址、性别、真实姓名)
     *
     * @author 暹罗
     */
    @PostMapping(value = "/update")
    public BasicResult update(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.update(param);
        return BasicResult.success();
    }

    /**
     * 绑定微信（当前登录会员 + wx.login 的 code 换 openid）。
     * 此前「绑定微信」没有可用入口：唯一写 is_bind_wx 的路径是微信登录，
     * 而登录页的微信手机号登录被禁（需手机号快速验证组件 + 旧解密流已废弃）→ 用户永远停在「未绑定」。
     *
     * @author 暹罗
     */
    @PostMapping(value = "/bindWx")
    public BasicResult bindWx(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.bindWx(param);
        return BasicResult.success();
    }

    /**
     * 修改是否需要弹出新人引导提示为否
     *
     * @author 暹罗
     */
    @PostMapping(value = "/updateIsRemindNewPeople")
    public BasicResult updateIsRemindNewPeople(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.updateIsRemindNewPeople(param);
        return BasicResult.success();
    }

    /**
     * 修改用户的是否需要请求授权服务通知状态为否/告知已发起过请求授权服务通知
     *
     * @author 暹罗
     */
    @PostMapping(value = "/updateIsRequestWxNotify")
    public BasicResult updateIsRequestWxNotify(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.updateIsRequestWxNotify(param);
        return BasicResult.success();
    }

    /**
     * 上报使用/进入小程序的定位地址
     *
     * @author 暹罗
     */
    @PostMapping(value = "/updateLastUseAddress")
    public BasicResult updateLastUseAddress(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.updateLastUseAddress(param);
        return BasicResult.success();
    }

    /**
     * 设置/找回支付密码第一步
     *
     * @author 暹罗
     */
    @RequestMapping(value = "/forgetPaymentPassword/step1",method = RequestMethod.POST)
    public BasicResult forgetPaymentPasswordStep1(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.forgetPaymentPasswordStep1(param);
        return BasicResult.success();
    }

    /**
     * 设置/找回支付密码第二步
     *
     * @author 暹罗
     */
    @RequestMapping(value =  "/forgetPaymentPassword/step2",method = RequestMethod.POST)
    public BasicResult forgetPaymentPasswordStep2(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.forgetPaymentPasswordStep2(param);
        return BasicResult.success();
    }

    /**
     * 验证支付密码是否正确(场景：支付、提现)
     *
     * @author 暹罗
     */
    @RequestMapping(value =  "/verifyPaymentPassword",method = RequestMethod.POST)
    @ApiOperation(value = "")
    public BasicResult verifyPaymentPassword(@RequestBody @Validated(value = {}) MemberParam param) {
        memberService.verifyPaymentPassword(param);
        return BasicResult.success();
    }



//    /**
//     * 登录-手机验证码
//     *
//     * @author 暹罗
//     */
//
//    @PostMapping(value = "/loginByMobile")
//    public BasicResult loginByMobile(@RequestBody @Validated(value = {}) MemberParam param) {
//        MemberResult result = memberService.loginByMobile(param);
//        return BasicResult.success(result);
//    }
}