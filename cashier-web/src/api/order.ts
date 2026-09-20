import { http } from './http'

/** 点单 / 下单接口（方案 A：复用 POST /rest/merchant/order/insert 一次性提交） */

/** 下单明细（对齐后端 OrderDetail：goodsId/goodsName/specList/number/price/subtotal） */
export interface OrderDetailSubmit {
  goodsId: number
  goodsName: string
  /** 规格 JSON 字符串（如 {"规格":"单杯"}），无规格传空对象字符串 */
  specList: string
  number: number
  price: number
  subtotal: number
}

/** 收银台下单参数（对齐 OrderParam + 台位作业字段） */
export interface OrderSubmitParam {
  orderDetailListStr: string // 明细 JSON 数组字符串
  actualPrice: number
  tableId: number
  tableNo: string
  tableName: string
  diningNum: number
  diningSessionId: number
  orderScene: 1 | 2 // 1=堂食
}

/** 下单：一次性提交购物车（生成一张订单，挂到当前会话） */
export const submitOrder = (param: OrderSubmitParam) =>
  http.post<unknown, { id: number; orderNo: string }>('/rest/merchant/order/insert', param)

/* ==================== W-10 今日订单 ==================== */

/**
 * 订单行（/rest/merchant/order/list 真实返回字段的子集，已 curl 实测。
 * ⚠️ 不要把没验证过的字段写进来 —— 该接口返回 60+ 字段，多数是配送/商城遗留的 null）
 */
export interface MerchantOrderRow {
  id: number
  orderNo: string
  /** 订单状态：1未付款 2待处理 3待自取 4待配送 5已配送 6已完成
   *  7售后处理中 8已退款(废弃) 9售后处理完成 10已取消(未支付) 11已取消(已支付)
   *  来源：MerchantOrderController 的 ApiImplicitParam 注释（权威口径） */
  status: number
  /** 取餐号（堂食单有值；历史种子单为 null） */
  queueNo: number | null
  tableNo: string | null
  tableName: string | null
  diningSessionId: number | null
  /** 订单描述（业务填的文本，如「批次5联调种子单（可开票）」） */
  description: string | null
  goodsTotalQuantity: number | null
  goodsTotalPrice: number | null
  actualPrice: number | null
  /** 支付方式：1微信 2余额 3积分(已取消) 4到店；null=未知
   *  文本口径对齐 uniapp-siam-user/utils/system-status.js 的 paymentModeText */
  paymentMode: number | null
  isPayment: boolean | null
  /** 1=自取（本店堂食与自提均为 1，见 insertByMerchant 强制口径） */
  shoppingWay: number | null
  /** 1=堂食 2=商城 */
  orderScene: number | null
  contactPhone: string | null
  createTime: string | null
  /** 出品状态 1=待制作 2=出品中 3=已出品（批次 D 新增列，服务 M-04 出品进度跟踪） */
  productionStatus: number | null
}

export interface OrderQueryParam {
  pageNo: number
  pageSize: number
  /** 后端支持的服务端筛选（已核 ApiImplicitParam）：shoppingWay / contactPhone / status / orderNo / shopId */
  shoppingWay?: number
  contactPhone?: string
  status?: number
}

/**
 * 订单列表（W-10）
 * ⚠️ 后端 **不支持** queueNo 与日期区间筛选（已核参数清单）→ 这两项在页面内对已加载集合做客户端筛选，
 *    故 pageSize 取较大值。订单量增长后应给后端补 queueNo/日期区间参数。
 */
export const fetchMerchantOrders = (param: OrderQueryParam) =>
  http.post<unknown, { records: MerchantOrderRow[]; total: number }>(
    '/rest/merchant/order/list',
    param
  )

/** 订单状态文案（权威口径，见 MerchantOrderRow.status 注释） */
export function orderStatusText(status: number): string {
  switch (status) {
    case 1: return '未付款'
    case 2: return '待处理'
    case 3: return '待自取'
    case 4: return '待配送'
    case 5: return '已配送'
    case 6: return '已完成'
    case 7: return '售后处理中'
    case 8: return '已退款'
    case 9: return '售后处理完成'
    case 10: return '已取消'
    case 11: return '已取消'
    default: return '—'
  }
}

/** 支付方式文案（复用客户端既有口径 paymentModeText，不另造一套） */
export function paymentModeText(mode: number | null | undefined): string {
  switch (mode) {
    case 1: return '微信支付'
    case 2: return '余额支付'
    case 3: return '积分支付'
    case 4: return '到店支付'
    default: return '—'
  }
}

/* ==================== W-11 / W-14 订单作业 ==================== */

/**
 * 订单状态流转
 * ⚠️⚠️ 实测踩坑：动作码的**参数名是 `flag`，不是 `status`**！
 *   读 `MerchantOrderController.updateStatus` 实现：`switch (param.getFlag())`。
 *   而该方法的 @ApiImplicitParam 把 `status` 注释成了状态枚举 —— **注释是误导**：
 *   传 `{status:1}` 时 `getFlag()` 为 null → switch 落 default → **静默什么都不做**（不报错！）。
 *
 *   动作码（flag）：
 *     1 = 处理/接单    要求订单 status=2；堂食/自取(shoppingWay=1) → 3
 *     2 = 完成(自取)   要求 status=3 → 6
 *     3 = 标记配送     要求 status=4 → 5
 *     4 = 完成(配送)   要求 status=5 → 6
 *     5 = 收银台结算   **已注释停用**
 *   前置状态不匹配会抛业务异常（如「该订单状态非待处理，不允许修改」）
 */
export type OrderStatusAction = 1 | 2 | 3 | 4

export const updateOrderStatus = (id: number, flag: OrderStatusAction) =>
  http.post<unknown, null>('/rest/merchant/order/updateStatus', { id, flag })

/**
 * 出品状态流转（批次 D 新增接口）
 * 1=待制作 2=出品中 3=已出品；仅改该字段（MP 非空更新，不动订单其它资料）
 */
export const updateProductionStatus = (id: number, productionStatus: 1 | 2 | 3) =>
  http.post<unknown, null>('/rest/merchant/order/updateProductionStatus', { id, productionStatus })

/** 出品状态文案 */
export function productionStatusText(v: number | null | undefined): string {
  switch (v) {
    case 1: return '待制作'
    case 2: return '出品中'
    case 3: return '已出品'
    default: return '—'
  }
}
