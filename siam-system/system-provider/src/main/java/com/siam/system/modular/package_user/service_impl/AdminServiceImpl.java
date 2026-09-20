package com.siam.system.modular.package_user.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.constant.BusinessType;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.Base64Utils;
import com.siam.package_common.util.CommonUtils;
import com.siam.package_common.util.OSSUtils;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_goods.mapper.ShopMapper;
import com.siam.system.modular.package_goods.service.SmsLogService;
import com.siam.system.modular.package_goods.entity.SmsLog;
import com.siam.system.modular.package_log.entity.LoginLog;
import com.siam.system.modular.package_log.service.LoginLogService;
import com.siam.system.modular.package_log.util.LogUtil;
import com.siam.system.modular.package_user.auth.cache.AdminSessionManager;
import com.siam.system.modular.package_user.auth.cache.MerchantSessionManager;
import com.siam.system.modular.package_user.entity.Admin;
import com.siam.system.modular.package_user.entity.Merchant;
import com.siam.system.modular.package_user.mapper.AdminMapper;
import com.siam.system.modular.package_user.mapper.MerchantMapper;
import com.siam.system.modular.package_user.model.example.AdminExample;
import com.siam.system.modular.package_user.model.param.AdminParam;
import com.siam.system.modular.package_user.model.result.AdminResult;
import com.siam.system.modular.package_user.service.*;
import com.siam.system.util.TokenUtil;
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
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminSessionManager adminSessionManager;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private MerchantSessionManager merchantSessionManager;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private AdminTokenService adminTokenService;

    @Autowired
    private OSSUtils ossUtils;

//    @Autowired
//    private ShopService shopService;
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private ShoppingCartService shoppingCartService;
//
//    @Autowired
//    private SystemUsageRecordService systemUsageRecordService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SmsLogService smsLogService;

    @Autowired
    private MerchantWithdrawRecordService merchantWithdrawRecordService;

    @Autowired
    private LoginLogService loginLogService;

//    @Autowired
//    private ShopChangeRecordService shopChangeRecordService;
//
//    @Autowired
//    private OrderRefundService orderRefundService;
//
//    @Autowired
//    private PointsMallOrderRefundService pointsMallOrderRefundService;
//
//    @Resource(name = "pointsMallOrderServiceImpl")
//    private PointsMallOrderService pointsMallOrderService;
//
//    @Autowired
//    private PointsMallShoppingCartService pointsMallShoppingCartService;
//
//    @Autowired
//    private PointsMallGoodsService pointsMallGoodsService;

    @Autowired
    private MemberTradeRecordService memberTradeRecordService;

    public int countByExample(AdminExample example){
        return adminMapper.countByExample(example);
    }

    public void insertSelective(Admin record){
        adminMapper.insertSelective(record);
    }

    public List<Admin> selectByExample(AdminExample example){
        return adminMapper.selectByExample(example);
    }

    public Admin selectByPrimaryKey(Integer id){
        return adminMapper.selectByPrimaryKey(id);
    }

    public void updateByExampleSelective(Admin record, AdminExample example){
        adminMapper.updateByExampleSelective(record, example);
    }

    public void updateByPrimaryKeySelective(Admin record){
        adminMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public String encryptionBySalt(String password, String passwordSalt) {
        // 生成盐

        return null;
    }

    @Override
    public Page getListByPage(AdminParam param) {
        Page<Map<String, Object>> page = adminMapper.getListByPage(new Page(param.getPageNo(), param.getPageSize()), param);
        return page;
    }

    @Override
    public Admin selectByUsername(String username) {
        return adminMapper.selectByUsername(username);
    }

    @Override
    public AdminResult login(AdminParam param) {
        try {
            Admin dbAdmin = adminMapper.selectByUsername(param.getUsername());
            if(dbAdmin == null){
                throw new StoneCustomerException("该账号不存在");
            }

            // 判断密码是否匹配
            String password = Base64Utils.decode(param.getPassword());
            password = CommonUtils.genMd5Password(password, dbAdmin.getPasswordSalt());
            if(!password.equals(dbAdmin.getPassword())){
                throw new StoneCustomerException("密码错误");
            }

            // 更新管理员登录信息
            Admin updateAdmin = new Admin();
            updateAdmin.setId(dbAdmin.getId());
            updateAdmin.setLastLoginTime(new Date());
            adminMapper.updateByPrimaryKeySelective(updateAdmin);

            // 生成token
            String token = TokenUtil.generateToken(dbAdmin);

            //创建登录会话
            adminSessionManager.createSession(token, dbAdmin);

            //单店桥接：同步建立所属商家的会话（管理端既有 /rest/merchant/** 页面依赖该会话）
            this.bridgeMerchantSession(token);

            //创建登录cookie
            TokenUtil.addLoginCookie(token);

            //记录登录日志-成功
            this.writeLoginLog(param.getUsername(), LoginLog.RESULT_SUCCESS, null);

            AdminResult result = new AdminResult();
            result.setToken(token);
            return result;
        } catch (RuntimeException e) {
            //记录登录日志-失败（失败原因即异常信息）
            this.writeLoginLog(param.getUsername(), LoginLog.RESULT_FAIL, e.getMessage());
            throw e;
        }
    }

    @Override
    public AdminResult loginByMobile(AdminParam param) {
        try {
            if("123456".equals(param.getMobileCode())) {
                //万能验证码，放行
            }else {
                // 判断验证码是否匹配
                SmsLog smsLog = smsLogService.getLastLog(param.getMobile(), BusinessType.SMS_LOG_TYPE_LOGIN, 5);
                if (smsLog==null || !smsLog.getVerifyCode().equals(param.getMobileCode())) {
                    throw new StoneCustomerException("手机验证码错误");
                }
            }

            // 判断是否已经注册
            Admin dbAdmin = adminMapper.selectByMobile(param.getMobile());
            if(dbAdmin == null){
                throw new StoneCustomerException("该账号不存在");
            }

            // 生成token
            String token = TokenUtil.generateToken(dbAdmin);

            //创建登录会话
            adminSessionManager.createSession(token, dbAdmin);

            //单店桥接：同步建立所属商家的会话（管理端既有 /rest/merchant/** 页面依赖该会话）
            this.bridgeMerchantSession(token);

            //创建登录cookie
            TokenUtil.addLoginCookie(token);

            //记录登录日志-成功（账号取登录账号名）
            this.writeLoginLog(dbAdmin.getUsername(), LoginLog.RESULT_SUCCESS, null);

            AdminResult result = new AdminResult();
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
            loginLogService.insert(LogUtil.buildLoginLog(account, LoginLog.ACCOUNT_TYPE_ADMIN, result, message));
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    @Override
    public AdminResult getLoginAdminInfo(AdminParam param) {
        Admin loginAdmin = adminSessionManager.getSession(TokenUtil.getToken());

        Admin dbAdmin = adminMapper.selectByPrimaryKey(loginAdmin.getId());

        AdminResult result = new AdminResult();
        BeanUtils.copyProperties(dbAdmin, result);
        return result;
    }

    @Override
    public void updatePassword(AdminParam param) {
        Admin loginAdmin = adminSessionManager.getSession(TokenUtil.getToken());
        Admin dbAdmin = adminMapper.selectByPrimaryKey(loginAdmin.getId());

        //Base64解密
        String oldPassword = Base64Utils.decode(param.getOldPassword());
        String newPassword = Base64Utils.decode(param.getNewPassword());

        String passwordSalt = dbAdmin.getPasswordSalt();
        oldPassword = CommonUtils.genMd5Password(oldPassword, passwordSalt);
        if(!dbAdmin.getPassword().equals(oldPassword)){
            throw new StoneCustomerException("原密码错误");
        }

        if(StringUtils.isNotBlank(newPassword) && newPassword.length()<6){
            throw new StoneCustomerException("密码长度最少6位");
        }

        newPassword = CommonUtils.genMd5Password(newPassword, passwordSalt);

        //修改管理员密码
        Admin updateAdmin = new Admin();
        updateAdmin.setId(dbAdmin.getId());
        updateAdmin.setPassword(newPassword);
        adminMapper.updateByPrimaryKeySelective(updateAdmin);
    }

    @Override
    public void logout(AdminParam param) {
        Admin loginAdmin = adminSessionManager.getSession(TokenUtil.getToken());

//        //记录退出日志
//        LogManager.me().executeLog(LogTaskFactory.exitLog(LoginContextHolder.getContext().getUser().getId(), getIp()));

        //删除Auth相关cookies
        TokenUtil.deleteLoginCookie();

        //删除会话
        adminSessionManager.removeSession(TokenUtil.getToken());

        //单店桥接：同步清理商家会话（与 bridgeMerchantSession 对称）
        merchantSessionManager.removeSession(TokenUtil.getToken());
    }

    @Override
    public void forgetPasswordStep1(AdminParam param) {
        if("123456".equals(param.getMobileCode())) {
            //万能验证码，放行
        }else {
            // 判断验证码是否匹配
            SmsLog smsLog = smsLogService.getLastLog(param.getMobile(), BusinessType.SMS_LOG_TYPE_FINDPWD, 5);
            if (smsLog==null || !smsLog.getVerifyCode().equals(param.getMobileCode())) {
                throw new StoneCustomerException("手机验证码错误");
            }
        }

        //TODO-将验证码改为已验证

        // 判断是否已经注册
        // 修复：原为 selectByUsername(mobileCode)，验证码当用户名查永远查不到，改为按手机号查
        Admin dbAdmin = adminMapper.selectByMobile(param.getMobile());
        if(dbAdmin == null){
            throw new StoneCustomerException("该账号不存在");
        }
    }

    @Override
    public void forgetPasswordStep2(AdminParam param) {
        //TODO-长时间未操作，请返回第一步重新操作

        // 判断是否已经注册
        Admin dbAdmin = adminMapper.selectByMobile(param.getMobile());
        if(dbAdmin == null){
            throw new StoneCustomerException("该账号不存在");
        }

        //Base64解密
        String password = Base64Utils.decode(param.getPassword());
        String passwordSalt = dbAdmin.getPasswordSalt();
        if(StringUtils.isNotBlank(password) && password.length()<6){
            throw new StoneCustomerException("密码长度最少6位");
        }
        password = CommonUtils.genMd5Password(password, passwordSalt);

        //修改商家密码
        Admin updateAdmin = new Admin();
        updateAdmin.setId(dbAdmin.getId());
        updateAdmin.setPassword(password);
        adminMapper.updateByPrimaryKeySelective(updateAdmin);
    }

    /**
     * 单店桥接：管理员登录后，用同一 token 建立该门店所属商家的会话。
     *
     * 背景（grill session 2026-09-19 架构决策 #1）：
     * 管理端（vue-siam-shop）改为管理员账号登录，但既有大量页面（商品/订单/台位/门店信息等）
     * 调用的是 /rest/merchant/** 接口，其拦截器只认 MerchantSessionManager 中的商家会话。
     * 单店口径下，管理员登录时同步桥接建立唯一门店所属商家的会话，
     * 使既有页面零改造继续可用；MerchantInterceptor 与前端页面均不改。
     *
     * 规则：优先取「已上架」门店（tb_shop.status=2）；兜底取 id 最小的一条（保证确定性）。
     * 失败不阻断登录（仅告警），管理员侧 /rest/admin/** 功能不受影响。
     */
    private void bridgeMerchantSession(String token) {
        try {
            //单店：优先取已上架(status=2)门店；兜底取 id 最小的一条
            List<Shop> shopList = shopMapper.selectList(new QueryWrapper<Shop>().eq("status", 2).orderByAsc("id"));
            if (shopList == null || shopList.isEmpty()) {
                shopList = shopMapper.selectList(new QueryWrapper<Shop>().orderByAsc("id"));
            }
            if (shopList == null || shopList.isEmpty()) {
                log.warn("管理员登录桥接商家会话失败：tb_shop 无门店数据，token={}", token);
                return;
            }
            Shop shop = shopList.get(0);
            if (shop.getMerchantId() == null) {
                log.warn("管理员登录桥接商家会话失败：门店[{}]未关联商家(merchant_id为空)", shop.getId());
                return;
            }
            //优先取该门店的「超级商家」账号（username 以 admin- 开头）：
            //MerchantPermissionAop 仅放行此类商家（MerchantLoginContextSpringServiceImpl.isSuperAdmin），
            //管理端带 @MerchantPermission 的接口（商品/菜单/库存等写操作）需超管身份才能通过
            Merchant merchant = null;
            List<Merchant> shopMerchantList = merchantMapper.selectList(new QueryWrapper<Merchant>().eq("shop_id", shop.getId()));
            if (shopMerchantList != null) {
                for (Merchant m : shopMerchantList) {
                    if (m.getUsername() != null && m.getUsername().startsWith("admin-")) {
                        merchant = m;
                        break;
                    }
                }
            }
            if (merchant == null) {
                merchant = merchantMapper.selectByPrimaryKey(shop.getMerchantId());
            }
            if (merchant == null) {
                log.warn("管理员登录桥接商家会话失败：商家[{}]不存在", shop.getMerchantId());
                return;
            }
            //桥接前先按 token 清理历史商家会话：
            //管理员 JWT 对同一账号是确定性的（同 token 复用），DB 版会话按 token 查询取首行，
            //若残留旧行（如上一版桥接指向的普通商家）会导致身份判定错误，先清再建保证唯一
            merchantSessionManager.removeSession(token);
            merchantSessionManager.createSession(token, merchant);
            log.info("管理员登录桥接商家会话成功：shopId={}, merchantId={}", shop.getId(), merchant.getId());
        } catch (Exception e) {
            log.error("管理员登录桥接商家会话异常", e);
        }
    }
}