import { http } from './http'

/** 台位作业接口（对齐后端 /rest/merchant/diningTable/**，2026-09-19） */

/** 桌台图数据：{ tables: TableFloorRow[], zoneList: ZoneRow[] } */
export interface TableFloorRow {
  id: number
  tableNo: string
  tableName: string
  typeName: string
  zone: string
  capacity: number
  status: 1 | 2 | 3
  minSpend: number
  // 会话摘要（进行中 或 最近一次已结台，后者用于派生待清台）
  sessionId: number | null
  memberNum: number | null
  openMinutes: number | null
  goodsTotalPrice: number | null
  addedCount: number | null
  /** 会话状态 1=进行中 2=已结台；无会话为 null */
  sessionStatus: number | null
  /** 结台时间（已结台时有值） */
  closeTime: string | null
  /** 待清台：台位仍占用且最近会话已结台（后端算好下发） */
  awaitingClean: boolean | null
}

/** 区域行 */
export interface ZoneRow {
  id: number
  shopId: number
  name: string
  description?: string
  sortNumber: number
}

/** 桌台总览：全量台位 + 区域列表（不分页） */
export const fetchTableList = () =>
  http.post<unknown, { tables: TableFloorRow[]; zoneList: ZoneRow[] }>(
    '/rest/merchant/diningTable/list',
    {}
  )

/** 开台：返回 { sessionId, tableId } */
export const openTable = (id: number, memberNum: number) =>
  http.post<unknown, { sessionId: number; tableId: number }>(
    '/rest/merchant/diningTable/open',
    { id, memberNum }
  )

/** 转台 */
export const changeTable = (id: number, targetTableId: number) =>
  http.post<unknown, null>('/rest/merchant/diningTable/change', { id, targetTableId })

/**
 * 结账（批次 C 2026-09-19 新增）
 * 只闭台、**不放台** → 台位进入「待清台」，再由 clearTable 真正释放。
 * 对齐 W-04 画板底注「收款后：订单→已完成 · 台位→待清台」。
 */
export const settleTable = (id: number) =>
  http.post<unknown, null>('/rest/merchant/diningTable/settle', { id })

/**
 * 清台
 * 兼容「待清台」：会话已结台时本次仅释放台位
 */
export const clearTable = (id: number) =>
  http.post<unknown, null>('/rest/merchant/diningTable/clear', { id })

/**
 * 确认收款（2026-09-24 新增）
 *
 * 口径＝「点单即收款」（先付后吃，用户 2026-09-24 拍板）：与下单同一动作完成，
 * 后端置 is_payment=1 + payment_mode + status 1→2（待处理=下厨）。
 * **台位保持不变**（顾客仍在用餐），离店点「清台」一步完成「结台 + 放台」。
 *
 * 定位方式二选一：堂食传 tableId（收该桌全部未付订单，含漏收与后续加酒）；外带传 orderNo。
 * payMode：1=微信收款码，4=到店支付(现金) —— 对齐 A-07 营收看板的 payment_mode 口径。
 * 返回 paidCount=0 表示已收过（幂等，前端提示「无需重复收款」）。
 */
export const confirmPayment = (param: {
  tableId?: number | null
  orderNo?: string | null
  payMode: number
}) =>
  http.post<unknown, { paidCount: number; paidAmount: number; payMode: number }>(
    '/rest/merchant/diningTable/confirmPayment',
    param
  )

/**
 * 扫码收款（付款码支付 · 被扫，2026-09-24 新增）
 *
 * 收银员用扫码枪/摄像头扫顾客手机上的微信付款码 → 后端调微信 /pay/micropay
 * **真扣款**，成功即落库（is_payment=1 + payment_mode=1 + status 1→2）。
 *
 * 与 confirmPayment 的分工对应画板结算栏两档：微信收款码 → 本接口（真扣款）；
 * 现金 → confirmPayment（人工确认记账）。
 *
 * 结果三态（看 data.status）：
 *  - `SUCCESS`    已扣款，data.paidAmount / transactionId 有值
 *  - `USERPAYING` 顾客仍在输密码，**未收款**，提示收银员稍后重扫（微信对同单号同参数幂等，重扫安全）
 *  - 失败         抛异常，message 为微信原文（「付款码已过期」「余额不足」等）
 *
 * ⚠️ authCode 是支付凭据，不要打印到控制台或上报。
 */
export const scanPay = (param: {
  tableId?: number | null
  orderNo?: string | null
  authCode: string
}) =>
  http.post<
    unknown,
    {
      status: 'SUCCESS' | 'USERPAYING'
      outTradeNo: string
      paidAmount?: number
      transactionId?: string
      message?: string
    }
  >('/rest/merchant/diningTable/scanPay', param)
