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
