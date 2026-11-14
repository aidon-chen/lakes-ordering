import { http } from './http'

/**
 * 订单改单 / 退单（W-13）· /rest/merchant/orderChange/**
 *
 * ⚠️ 退款执行与**收款同构**：微信支付路径最终调 `wxPayService.refund()`，
 *    而该方法在**非 prod profile 下直接返回 true（模拟成功）**——与收款走 MockPay 是同一套演示口径。
 *    因此当前线上（mvp）退单会「成功退款」但**没有真实资金动作**；商户号/证书齐备后才动真格。
 * ⚠️ 改单**不影响已支付金额**（画板口径）：差额只记录为「待补收 / 待退」，不触发真实收退款。
 */

export interface ChangeItem {
  /** 已有明细 id；为空表示新增 */
  detailId?: number | null
  goodsId?: number | null
  goodsName?: string | null
  /**
   * 商品主图。tb_order_detail.main_image 是 NOT NULL 且无默认值，
   * 新增明细时必须带上（否则 MyBatis 动态 SQL 会跳过该列 → 插入报
   * 「Field 'main_image' doesn't have a default value」）。
   */
  mainImage?: string | null
  /** 商品规格 JSON，可空 */
  specList?: string | null
  price?: number | null
  number: number
}

export interface ModifyResult {
  beforeAmount: number
  afterAmount: number
  /** 正 = 需补收，负 = 应退 */
  diffAmount: number
}

export interface RefundResult {
  refundAmount: number
  refundSuccess: boolean
  refundId: number
  orderStatus: number
}

export interface ChangeRecordRow {
  id: number
  orderId: number
  orderNo: string | null
  /** 1=改单 2=退单 */
  changeType: number
  beforeAmount: number | null
  afterAmount: number | null
  diffAmount: number | null
  refundAmount: number | null
  /** 1=全额 2=部分 */
  refundWay: number | null
  reasonCode: number | null
  reasonText: string | null
  detailSnapshot: string | null
  operatorId: number | null
  createTime: string | null
}

/** 改单：改数量 / 加新品 / 删明细（number<=0 即删除该明细） */
export const modifyOrder = (orderId: number, items: ChangeItem[]) =>
  http.post<unknown, ModifyResult>('/rest/merchant/orderChange/modify', { orderId, items })

/** 退单：refundWay 1=整单退 2=部分退（部分退需带 items） */
export const refundOrder = (
  orderId: number,
  refundWay: 1 | 2,
  reasonCode: number,
  reasonText: string,
  items: ChangeItem[] = []
) =>
  http.post<unknown, RefundResult>('/rest/merchant/orderChange/refund', {
    orderId,
    refundWay,
    reasonCode,
    reasonText,
    items
  })

export const fetchChangeRecords = (q: { changeType?: number; orderNo?: string } = {}) =>
  http.post<unknown, ChangeRecordRow[]>('/rest/merchant/orderChange/record/list', q)

/**
 * 退单原因（取自 tb_order_refund.refund_reason 的既有枚举，只挑堂食酒馆场景常用的）
 * —— 不新增枚举，避免与后端已有 20 项口径分裂。
 */
export const REFUND_REASONS = [
  { code: 4, text: '商家缺货' },
  { code: 3, text: '买错了/买少了' },
  { code: 5, text: '商家联系我取消' },
  { code: 8, text: '顾客不想要了' },
  { code: 9, text: '商家通知我卖完了' },
  { code: 14, text: '商家少送商品' },
  { code: 15, text: '商家送错商品' },
  { code: 16, text: '口味不佳/个人感受不好' },
  { code: 20, text: '其他' }
] as const

export function refundReasonText(code: number | null | undefined): string {
  return REFUND_REASONS.find(r => r.code === code)?.text ?? (code == null ? '—' : '其他')
}
