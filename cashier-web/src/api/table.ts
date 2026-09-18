import http from './http'
import type { TableInfo } from '@/types'

/** 桌台 / 会话接口（M1 后端交付后启用） */

/** 桌台总览：一次带回桌位+分区+进行中会话聚合 */
export const fetchTableList = (shopId: number) =>
  http.get<unknown, TableInfo[]>('/rest/merchant/table/list', { params: { shopId } })

/** 开台：乐观占位 + 建会话 + 首单；同桌已有进行中会话时返回既有（复用） */
export const openTable = (tableId: number, guests: number) =>
  http.post<unknown, { sessionId: number; orderId: number; reused: boolean }>(
    '/rest/merchant/table/open',
    { tableId, guests }
  )

/** 进行中会话列表（chips 行 / TicketView 共用数据源） */
export const fetchPendingOrders = (shopId: number) =>
  http.get<unknown, unknown[]>('/rest/merchant/order/pending-list', { params: { shopId } })
