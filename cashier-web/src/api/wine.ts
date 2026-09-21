import { http } from './http'

/** 存酒 / 取酒申请接口（商家侧 /rest/merchant/wineStorage/**，方案 v1.1） */

/**
 * 存酒列表行（对齐后端 WineStorageVo；**商家侧已填充 memberName/memberPhone**，手机号已脱敏）
 * 状态码由服务端计算下发：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期（临期阈值 30 天）
 */
export interface WineStorageRow {
  id: number
  goodsName: string | null
  bottleCount: number | null
  remainCount: number | null
  location: string | null
  shopName: string | null
  /** 商家侧专用：会员姓名 */
  memberName: string | null
  /** 商家侧专用：会员手机号（**已脱敏** 151****0000） */
  memberPhone: string | null
  memberId: number | null
  statusCode: number | null
  statusText: string | null
  storageDate: string | null
  expireDate: string | null
  /** 该存酒当前是否有「待核销」取酒申请（有则列表内「代发起取酒」应置灰） */
  pendingApply?: boolean
}

export interface WineStorageQuery {
  /** 顾客手机号（后端精确定位会员） */
  memberPhone?: string
}

/**
 * 存酒列表（W-18）
 * ⚠️ 后端仅支持 memberPhone 筛选（已核实）；状态筛选在页面内本地做。
 * 返回的是**数组**（非分页对象），与顾客侧 C-16 一致。
 */
export const fetchWineStorageList = (q: WineStorageQuery = {}) =>
  http.post<unknown, WineStorageRow[]>('/rest/merchant/wineStorage/list', q)

/**
 * 取酒申请行（对齐后端 WineTakeApplyVo）
 * 状态码：1 待核销 / 2 已核销 / 3 已取消 / 4 已过期
 * 来源：1 顾客自助 / 2 商家代发起
 */
export interface WineTakeApplyRow {
  id: number
  storageId: number
  memberId: number | null
  goodsName: string | null
  location: string | null
  takeCount: number | null
  /** 一次性取酒码（仅「待核销」下发） */
  takeCode: string | null
  statusCode: number | null
  statusText: string | null
  applySource: number | null
  applySourceText: string | null
  applyTime: string | null
  expireTime: string | null
  confirmTime: string | null
  memberName: string | null
  memberPhone: string | null
}

/** 待核销取酒申请列表（收银端弹层待办） */
export const fetchApplyList = () =>
  http.post<unknown, WineTakeApplyRow[]>('/rest/merchant/wineStorage/apply/list', {})

/** 按取酒码预览待核销详情（只读；两段式核销第一步） */
export const previewTake = (takeCode: string) =>
  http.post<unknown, WineTakeApplyRow>('/rest/merchant/wineStorage/apply/preview', { takeCode })

/** 取酒核销（原子扣减，防超扣；扣到 0 自动置「已取完」并写取酒记录） */
export const confirmTake = (takeCode: string) =>
  http.post<unknown, WineTakeApplyRow>('/rest/merchant/wineStorage/apply/confirm', { takeCode })

/** 代发起取酒（商家兜底通道）→ 返回新申请（含取酒码） */
export const createApplyByShop = (p: { storageId: number; takeCount: number; remark?: string }) =>
  http.post<unknown, WineTakeApplyRow>('/rest/merchant/wineStorage/apply/create', p)

/**
 * 手机号查会员（W-17「查询」按钮）
 * 后端按 mobile 精确查；查不到抛业务异常「未找到该手机号对应的会员」。
 * 只返回**脱敏**手机号（收银端当面核对够用，不外泄完整号码）。
 */
export interface MerchantMemberBrief {
  id: number
  nickname: string | null
  name: string | null
  /** 已脱敏，如 138****8601 */
  mobile: string | null
}

export const lookupMemberByMobile = (mobile: string) =>
  http.post<unknown, MerchantMemberBrief>('/rest/merchant/member/selectByMobile', { mobile })

/**
 * 存酒登记（W-17）
 * 手机号定位会员 → storage_date=今天，expire_date=今天+expireDays（默认 90）
 * 注：方案 v1.1 起登记阶段不再生成存酒码，取酒码在「申请/代发起」时生成。
 * @returns 创建结果 { id, expireDate }
 */
export const createWine = (p: {
  memberPhone: string
  goodsName: string
  bottleCount: number
  location?: string
  remark?: string
  expireDays?: number
}) =>
  http.post<unknown, { id: number; expireDate: string }>('/rest/merchant/wineStorage/create', p)
