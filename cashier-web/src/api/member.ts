import { http } from './http'

/**
 * 会员接口（商家侧 /rest/merchant/member/**）
 *
 * 用途一：收银端 W-17 存酒登记的手机号查会员（与本文件同 URL，见 api/wine.ts）
 * 用途二：收银端 **W-27 会员现金充值** —— 顾客到店付现金办卡/储值，店员当场录入
 */

/**
 * 查会员返回（比 wine.ts 的 MerchantMemberBrief 多带余额类字段）
 *
 * 后端 2026-09-24 起 selectByMobile 额外下发 balance/growth/type/vipNo，
 * 老调用方（存酒）只取 id/name/mobile，不受影响。
 */
export interface MerchantMemberForRecharge {
  id: number
  nickname: string | null
  name: string | null
  /** 已脱敏，如 138****8601 */
  mobile: string | null
  /** 当前余额（元）—— 充值时店员需向顾客交代充前余额 */
  balance: number
  /** 累计成长值 */
  growth: number
  /** 1 普通用户 / 2 VIP 会员 */
  type: number | null
  vipNo: string | null
}

/** 手机号精确查会员（供充值页使用） */
export const lookupMemberForRecharge = (mobile: string) =>
  http.post<unknown, MerchantMemberForRecharge>('/rest/merchant/member/selectByMobile', { mobile })

export interface CashRechargePayload {
  memberId: number
  /** 本次实收现金（元），> 0 且单笔 ≤ 5000 */
  amount: number
  /** 赠送余额（元），不传/0 = 不赠送 */
  giveBalance?: number
  remark?: string
}

export interface CashRechargeResult {
  /** 现金充值单号（XR 前缀，便于运营筛选） */
  orderNo: string
  amount: number
  giveBalance: number
  /** 充值后余额（元） */
  balanceAfter: number
}

/**
 * 会员现金充值（W-27）
 *
 * 一次调用完成：写交易单（记经手人）→ 写充值单（channel=2）→ 加余额 → 写流水。
 * 不走微信支付，现金当场收讫，服务端直接落 status=2。
 */
export const cashRecharge = (p: CashRechargePayload) =>
  http.post<unknown, CashRechargeResult>('/rest/merchant/member/recharge', p)

/** 金额格式化：保留 2 位小数 */
export const money = (v: number | null | undefined) =>
  (Number(v) || 0).toFixed(2)
