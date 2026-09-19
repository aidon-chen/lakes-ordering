import { http } from './http'

/**
 * 商家端账号与登录接口层
 * 契约来源：siam-system `package_user/controller/merchant/MerchantController.java`
 *          `package_goods/controller/merchant/MerchantShopController.java`
 *          `package_goods/controller/member/SmsLogController.java`
 */

/** 密码字段一律 Base64 传输（后端 Base64Utils.decode 后 md5+salt 比对） */
export const b64 = (raw: string) => btoa(unescape(encodeURIComponent(raw)))

/** 登录返回：MerchantResult extends Merchant（含 token） */
export interface MerchantInfo {
  id: number
  shopId: number
  username?: string
  mobile?: string
  nickname?: string
  headImg?: string
  sex?: number
  email?: string
  realName?: string
  /** 权限（库中有该列，当前全为 NULL —— 角色模型尚未实现） */
  roles?: string
  isDisabled?: number
  auditStatus?: number
  createTime?: string
  balance?: number
}

export interface LoginResult extends MerchantInfo {
  token: string
}

/** 商家账号密码登录 */
export const login = (username: string, password: string) =>
  http.post<unknown, LoginResult>('/rest/merchant/login', { username, password: b64(password) })

/**
 * 商家手机号验证码登录
 * 后端：万能码 `123456` 直接放行，否则比对 tb_sms_log 最近一条（5 分钟内）
 */
export const loginByMobile = (mobile: string, mobileCode: string) =>
  http.post<unknown, LoginResult>('/rest/merchant/loginByMobile', { mobile, mobileCode })

/** 短信验证码类型（对齐后端 SmsLogController type 取值） */
export type SmsType = 'register' | 'login' | 'verification' | 'findpwd'

/**
 * 发送手机验证码
 * Redis 里 smsSwitch=false 时后端抛「暂不支持获取验证码，请用临时验证码 123456 进行操作」
 */
export const sendMobileCode = (mobile: string, type: SmsType) =>
  http.post<unknown, null>('/rest/smsLog/sendMobileCode', { mobile, type })

/** 忘记密码·第一步：校验手机号与验证码 */
export const forgetPasswordStep1 = (mobile: string, mobileCode: string) =>
  http.post<unknown, null>('/rest/merchant/forgetPassword/step1', { mobile, mobileCode })

/** 忘记密码·第二步：设置新密码（注意后端字段名为 password） */
export const forgetPasswordStep2 = (mobile: string, password: string) =>
  http.post<unknown, null>('/rest/merchant/forgetPassword/step2', { mobile, password: b64(password) })

/** 退出登录 */
export const logout = () => http.post<unknown, null>('/rest/merchant/logout', {})

/** 当前登录商家信息（无参，token 由拦截器注入） */
export const getLoginMerchantInfo = () =>
  http.post<unknown, MerchantInfo>('/rest/merchant/getLoginMerchantInfo', {})

/** 当前登录商家所属门店信息 */
export interface ShopInfo {
  id: number
  name?: string
  shopLogoImg?: string
  province?: string
  city?: string
  area?: string
  street?: string
  houseNumber?: string
  startTime?: string
  endTime?: string
  isOperatingOfShop?: number
  status?: number
}

export const getLoginShopInfo = () =>
  http.post<unknown, ShopInfo>('/rest/merchant/shop/getLoginMerchantShopInfo', {})

/**
 * 修改密码（需 token）
 * 后端校验：原密码错误 → 「原密码错误」；新密码 <6 位 → 「密码长度最少6位」
 */
export const updatePassword = (oldPassword: string, newPassword: string) =>
  http.post<unknown, null>('/rest/merchant/updatePassword', {
    oldPassword: b64(oldPassword),
    newPassword: b64(newPassword)
  })
