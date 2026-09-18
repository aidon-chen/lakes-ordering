/** 桌台 / 会话 / 账单类型定义（与后端 package_dining 模型对齐） */

/** 桌台状态：1=空闲 2=占用 3=停用（预留态 DDL 保留，一期不做） */
export type TableStatus = 1 | 2 | 3

/** 桌卡 UI 展示态（W-07 四态；M1 由 status + session + 预定记录推导） */
export type TableUiState = 'idle' | 'busy' | 'reserved' | 'cleaning'

/** 桌台（GET /rest/merchant/table/list 行结构，M1 定稿） */
export interface TableInfo {
  id: number
  shopId: number
  tableNo: string
  tableName: string
  typeId: number | null
  capacity: number
  status: TableStatus
  zone: string
  /** 桌台类型名：散台 / 卡座 / 包间 */
  typeName: string
  /** 低消金额（0 = 无低消；一期结账校验不做，仅展示） */
  minSpend: number
  /** 桌卡展示态（默认 idle） */
  ui: TableUiState
  /** 占用时携带进行中会话（聚合下发，桌卡图零二次请求） */
  session: SessionBrief | null
  /** 待清台：结账时间（HH:mm） */
  settledAt: string | null
  /** 预定信息 */
  reservation: ReservationInfo | null
}

/** 预定（M0 仅桌卡展示；改期/取消属管理端 A-09 语义，收银端只读） */
export interface ReservationInfo {
  expectedGuests: number
  /** 预计到店 HH:mm */
  arriveTime: string
}

/** 进行中会话摘要 */
export interface SessionBrief {
  sessionId: number
  orderId: number | null
  memberNum: number
  openTime: string
  openMinutes: number
  goodsTotalQuantity: number
  goodsTotalPrice: number
  /** 新加未下单件数（桌卡角标 / chips 角标） */
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
