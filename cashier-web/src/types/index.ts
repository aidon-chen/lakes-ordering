/** 桌台 / 会话 / 账单类型定义（与后端 package_dining 模型对齐） */

/** 桌台状态：1=空闲 2=占用 3=停用（待清台由 awaitingClean 派生，见 TableInfo） */
export type TableStatus = 1 | 2 | 3

/** 桌卡 UI 展示态（批次 C 扩为三态：idle/busy/toclean） */
export type TableUiState = 'idle' | 'busy' | 'toclean'

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
  /**
   * 待清台：台位仍占用但会话已结台（后端算好下发，批次 C 2026-09-19）
   * 由 settle（结账）产生，等 clear（清台）消除
   */
  awaitingClean: boolean
  /** 桌卡展示态（由 awaitingClean + status + session 推导） */
  ui: TableUiState
  /** 占用/待清台时携带会话（聚合下发，桌卡图零二次请求） */
  session: SessionBrief | null
}

/** 会话摘要 */
export interface SessionBrief {
  sessionId: number
  memberNum: number
  openMinutes: number
  goodsTotalPrice: number
  addedCount: number
  /** 会话状态 1=进行中 2=已结台（3=已失效不下发） */
  sessionStatus: number
  /** 结台时间（sessionStatus=2 时有值），用于待清台卡显示「已结账 HH:mm」 */
  closeTime: string | null
}

/** 账单明细行（对齐 W-04 结算栏行：品名（规格）／规格 ×n · 备注／小计） */
export interface CartLine {
  goodsId: number
  goodsName: string
  /** 可读规格串，如「加冰」（用于行内展示与后端 specList） */
  specText: string
  /** 规格名 → 选中项集合（支持「配搭（可多选）」） */
  specs: Record<string, string[]>
  /** 单品备注 */
  remark: string
  price: number
  qty: number
  /** true=新加未下单（金标「加」）；false=已下单（一期禁止调量退菜） */
  added: boolean
}

/** 分类横条项（W-04：分类名 + 该分类商品数，「全部 N」为默认项） */
export interface MenuChip {
  /** null = 全部 */
  id: number | null
  name: string
  count: number
}

/** 收银台三视图（Odoo Screen 状态机映射） */
export type PosView = 'floor' | 'order' | 'ticket'
