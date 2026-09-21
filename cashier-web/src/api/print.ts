import { http } from './http'

/**
 * 打印任务与模板（商家侧 /rest/merchant/printTask/**、/printTemplate/**，批次 G · W-25 / W-21）
 *
 * ⚠️ 门店维度由后端用登录会话的 shopId 决定。
 * ⚠️ 「立即重试」只把任务置回「待打印」并写 nextRetryTime；
 *    **真正的下发依赖云打印平台账号（P-01，未就绪）** → 重试后任务停在「待打印」是当前预期行为。
 */

/** 小票类型（与后端 PrintTemplate.SCENE_* 同口径） */
export const TICKET_TYPES = [
  { code: 1, text: '接单小票' },
  { code: 2, text: '出品小票' },
  { code: 3, text: '结账小票' },
  { code: 4, text: '自提小票' }
] as const

/** 任务状态（与后端 PrintTask.STATUS_* 同口径） */
export const TASK_STATUS = [
  { code: 1, text: '待打印' },
  { code: 2, text: '打印中' },
  { code: 3, text: '成功' },
  { code: 4, text: '失败' },
  { code: 5, text: '已忽略' }
] as const

export interface PrintTaskRow {
  id: number
  orderNo: string | null
  ticketType: number | null
  printerId: number | null
  /** 服务端补的打印机名 */
  printerName: string | null
  copies: number | null
  status: number | null
  retryCount: number | null
  maxRetry: number | null
  failReason: string | null
  dispatchTime: string | null
  nextRetryTime: string | null
  printTime: string | null
  isReprint: boolean | null
  operatorId: number | null
  /** 服务端补的操作人姓名 */
  operatorName: string | null
}

export interface PrintTaskQuery {
  status?: number
  orderNo?: string
  /** true = 只取「待打印/失败」（W-25 待重试队列） */
  onlyPendingOrFail?: boolean
  /** true = 只看补打（W-21 补打记录） */
  isReprint?: boolean
  pageNo?: number
  pageSize?: number
}

export const fetchPrintTasks = (q: PrintTaskQuery = {}) =>
  http.post<unknown, { records: PrintTaskRow[]; total: number }>(
    '/rest/merchant/printTask/list',
    { pageNo: 1, pageSize: 50, ...q }
  )

/** 队列指标：待重试 / 今日失败 / 今日成功 */
export const fetchPrintSummary = () =>
  http.post<unknown, { waitRetry: number; todayFail: number; todaySuccess: number }>(
    '/rest/merchant/printTask/summary',
    {}
  )

export const retryPrintTask = (id: number) =>
  http.post<unknown, null>('/rest/merchant/printTask/retry', { id })

export const ignorePrintTask = (id: number) =>
  http.post<unknown, null>('/rest/merchant/printTask/ignore', { id })

export const reprintTask = (id: number) =>
  http.post<unknown, PrintTaskRow>('/rest/merchant/printTask/reprint', { id })

/* ---------------- 模板（W-21） ---------------- */

export interface PrintTemplateRow {
  id: number
  shopId: number | null
  name: string | null
  scene: number | null
  /** 58 / 80（mm） */
  paperWidth: number | null
  footerText: string | null
  copies: number | null
  showQr: boolean | null
  isDefault: boolean | null
}

export const fetchPrintTemplates = (scene?: number) =>
  http.post<unknown, PrintTemplateRow[]>('/rest/merchant/printTemplate/list', scene ? { scene } : {})

export const savePrintTemplate = (t: Partial<PrintTemplateRow>) =>
  http.post<unknown, PrintTemplateRow>('/rest/merchant/printTemplate/save', t)

export const setDefaultPrintTemplate = (id: number) =>
  http.post<unknown, null>('/rest/merchant/printTemplate/setDefault', { id })

export const deletePrintTemplate = (id: number) =>
  http.post<unknown, null>('/rest/merchant/printTemplate/delete', { id })

export function ticketTypeText(code: number | null | undefined): string {
  return TICKET_TYPES.find(t => t.code === code)?.text ?? '—'
}

export function taskStatusText(code: number | null | undefined): string {
  return TASK_STATUS.find(t => t.code === code)?.text ?? '—'
}
