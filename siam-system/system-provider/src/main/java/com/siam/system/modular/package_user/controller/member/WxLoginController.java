package com.siam.system.modular.package_user.controller.member;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.siam.package_common.constant.BasicResultCode;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_weixin_basic.config.WxCode;
import com.siam.package_weixin_basic.config.WxEncrypted;
import com.siam.package_weixin_basic.config.WxSession;
import com.siam.package_weixin_pay.util.WxdecodeUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @CalssName WxLoginController
 * @Description TODO
 * @Author WP
 * @Date 2019/11/18 10:44
 **/
@Slf4j
@RestController
@RequestMapping(value = "/rest/wxLogin")
@Transactional(rollbackFor = Exception.class)
@Component
@ConfigurationProperties(value = "wxlogin")
public class WxLoginController {
    private static String appId;//AppID(小程序ID)
    private static String secret;//AppSecret(小程序密钥)

    /**
     * @Description 通过appId、secret和code获取微信提供的openid、session_key和unionid
     * @Author WP
     * @Date 2019/11/19 13:49
     * @Param [appId, secret, code]
     * @Return com.siam.package_common.config.WxSession
     **/
    public static WxSession getSessionKeyFromWxByCode(String code) {
        //appid:小程序 appId;secret:小程序 appSecret;js_code:登录时获取的 code;grant_type:授权类型，此处只需填写 authorization_code
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + secret
                + "&js_code=" + code + "&grant_type=authorization_code";

        //容错：云端容器可能访问不了微信接口（网络受限/被拦），此时不应让整个登录流程崩掉——
        //失败只意味着拿不到 openid，手机验证码登录仍应成功（见 04-deploy-cost/mvp-migration/云托管部署工作日志.md）
        try {
            RestTemplate restTemplate = new RestTemplate();
            // 设置超时，避免网络不通时长时间挂住
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(8000);
            restTemplate.setRequestFactory(factory);

            // 进行网络请求,访问url接口
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (responseEntity != null) {
                //获取到openid:用户唯一标识,session_key:会话密钥,unionid:用户在开放平台的唯一标识符,在满足 UnionID 下发条件的情况下会返回
                String sessionData = responseEntity.getBody();
                log.debug("-------------------------" + sessionData);
                Gson gson = new Gson();
                // 解析从微信服务器获得的openid和session_key;
                WxSession wsSession = gson.fromJson(sessionData, WxSession.class);
                if (wsSession == null) {
                    wsSession = new WxSession();
                }
                log.debug(wsSession.getSession_key());
                return wsSession;
            }
        } catch (Exception e) {
            log.warn("\n\n调用微信 jscode2session 失败（不影响手机验证码登录，仅拿不到 openid）：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return new WxSession();
    }

    /**
     * @Description code：小程序的wx.login返回的code,用户点击一键登录按钮时返回的iv和encryptedData
     * 把小程序传过来的用户加密信息转成用户对象
     * @Author WP
     * @Date 2019/11/19 13:51
     * @Param [code, iv, encryptedData]
     * @Return com.siam.package_common.config.WxEncrypted
     **/
    public WxEncrypted getWxUserInfo(String code, String iv, String encryptedData) throws Exception {
        WxSession wsSession = getSessionKeyFromWxByCode(code);
        log.debug(encryptedData);
        //容错提示：新基础库不再下发 encryptedData/iv，或云端访问不到微信接口时，给出可读提示而不是「系统异常」
        if (wsSession == null || wsSession.getSession_key() == null || encryptedData == null || iv == null) {
            throw new StoneCustomerException("获取微信信息失败，请改用手机验证码登录");
        }
        //通过session_key和encryptedData解密微信用户信息
        String str = WxdecodeUtils.decryptData(encryptedData, wsSession.getSession_key(), iv);
        log.debug(str + "------");
        if (str != null) {
            Gson gson = new Gson();
            //解析从微信服务器获得的手机号码及其他;
            WxEncrypted wxEncrypted = gson.fromJson(str, WxEncrypted.class);
            wxEncrypted.setOpenid(wsSession.getOpenid());
            log.debug(wxEncrypted.getPurePhoneNumber());
            return wxEncrypted;
        }
        return null;
    }

    @ApiOperation(value = "获取微信手机号")
    @PostMapping(value = "/getWxPhone")
    public BasicResult getWxPhone(@RequestBody WxCode wxCode) throws Exception {
        BasicData basicResult = new BasicData();
        /*
         1. 得用wxCode从wx服务器拿到session_key
         2. 利用session_key解密wxEncryptedData,得到手机号码
        */
        WxEncrypted wxEncrypted = this.getWxUserInfo(wxCode.getWxCode(), wxCode.getWxIV(), wxCode.getEncryptedData());

        if (wxEncrypted == null) {
            throw new StoneCustomerException("操作失败");
        }
        basicResult.setData(wxEncrypted);
        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("操作成功");
        return basicResult;
    }

    @ApiOperation(value = "微信静默登录获取用户信息")
    @PostMapping(value = "/silence/getLogin")
    public BasicResult getLogin(@RequestBody JSONObject params) {
        BasicData basicResult = new BasicData();
        /*
         1. 得用wxCode从wx服务器拿到session_key
         2. 利用session_key解密wxEncryptedData,得到手机号码
        */
        WxSession wxSession = this.getSessionKeyFromWxByCode(params.getString("wxCode"));

        if (wxSession == null) {
            throw new StoneCustomerException("登录失败");
        }
        basicResult.setData(wxSession);
        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("操作成功");
        return basicResult;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
