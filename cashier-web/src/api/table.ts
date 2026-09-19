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
