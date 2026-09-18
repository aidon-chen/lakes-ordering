/** 桌台 / 会话 / 账单类型定义（与后端 package_dining 模型对齐） */

/** 桌台状态：1=空闲 2=占用 3=停用（Round 8 回归三态，待清台/预定后续扩展） */
export type TableStatus = 1 | 2 | 3

/** 桌卡 UI 展示态（Round 8：仅两态 idle/busy） */
export type TableUiState = 'idle' | 'busy'

/** 桌台（对齐后端 TableFloorVo，list 接口行结构） */
export interface TableInfo {
  id: number
  shopId: number
  tableNo: string
  tableName: string
  typeId: number | null
  capacity: number
  status: TableStatus
  /** 区域名（A区散台/K区卡座/V区包间，后端 join 区域表） */
  zone: string
  /** 桌台类型名：散台 / 卡座 / 包间 */
  typeName: string
  /** 低消金额（0 = 无低消；一期仅展示不校验） */
  minSpend: number
  /** 桌卡展示态（由 status+session 推导） */
  ui: TableUiState
  /** 占用时携带进行中会话（聚合下发，桌卡图零二次请求） */
  session: SessionBrief | null
}

/** 进行中会话摘要 */
export interface SessionBrief {
  sessionId: number
  memberNum: number
  openMinutes: number
  goodsTotalPrice: number
  addedCount: number
}

/** 账单明细行 */
export interface CartLine {
  goodsId: number
  goodsName: string
  specText: string
  price: number
  qty: number
  /** true=新加未下单（金标「加」）；false=已下单（一期禁止调量退菜） */
  added: boolean
}

/** 收银台三视图（Odoo Screen 状态机映射） */
export type PosView = 'floor' | 'order' | 'ticket'
