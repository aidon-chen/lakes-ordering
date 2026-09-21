import { http } from './http'

/**
 * 存酒临期提醒（W-19）
 * 通知方式（用户拍板）= **小程序端弹窗提示**，不是短信：
 *   顾客打开小程序「我的存酒」→ 客户端检查临期/过期 → 弹窗 → 调 /remindAck 记录
 *   → 本页「通知状态」列显示「已弹窗 MM-dd」，未弹过则「待提示」。
 * ⚠️ 收银端**无法主动触发**顾客端弹窗（弹窗发生在顾客自己的手机上），
 *    故本页操作列**不放「立即通知」**（不放假按钮）。
 */

export interface WineRemindRow {
  storageId: number
  /** 记录号（=存酒记录 id）。⚠️ 方案 D 已废除「存酒码」，画板的存酒码列改用记录号 */
  recordNo: number | null
  customerName: string | null
  /** 已脱敏（138****8601） */
  customerPhone: string | null
  goodsName: string | null
  remainCount: number | null
  /** yyyy-MM-dd */
  expireDate: string | null
  /** >0 未到期；0=当天到期；<0 已过期 N 天 */
  daysRemaining: number
  expired: boolean
  lastNotifyTime: string | null
  notified: boolean
}

export const fetchWineRemindList = () =>
  http.post<unknown, WineRemindRow[]>('/rest/merchant/wineStorage/remind/list', {})

export const fetchWineRemindSummary = () =>
  http.post<unknown, { expiring: number; expired: number; notified: number }>(
    '/rest/merchant/wineStorage/remind/summary',
    {}
  )

export const fetchWineRemindSetting = () =>
  http.post<unknown, { wineRemindAheadDays: number | string | null }>(
    '/rest/merchant/wineStorage/remind/setting/list',
    {}
  )

export const saveWineRemindSetting = (wineRemindAheadDays: number) =>
  http.post<unknown, { wineRemindAheadDays: string }>(
    '/rest/merchant/wineStorage/remind/setting/save',
    { wineRemindAheadDays }
  )

/** MM-dd（画板「已发短信 09-13」→ 我们显示「已弹窗 09-21」） */
export function mmdd(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{1,2})-(\d{1,2})/)
  if (!m) return '—'
  return m[1].padStart(2, '0') + '-' + m[2].padStart(2, '0')
}
