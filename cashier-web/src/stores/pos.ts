import { defineStore } from 'pinia'
import router from '@/router'
import type { CartLine, PosView, TableInfo, TableUiState } from '@/types'

/**
 * 收银台全局状态（Odoo PosStore / Screen 状态机映射）
 * - view: floor（桌台总览）/ order（点单管理）/ ticket（未结订单）
 * - tables: table/list 全量下发，唯一事实源
 * - cart: 当前会话账单本地态；added=true 即原型「新加未下单」金标
 * 一期口径：已下单（added=false）禁止调量退菜；低消不做
 */
export const usePosStore = defineStore('pos', {
  state: () => ({
    view: 'floor' as PosView,
    zone: '全部区域',
    statusFilter: '全部状态',
    keyword: '',
    shopId: 13, // 登录态注入后覆盖（商家 siam / shopId=13）
    tables: [] as TableInfo[],
    currentTableId: null as number | null,
    currentSessionId: null as number | null,
    cart: [] as CartLine[],
    // 开台弹窗中间态
    openForTableId: null as number | null,
    guestPick: 2
  }),

  getters: {
    currentTable(s): TableInfo | null {
      return s.tables.find(t => t.id === s.currentTableId) ?? null
    },
    /** W-07 工具条筛选：区域 + 状态 + 台号关键词 */
    filteredTables(s): TableInfo[] {
      const kw = this.keyword.trim()
      return s.tables.filter(t => {
        if (s.zone !== '全部区域' && t.zone !== s.zone) return false
        if (s.statusFilter !== '全部状态' && uiOf(t) !== statusLabelOf(s.statusFilter)) return false
        if (kw && !t.tableNo.toLowerCase().includes(kw.toLowerCase())) return false
        return true
      })
    },
    /** 图例计数：空闲 / 占用 / 预定 / 待清台 */
    uiCounts(s): Record<TableUiState, number> {
      const c: Record<TableUiState, number> = { idle: 0, busy: 0, reserved: 0, cleaning: 0 }
      for (const t of s.tables) c[uiOf(t)]++
      return c
    },
    busyTables(s): TableInfo[] {
      return s.tables.filter(t => uiOf(t) === 'busy')
    },
    diningCount(s): number {
      return s.tables.reduce((n, t) => n + (t.session?.memberNum ?? 0), 0)
    },
    cartTotal(s): number {
      return s.cart.reduce((sum, l) => sum + l.price * l.qty, 0)
    },
    addedCount(s): number {
      return s.cart.filter(l => l.added).reduce((n, l) => n + l.qty, 0)
    }
  },

  actions: {
    setView(v: PosView) {
      this.view = v
      // 视图状态与 vue-router 单一事实源：跳转即导航（M0 修复：此前仅改 store 导致 URL 脱节）
      if (router.currentRoute.value.path !== `/${v}`) void router.push(`/${v}`)
    },

    /** M0: mock 数据（对齐 W-07 画板）；M1 替换为 GET /rest/merchant/table/list */
    loadTables() {
      if (this.tables.length) return
      const mk = (
        id: number,
        tableNo: string,
        zone: string,
        typeName: string,
        capacity: number,
        ui: TableUiState,
        opts: Partial<TableInfo> = {}
      ): TableInfo => ({
        id,
        shopId: this.shopId,
        tableNo,
        tableName: `${zone}·${tableNo}`,
        typeId: null,
        capacity,
        status: ui === 'busy' ? 2 : 1,
        zone,
        typeName,
        minSpend: 0,
        ui,
        session: null,
        settledAt: null,
        reservation: null,
        ...opts
      })
      const sess = (memberNum: number, openMinutes: number, total: number): TableInfo['session'] => ({
        sessionId: 9100 + memberNum * 10 + openMinutes,
        orderId: 55000 + memberNum * 10 + openMinutes,
        memberNum,
        openTime: '',
        openMinutes,
        goodsTotalQuantity: 0,
        goodsTotalPrice: total,
        addedCount: 0
      })
      this.tables = [
        mk(1, 'A01', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(2, 'A02', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(3, 'A03', 'A区散台', '散台', 4, 'busy', { minSpend: 300, session: sess(4, 134, 452) }),
        mk(4, 'A04', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(5, 'A05', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(6, 'A06', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(7, 'A07', 'A区散台', '散台', 4, 'cleaning', { settledAt: '21:02' }),
        mk(8, 'A08', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(9, 'A09', 'A区散台', '散台', 4, 'cleaning', { settledAt: '21:15' }),
        mk(10, 'A10', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(11, 'A11', 'A区散台', '散台', 4, 'busy', { minSpend: 0, session: sess(2, 41, 96) }),
        mk(12, 'A12', 'A区散台', '散台', 4, 'idle', { minSpend: 0 }),
        mk(13, 'K01', 'K区卡座', '卡座', 6, 'idle', { minSpend: 300 }),
        mk(14, 'K02', 'K区卡座', '卡座', 6, 'busy', { minSpend: 300, session: sess(5, 92, 680) }),
        mk(15, 'K03', 'K区卡座', '卡座', 6, 'idle', { minSpend: 300 }),
        mk(16, 'K04', 'K区卡座', '卡座', 6, 'reserved', {
          minSpend: 300,
          reservation: { expectedGuests: 8, arriveTime: '20:00' }
        }),
        mk(17, 'V01', 'V区包间', '包间', 10, 'busy', { minSpend: 800, session: sess(3, 70, 1240) }),
        mk(18, 'V02', 'V区包间', '包间', 10, 'idle', { minSpend: 800 })
      ]
    },

    /** 桌卡点击路由（W-07 四态）：占用→恢复进单；空闲→开台弹窗；待清台→清台；预定→到店开台 */
    clickTable(table: TableInfo) {
      const ui = uiOf(table)
      if (ui === 'busy') {
        this.resumeOrder(table.id)
      } else if (ui === 'idle') {
        this.openForTableId = table.id
        this.guestPick = Math.max(1, Math.round(table.capacity / 2))
      } else if (ui === 'cleaning') {
        this.cleanTable(table.id)
      } else if (ui === 'reserved') {
        this.openForTableId = table.id // 预定到店：直接按开台流走（改期/取消在管理端）
        this.guestPick = table.reservation?.expectedGuests ?? table.capacity
      }
    },

    /** 待清台 → 空闲（M1: POST 清台接口） */
    cleanTable(tableId: number) {
      const t = this.tables.find(x => x.id === tableId)
      if (!t) return
      t.ui = 'idle'
      t.settledAt = null
      t.session = null
      t.status = 1
    },

    /** M1: POST /rest/merchant/table/open（乐观占位）→ 进 order 视图 */
    confirmOpen() {
      const t = this.tables.find(x => x.id === this.openForTableId)
      if (!t) return
      t.ui = 'busy'
      t.status = 2
      t.session = {
        sessionId: Date.now() % 100000,
        orderId: null,
        memberNum: this.guestPick,
        openTime: new Date().toISOString(),
        openMinutes: 0,
        goodsTotalQuantity: 0,
        goodsTotalPrice: 0,
        addedCount: 0
      }
      this.resumeOrder(t.id)
      this.openForTableId = null
    },

    cancelOpen() {
      this.openForTableId = null
    },

    /** 恢复既有会话进单（TicketScreen「继续点单」/ 点占用桌） */
    resumeOrder(tableId: number) {
      this.currentTableId = tableId
      // M1: 定位 sessionId + 拉取会话账单
      this.setView('order')
    },

    /** 加菜：同名合并 + 置 added（原型 pick 语义） */
    pickGoods(goodsId: number, goodsName: string, price: number, specText = '') {
      const found = this.cart.find(l => l.goodsId === goodsId && l.specText === specText)
      if (found) {
        found.qty++
        found.added = true
      } else {
        this.cart.push({ goodsId, goodsName, specText, price, qty: 1, added: true })
      }
    },

    /** 步进器：delta 负向减到 0 时——added 行删除，已下单行拒绝（一期决策） */
    changeQty(index: number, delta: number) {
      const line = this.cart[index]
      if (!line) return
      if (line.qty + delta <= 0) {
        if (!line.added) return // 已下单禁止退菜
        this.cart.splice(index, 1)
        return
      }
      line.qty += delta
      line.added = true
    },

    /** M1: POST /rest/merchant/order/send-items → added 明细落库送厨打，成功后清标 */
    async sendItems() {
      // 占位
    },

    /** 暂存挂单：不清 cart（保留 added 标记），返回桌台图 */
    holdOrder() {
      this.setView('floor')
    }
  }
})

/** 桌卡展示态推导（M0 直接读 ui；M1 兼容仅 status/session 的接口行） */
function uiOf(t: TableInfo): TableUiState {
  if (t.ui) return t.ui
  if (t.session) return 'busy'
  return 'idle'
}

/** 状态筛选文案 → 展示态 */
function statusLabelOf(label: string): TableUiState | null {
  switch (label) {
    case '空闲': return 'idle'
    case '占用': return 'busy'
    case '预定': return 'reserved'
    case '待清台': return 'cleaning'
    default: return null
  }
}
