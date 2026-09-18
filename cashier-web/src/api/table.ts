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
  // 占用时的进行中会话摘要
  sessionId: number | null
  memberNum: number | null
  openMinutes: number | null
  goodsTotalPrice: number | null
  addedCount: number | null
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

/** 清台 */
export const clearTable = (id: number) =>
  http.post<unknown, null>('/rest/merchant/diningTable/clear', { id })
