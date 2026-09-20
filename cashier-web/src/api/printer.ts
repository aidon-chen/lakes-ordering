import { http } from './http'

/**
 * 打印机接口（商家侧 /rest/merchant/printer/**，批次 G / W-20）
 * ⚠️ 实测的 HTTP 方法**不统一**（原样遵守，别想当然）：
 *    POST /list · POST /insert · **PUT /update** · **DELETE /delete**
 * ⚠️ /update 与 /delete 带 @MerchantPermission，权限不足会返回业务异常。
 * ⚠️ 该 Controller 上的 @ApiImplicitParam 注释是从商品模块复制来的（写「商品表主键id／商品名称」）
 *    —— **注释是误导**，真实字段以 Printer 实体（tb_printer）为准。
 */

/** 打印机（对齐 tb_printer / Printer 实体） */
export interface PrinterRow {
  id: number
  shopId: number | null
  name: string
  /** 云打印平台设备编号 */
  number: string | null
  /** 云打印平台密钥/识别码（**列表页展示时脱敏**） */
  identifyingCode: string | null
  isAutoPrint: boolean | null
  mobileCardNumber: string | null
  /** (预留) 云端注册状态 —— **不是运行态**，不能当「在线/缺纸」用 */
  cloudRegistrationStatus: string | null
  /** 1=小票打印机 2=标签打印机 */
  type: number | null
  /** 品牌 1=飞鹅打印机 2=易联云打印机 */
  brand: number | null
  createTime: string | null
  updateTime: string | null
}

export interface PrinterQuery {
  name?: string
  /** 设备编号（后端 Printer 实体支持，走 like） */
  number?: string
  pageNo?: number
  pageSize?: number
}

/** 打印机列表（返回分页对象；shopId 由后端会话注入） */
export const fetchPrinterList = (q: PrinterQuery = {}) =>
  http.post<unknown, { records: PrinterRow[]; total: number }>('/rest/merchant/printer/list', {
    pageNo: q.pageNo ?? -1,
    pageSize: q.pageSize ?? 100,
    ...(q.name ? { name: q.name } : {}),
    ...(q.number ? { number: q.number } : {})
  })

export type PrinterSaveParam = Partial<Pick<PrinterRow, 'id' | 'name' | 'number' | 'identifyingCode' | 'isAutoPrint' | 'type' | 'brand'>>

/** 新增打印机（shopId 由后端注入） */
export const insertPrinter = (p: PrinterSaveParam) =>
  http.post<unknown, null>('/rest/merchant/printer/insert', p)

/** 修改打印机（注意是 **PUT**） */
export const updatePrinter = (p: PrinterSaveParam) =>
  http.put<unknown, null>('/rest/merchant/printer/update', p)

/**
 * 删除打印机（**DELETE** + @RequestBody）
 *
 * ⚠️⚠️ 实测踩坑（两处，都很隐蔽）：
 *  ①  的类型是 **List<Integer>（数组）**，不是逗号分隔字符串 ——
 *     虽然该接口的 @ApiImplicitParam 写着「批量删除时id以逗号分隔」（**又一处误导注释**），
 *     但实体字段是 List → 传 `{"ids":"5"}` 会让 Jackson 反序列化失败 → **500「系统异常」**；
 *     传 `{"ids":[5]}` 才成功（线上对照实测：字符串 500 / 数组「删除成功」）。
 *  ② axios 的 delete 第二参是 **config**，body 必须写成 `{ data: {...} }`。
 */
export const deletePrinters = (ids: number[]) =>
  http.delete<unknown, null>('/rest/merchant/printer/delete', { data: { ids } })

/** 品牌文案（工程枚举，**不是**画板样例里的「芯烨」） */
export function printerBrandText(brand: number | null | undefined): string {
  switch (brand) {
    case 1: return '飞鹅打印机'
    case 2: return '易联云打印机'
    default: return '—'
  }
}

/** 类型文案 */
export function printerTypeText(type: number | null | undefined): string {
  switch (type) {
    case 1: return '小票打印机'
    case 2: return '标签打印机'
    default: return '—'
  }
}
