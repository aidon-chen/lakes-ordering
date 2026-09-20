import { http } from './http'

/** 存酒接口（商家侧 /rest/merchant/wineStorage/**，批次 F） */

/**
 * 存酒列表行（对齐后端 WineStorageVo；**商家侧已填充 memberName/memberPhone**，手机号已脱敏）
 * 状态码由服务端计算下发：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期（临期阈值 30 天）
 */
export interface WineStorageRow {
  id: number
  goodsName: string | null
  bottleCount: number | null
  remainCount: number | null
  storageCode: string
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
}

export interface WineStorageQuery {
  /** 存酒码（后端 like 模糊） */
  storageCode?: string
  /** 顾客手机号（后端精确定位会员） */
  memberPhone?: string
}

/**
 * 存酒列表（W-18）
 * ⚠️ 后端仅支持 storageCode / memberPhone 两个筛选（已核实）；状态筛选在页面内本地做。
 * 返回的是**数组**（非分页对象），与顾客侧 C-16 一致。
 */
export const fetchWineStorageList = (q: WineStorageQuery = {}) =>
  http.post<unknown, WineStorageRow[]>('/rest/merchant/wineStorage/list', q)

/**
 * 取酒核销（W-18）
 * 后端为原子扣减（防超扣），扣到 0 自动置「已取完」并写取酒记录；扣超会抛业务异常。
 */
export const takeWine = (storageCode: string, takeCount: number) =>
  http.post<unknown, null>('/rest/merchant/wineStorage/take', { storageCode, takeCount })

/**
 * 存酒登记（W-17）
 * 手机号定位会员 → 生成唯一存酒码 → 有效期默认 90 天（expireDays 可覆盖）
 */
export const createWine = (p: {
  memberPhone: string
  goodsName: string
  bottleCount: number
  location?: string
  remark?: string
  expireDays?: number
}) => http.post<unknown, null>('/rest/merchant/wineStorage/create', p)
