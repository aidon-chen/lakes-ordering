package com.siam.system.modular.package_user.service_impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.constant.BusinessType;
import com.siam.package_common.constant.Quantity;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.Base64Utils;
import com.siam.package_common.util.CommonUtils;
import com.siam.package_common.util.OSSUtils;
import com.siam.system.modular.package_goods.service.SmsLogService;
import com.siam.system.modular.package_goods.entity.*;
import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.modular.package_log.service.LoginLogService;
import com.siam.system.modular.package_log.util.LogUtil;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.modular.package_user.mapper.MerchantBillingRecordMapper;
import com.siam.system.modular.package_user.mapper.MerchantMapper;
import com.siam.system.modular.package_user.model.example.MerchantExample;
import com.siam.system.modular.package_user.model.param.MerchantParam;
import com.siam.system.modular.package_user.model.result.MerchantResult;
import com.siam.system.modular.package_user.service.MerchantService;
import com.siam.system.util.TokenUtil;
import com.siam.system.modular.package_goods.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private MerchantBillingRecordMapper merchantBillingRecordMapper;

    @Autowired
    private OSSUtils ossUtils;

    @Autowired
    private SmsLogService smsLogService;

    @Autowired
    private ShopService shopService;

//    @Autowired
//    private OrderService orderService;

//    @Autowired
//    private ShoppingCartService shoppingCartService;
//
//    @Autowired
//    private SystemUsageRecordService systemUsageRecordService;

    @Autowired
    private CouponsShopRelationService couponsShopRelationService;

    @Autowired
    private CouponsService couponsService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LoginLogService loginLogService;

    //    @Override
//    public void updateBalance(MerchantBillingRecord merchantBillingRecord) {
//        Merchant dbMerchant = merchantMapper.selectByPrimaryKey(merchantBillingRecord.getMerchantId());
//        if(dbMerchant == null){
//            throw new StoneCustomerException("该商家信息不存在");
//        }
//
//        //修改商家余额
//        BigDecimal updateBalance = merchantBillingRecord.getOperateType() == Quantity.INT_1
//                ? dbMerchant.getBalance().add(merchantBillingRecord.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP)
//                : dbMerchant.getBalance().subtract(merchantBillingRecord.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP);;
//
//        BigDecimal updateWithdrawableBalance = merchantBillingRecord.getOperateType() == Quantity.INT_1
//                ? dbMerchant.getWithdrawableBalance().add(merchantBillingRecord.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP)
//                : dbMerchant.getWithdrawableBalance().subtract(merchantBillingRecord.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP);;
//
//        Merchant updateMerchant = new Merchant();
//        updateMerchant.setId(merchantBillingRecord.getMerchantId());
//        updateMerchant.setBalance(updateBalance);
//        updateMerchant.setWithdrawableBalance(updateWithdrawableBalance);
//        updateMerchant.setUpdateTime(new Date());
//        merchantMapper.updateByPrimaryKeySelective(updateMerchant);
//
//        //增加商家账单记录
//        merchantBillingRecord.setCreateTime(new Date());
//        merchantBillingRecordMapper.insertSelective(merchantBillingRecord);
//    }

    public int countByExample(MerchantExample example){
        return merchantMapper.countByExample(example);
    }

    public void insertSelective(Merchant record){
        merchantMapper.insertSelective(record);
    }

    public List<Merchant> selectByExample(MerchantExample example){
        return merchantMapper.selectByExample(example);
    }

    public Merchant selectByPrimaryKey(Integer id){
        return merchantMapper.selectByPrimaryKey(id);
    }

    public void updateByExampleSelective(Merchant record, MerchantExample example){
        merchantMapper.updateByExampleSelective(record, example);
    }

    public void updateByPrimaryKeySelective(Merchant record){
        merchantMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public String encryptionBySalt(String password, String passwordSalt) {
        // 生成盐

        return null;
    }

    @Override
    public Page getListByPage(int pageNo, int pageSize, Merchant merchant) {
        Page<Map<String, Object>> page = merchantMapper.getListByPage(new Page(pageNo, pageSize), merchant);
        return page;
    }

    @Override
    public Merchant selectByUsernameOrMobile(String username) {
        return merchantMapper.selectByUsernameOrMobile(username);
    }

    @Override
    public Merchant selectByMobile(String mobile) {
        return merchantMapper.selectByMobile(mobile);
    }

    @Override
    public MerchantResult login(MerchantParam param) {
        try {
            Merchant dbMerchant = merchantMapper.selectByUsernameOrMobile(param.getUsername());
            if(dbMerchant == null){
                throw new StoneCustomerException("该账号不存在");
            }

            // 判断密码是否匹配
            String password = Base64Utils.decode(param.getPassword());
            password = CommonUtils.genMd5Password(password, dbMerchant.getPasswordSalt());
            if(!password.equals(dbMerchant.getPassword())){
                throw new StoneCustomerException("密码错误");
            }

            // 更新merchant记录
            /*Merchant updateMerchant = new Merchant();
            updateMerchant.setId(dbMerchant.getId());
            merchantMapper.updateByPrimaryKeySelective(updateMerchant);*/

            // 生成token
            String token = TokenUtil.generateToken(dbMerchant);

            //创建登录会话
            merchantSessionManager.createSession(token, dbMerchant);

            //创建登录cookie
            TokenUtil.addLoginCookie(token);

            //记录登录日志-成功
            this.writeLoginLog(param.getUsername(), LoginLog.RESULT_SUCCESS, null);

            MerchantResult result = new MerchantResult();
            result.setToken(token);
            return result;
        } catch (RuntimeException e) {
            //记录登录日志-失败（失败原因即异常信息）
            this.writeLoginLog(param.getUsername(), LoginLog.RESULT_FAIL, e.getMessage());
            throw e;
        }
    }

    @Override
    public MerchantResult loginByMobile(MerchantParam param) {
        try {
            if("123456".equals(param.getMobileCode())) {
                //万能验证码，放行
            }else{
                // 判断验证码是否匹配
                SmsLog smsLog = smsLogService.getLastLog(param.getMobile(), BusinessType.SMS_LOG_TYPE_LOGIN, 5);
                if (smsLog==null || !smsLog.getVerifyCode().equals(param.getMobileCode())) {
                    throw new StoneCustomerException("手机验证码错误");
                }
            }

            //TODO-将验证码改为已验证

            // 判断是否已经注册
            Merchant dbMerchant = merchantMapper.selectByMobile(param.getMobile());
            if(dbMerchant == null){
                throw new StoneCustomerException("该账号不存在");
            }

            // 生成token
            String token = TokenUtil.generateToken(dbMerchant);

            //创建登录会话
            merchantSessionManager.createSession(token, dbMerchant);

            //创建登录cookie
            TokenUtil.addLoginCookie(token);

            //记录登录日志-成功（账号取登录账号名）
            this.writeLoginLog(dbMerchant.getUsername(), LoginLog.RESULT_SUCCESS, null);

            MerchantResult result = new MerchantResult();
            result.setToken(token);
            return result;
        } catch (RuntimeException e) {
            //记录登录日志-失败（失败原因即异常信息）
            this.writeLoginLog(param.getMobile(), LoginLog.RESULT_FAIL, e.getMessage());
            throw e;
        }
    }

    /**
     * 记录登录日志：任何异常都不得影响登录主流程
     */
    private void writeLoginLog(String account, Integer result, String message) {
        try {
            loginLogService.insert(LogUtil.buildLoginLog(account, LoginLog.ACCOUNT_TYPE_MERCHANT, result, message));
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    @Override
    public MerchantResult registerByMobile(MerchantParam param) {
        if("123456".equals(param.getMobileCode())) {
            //万能验证码，放行
        }else{
            // 判断验证码是否匹配
            SmsLog smsLog = smsLogService.getLastLog(param.getMobile(), BusinessType.SMS_LOG_TYPE_REGISTER, 5);
            if (smsLog==null || !smsLog.getVerifyCode().equals(param.getMobileCode())) {
                throw new StoneCustomerException("手机验证码错误");
            }
        }

        // 判断是否已经注册
        Merchant dbMerchant = merchantMapper.selectByMobile(param.getMobile());
        if (dbMerchant != null) {
            //用户已经注册，直接进行登录操作
        }else{
            //默认密码为123456
            String passwordSalt = CommonUtils.genSalt();
            String password = "123456";
            password = CommonUtils.genMd5Password(password, passwordSalt);

            // 添加merchant记录
            Merchant insertMerchant = new Merchant();
            insertMerchant.setUsername(param.getUsername() != null ? param.getUsername() : "admin-" + param.getMobile());
            insertMerchant.setMobile(param.getMobile());
            insertMerchant.setPassword(password);
            insertMerchant.setPasswordSalt(passwordSalt);
            insertMerchant.setIsDisabled(false);
            insertMerchant.setHeadImg(null);
            insertMerchant.setRoles(null);
            insertMerchant.setSex(null);
            insertMerchant.setEmail(null);
            insertMerchant.setIsDisabled(false);
            insertMerchant.setIsDeleted(false);
            insertMerchant.setAuditStatus(Quantity.INT_1);
            insertMerchant.setCreateTime(new Date());
            insertMerchant.setUpdateTime(new Date());
            merchantMapper.insertSelective(insertMerchant);

            //同步创建一条店铺信息
            Shop insertShop = new Shop();
            insertShop.setMerchantId(insertMerchant.getId());
            insertShop.setAuditStatus(Quantity.INT_1);
            insertShop.setCreateTime(new Date());
            insertShop.setUpdateTime(new Date());
            shopService.save(insertShop);
            int shopId = insertShop.getId();

            //商家账号绑定店铺信息
            Merchant updateMerchant = new Merchant();
            updateMerchant.setId(insertMerchant.getId());
            updateMerchant.setShopId(shopId);
            merchantMapper.updateByPrimaryKeySelective(updateMerchant);

            //建立店铺与系统默认优惠券ID-新人3折卷的关联关系
            Coupons dbCoupons = couponsService.selectByPrimaryKey(BusinessType.NEW_PEOPLE_COUPONS_ID);
            if(dbCoupons != null){
                CouponsShopRelation insertCouponsShopRelation = new CouponsShopRelation();
                insertCouponsShopRelation.setCouponsId(BusinessType.NEW_PEOPLE_COUPONS_ID);
                insertCouponsShopRelation.setShopId(shopId);
                insertCouponsShopRelation.setCreateTime(new Date());
                couponsShopRelationService.insertSelective(insertCouponsShopRelation);
            }else{
                log.error("系统默认优惠券-新人3折卷不存在");
            }

            //建立店铺与系统默认优惠券ID-推荐新人3折卷的关联关系
            Coupons dbCouponsInvite = couponsService.selectByPrimaryKey(BusinessType.INVITE_NEW_PEOPLE_COUPONS_ID);
            if(dbCouponsInvite != null){
                CouponsShopRelation insertCouponsShopRelationInvite = new CouponsShopRelation();
                insertCouponsShopRelationInvite.setCouponsId(BusinessType.INVITE_NEW_PEOPLE_COUPONS_ID);
                insertCouponsShopRelationInvite.setShopId(shopId);
                insertCouponsShopRelationInvite.setCreateTime(new Date());
                couponsShopRelationService.insertSelective(insertCouponsShopRelationInvite);
            }else{
                log.error("系统默认优惠券-推荐新人3折卷不存在");
            }

            //TODO-给调度后台发送一条系统消息
            SysMessage sysMessage = new SysMessage();
            sysMessage.setUserId(BusinessType.ADMIN_ID);
            sysMessage.setUserType(SysMessage.USER_TYPE_ADMIN);
            sysMessage.setTitle("新商家注册提醒");
            sysMessage.setContent("商家信息：\n账号: " + insertMerchant.getUsername() + "\n手机号: " + insertMerchant.getMobile());
            messageService.insertSelective(sysMessage);

            //从数据库中获取注册好的用户信息
            dbMerchant = merchantMapper.selectByMobile(param.getMobile());
        }

        // 生成token
        String token = TokenUtil.generateToken(dbMerchant);

        //创建登录会话
        merchantSessionManager.createSession(token, dbMerchant);

        //创建登录cookie
        TokenUtil.addLoginCookie(token);

        MerchantResult result = new MerchantResult();
        result.setToken(token);
        return result;
    }

    @Override
    public MerchantResult getLoginMerchantInfo(MerchantParam param) {
        BasicData basicResult = new BasicData();
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        // 2026-09-25 修复：token 失效/无会话时 getSession 返回 null，原代码直接 .getId() 抛 NPE，
        // 被全局兜底成「系统异常」⇒ 前端拿不到「未登录」信号，只能弹「查不到商户信息」的误导兜底。
        // 现改为明确的业务异常（前端可据此引导重新登录）。
        if (loginMerchant == null) {
            throw new StoneCustomerException("登录已过期，请重新登录");
        }
        Merchant dbMerchant = merchantMapper.selectByPrimaryKey(loginMerchant.getId());
        if (dbMerchant == null) {
            throw new StoneCustomerException("登录商户不存在，请重新登录");
        }

        //需要返回关联的店铺信息，返回是否营业
        Map<String, Object> resultMap = com.siam.package_common.util.BeanUtils.beanToMap(dbMerchant);
        Shop shop = shopService.getById(dbMerchant.getShopId());
        if (shop != null) {
            resultMap.put("shopName", shop.getName());
            resultMap.put("shopLogoImg", shop.getShopLogoImg());
        }

        basicResult.setData(resultMap);

        MerchantResult result = new MerchantResult();
        BeanUtils.copyProperties(dbMerchant, result);
        return result;
    }

    @Override
    public void update(MerchantParam param) {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        merchantMapper.updateByPrimaryKeySelective(param);
    }

    @Override
    public void updatePassword(MerchantParam param) {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());
        Merchant dbMerchant = merchantMapper.selectByPrimaryKey(loginMerchant.getId());

        //Base64解密
        String oldPassword = Base64Utils.decode(param.getOldPassword());
        String newPassword = Base64Utils.decode(param.getNewPassword());

        String passwordSalt = dbMerchant.getPasswordSalt();
        oldPassword = CommonUtils.genMd5Password(oldPassword, passwordSalt);
        if(!dbMerchant.getPassword().equals(oldPassword)){
            throw new StoneCustomerException("原密码错误");
        }

        if(StringUtils.isNotBlank(newPassword) && newPassword.length()<6){
            throw new StoneCustomerException("密码长度最少6位");
        }

        newPassword = CommonUtils.genMd5Password(newPassword, passwordSalt);

        //修改商家密码
        Merchant updateMerchant = new Merchant();
        updateMerchant.setId(dbMerchant.getId());
        updateMerchant.setPassword(newPassword);
        merchantMapper.updateByPrimaryKeySelective(updateMerchant);
    }

    @Override
    public void logout(MerchantParam param) {
        Merchant loginMerchant = merchantSessionManager.getSession(TokenUtil.getToken());

//        //记录退出日志
//        LogManager.me().executeLog(LogTaskFactory.exitLog(LoginContextHolder.getContext().getUser().getId(), getIp()));

        //删除Auth相关cookies
        TokenUtil.deleteLoginCookie();

        //删除会话
        merchantSessionManager.removeSession(TokenUtil.getToken());
    }

    @Override
    public void forgetPasswordStep1(MerchantParam param) {
        if("123456".equals(param.getMobileCode())) {
            //万能验证码，放行
        }else{
            // 判断验证码是否匹配
            SmsLog smsLog = smsLogService.getLastLog(param.getMobile(), BusinessType.SMS_LOG_TYPE_FINDPWD, 5);
            if (smsLog==null || !smsLog.getVerifyCode().equals(param.getMobileCode())) {
                throw new StoneCustomerException("手机验证码错误");
            }
        }

        //TODO-将验证码改为已验证

        // 判断是否已经注册
        Merchant dbMerchant = merchantMapper.selectByMobile(param.getMobile());
        if(dbMerchant == null){
            throw new StoneCustomerException("该账号不存在");
        }
    }

    @Override
    public void forgetPasswordStep2(MerchantParam param) {
        //TODO-长时间未操作，请返回第一步重新操作

        // 判断是否已经注册
        Merchant dbMerchant = merchantMapper.selectByMobile(param.getMobile());
        if(dbMerchant == null){
            throw new StoneCustomerException("该账号不存在");
        }

        //Base64解密
        String password = Base64Utils.decode(param.getPassword());
        String passwordSalt = dbMerchant.getPasswordSalt();
        if(StringUtils.isNotBlank(password) && password.length()<6){
            throw new StoneCustomerException("密码长度最少6位");
        }
        password = CommonUtils.genMd5Password(password, passwordSalt);

        //修改商家密码
        Merchant updateMerchant = new Merchant();
        updateMerchant.setId(dbMerchant.getId());
        updateMerchant.setPassword(password);
        merchantMapper.updateByPrimaryKeySelective(updateMerchant);
    }
}