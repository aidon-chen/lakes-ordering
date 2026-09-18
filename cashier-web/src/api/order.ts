import http from './http'

/** 点单 / 结账接口（M1 后端交付后启用） */

/** 加菜：向会话当前未结订单追加明细（added=1） */
export const addItems = (sessionId: number, orderId: number, items: unknown) =>
  http.post('/rest/merchant/order/add-items', { sessionId, orderId, items })

/** 数量调整 / 退菜：已下单（added=0）后端拒绝（一期决策） */
export const changeItemQty = (orderId: number, detailId: number, number: number) =>
  http.put('/rest/merchant/order/item-qty', { orderId, detailId, number })

/** 加菜下单：当前订单 added=1 明细落为已下单（送厨打） */
export const sendItems = (orderId: number) =>
  http.post('/rest/merchant/order/send-items', { orderId })

/** 会话结账入口 → M-02 收银支付；支付回调自动清台 */
export const checkout = (sessionId: number) =>
  http.post('/rest/merchant/order/checkout', { sessionId })
