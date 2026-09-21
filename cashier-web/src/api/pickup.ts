import { http } from './http'

/**
 * 自提核销接口（商家侧 /rest/merchant/pointsMall/order/**，批次 E · W-15 / W-16）
 *
 * ⚠️ 三个端点均以**登录商家的 shopId** 限定门店（后端用会话覆盖，前端传了也没用）。
 * ⚠️ 状态机：待自提(4) → 已完成(6)；核销写 verifyTime + verifierId；
 *    **幂等**由后端保证（已完成再次调用直接返回成功，不报错）。
 * ⚠️ 核销方式是「扫码 / 手输」，工程**无该字段**（未持久化）→ 页面上不做这一列。
 */

/** 商城订单行（对齐 tb_points_mall_order；核销记录行会多带 verifierName） */
export interface PickupOrderRow {
  id: number
  orderNo: string | null
  /** 自提码（批次6 新增字段） */
  pickupCode: string | null
  /** 品项摘要，如「经典威士忌酸×2 等 2 件」 */
  description: string | null
  actualPrice: number | null
  /** 1待付款 4待自提 6已完成 …（见 PointsMallOrder 常量） */
  status: number | null
  createTime: string | null
  contactPhone: string | null
  /** 自提截止时间 */
  pickupDeadline: string | null
  /** 核销时间（核销过的才有） */
  verifyTime: string | null
  verifierId: number | null
  /** 服务端补的核销人姓名（取自 tb_merchant） */
  verifierName: string | null
}

export interface PickupQuery {
  /** 自提码（模糊） */
  pickupCode?: string
  /** 手机号 / 订单号 */
  keyWords?: string
  /** 核销人 id */
  verifierId?: number
  /** 核销时间起（yyyy-MM-dd HH:mm:ss） */
  startVerifyTime?: string
  /** 核销时间止 */
  endVerifyTime?: string
  pageNo?: number
  pageSize?: number
}

/** 待核销订单（W-15）：status=4 待自提 */
export const fetchWaitPickupList = (q: PickupQuery = {}) =>
  http.post<unknown, { records: PickupOrderRow[]; total: number }>(
    '/rest/merchant/pointsMall/order/list',
    { pageNo: -1, pageSize: 50, status: 4, ...q }
  )

/** 核销记录（W-16）：已完成 + verify_time 非空 */
export const fetchVerifyRecordList = (q: PickupQuery = {}) =>
  http.post<unknown, { records: PickupOrderRow[]; total: number }>(
    '/rest/merchant/pointsMall/order/verifyRecord/list',
    { pageNo: 1, pageSize: 200, ...q }
  )

/**
 * 自提核销：待自提(4) → 已完成(6)
 * 幂等：后端对「已完成」的订单直接返回成功。
 * 失败分支（后端文案）：该订单不存在 / 该订单当前状态不可核销，仅「待自提」订单可核销
 */
export const confirmPickup = (id: number) =>
  http.post<unknown, null>('/rest/merchant/pointsMall/order/confirmPickup', { id })

/** 手机号尾号（画板「尾号 8601」口径） */
export function phoneTail(phone: string | null | undefined): string {
  const p = String(phone ?? '').replace(/\D/g, '')
  return p.length >= 4 ? p.slice(-4) : ''
}

/** HH:mm（画板「20:31 下单」口径） */
export function hhmm(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : '—'
}
