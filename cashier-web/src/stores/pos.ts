import { defineStore } from 'pinia'
import router from '@/router'
import type { CartLine, PosView, TableInfo, TableUiState } from '@/types'
import { clearTable, changeTable as changeTableApi, fetchTableList, openTable, type ZoneRow } from '@/api/table'

/**
 * 收银台全局状态（Odoo PosStore / Screen 状态机映射）
 * - view: floor（桌台总览）/ order（点单管理）/ ticket（未结订单）
 * - tables: /rest/merchant/diningTable/list 全量下发，唯一事实源（Round 9 不分页）
 * - cart: 当前会话账单本地态；added=true 即原型「新加未下单」金标
 * 本期口径（grill Round 0-10）：开台/清台/转台 三作业；两态 idle/busy；低消不做；并台/待清台/预定不做
 */
export const usePosStore = defineStore('pos', {
  state: () => ({
    view: 'floor' as PosView,
    zone: '全部区域',
    statusFilter: '全部状态',
    keyword: '',
    shopId: 13, // 登录态注入后覆盖（商家 siam / shopId=13）
    tables: [] as TableInfo[],
    zones: [] as ZoneRow[],
    currentTableId: null as number | null,
    currentSessionId: null as number | null,
    cart: [] as CartLine[],
    // 开台弹窗中间态
    openForTableId: null as number | null,
    guestPick: 2,
    // 转台弹窗中间态（Round 5 拍板弹窗选目标台）
    changeForTableId: null as number | null
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
    /** 图例计数：空闲 / 占用（Round 8 两态） */
    uiCounts(s): Record<TableUiState, number> {
      const c: Record<TableUiState, number> = { idle: 0, busy: 0 }
      for (const t of s.tables) c[uiOf(t)]++
      return c
    },
    busyTables(s): TableInfo[] {
      return s.tables.filter(t => uiOf(t) === 'busy')
    },
    diningCount(s): number {
      return s.tables.reduce((n, t) => n + (t.session?.memberNum ?? 0), 0)
    },
    /** 转台弹窗候选：空闲台（排除自身） */
    changeTargets(s): TableInfo[] {
      const src = s.changeForTableId
      return s.tables.filter(t => t.id !== src && t.status === 1)
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
      // 视图状态与 vue-router 单一事实源：跳转即导航
      if (router.currentRoute.value.path !== `/${v}`) void router.push(`/${v}`)
    },

    /** 桌台图数据：GET list（Round 9 不分页全量；zone 动态化） */
    async loadTables() {
      try {
        const { tables, zoneList } = await fetchTableList()
        // 后端行 → 前端 TableInfo（ui 由 status+session 推导）
        this.tables = tables.map(r => ({
          id: r.id,
          shopId: this.shopId,
          tableNo: r.tableNo,
          tableName: r.tableName,
          typeId: null,
          capacity: r.capacity,
          status: r.status,
          zone: r.zone,
          typeName: r.typeName,
          minSpend: r.minSpend ?? 0,
          ui: r.status === 2 ? 'busy' : 'idle',
          session: r.sessionId
            ? {
                sessionId: r.sessionId,
                memberNum: r.memberNum ?? 0,
                openMinutes: r.openMinutes ?? 0,
                goodsTotalPrice: r.goodsTotalPrice ?? 0,
                addedCount: r.addedCount ?? 0
              }
            : null
        }))
        this.zones = zoneList
      } catch (e) {
        console.error('[pos] loadTables 失败', e)
      }
    },

    /** 桌卡点击路由：占用→恢复进单；空闲→开台弹窗 */
    clickTable(table: TableInfo) {
      const ui = uiOf(table)
      if (ui === 'busy') {
        this.resumeOrder(table.id)
      } else if (ui === 'idle') {
        this.openForTableId = table.id
        this.guestPick = Math.max(1, Math.round(table.capacity / 2))
      }
    },

    /** 开台：调接口，失败回滚并提示 */
    async confirmOpen() {
      const t = this.tables.find(x => x.id === this.openForTableId)
      if (!t) return
      try {
        const { sessionId } = await openTable(t.id, this.guestPick)
        t.status = 2
        t.ui = 'busy'
        t.session = {
          sessionId,
          memberNum: this.guestPick,
          openMinutes: 0,
          goodsTotalPrice: 0,
          addedCount: 0
        }
        this.currentSessionId = sessionId
        this.openForTableId = null
        this.resumeOrder(t.id)
      } catch (e) {
        console.error('[pos] 开台失败', e)
        alert((e as Error).message || '开台失败')
      }
    },

    cancelOpen() {
      this.openForTableId = null
    },

    /** 转台：弹窗选目标台 → 调接口 → 刷新 */
    startChange(tableId: number) {
      this.changeForTableId = tableId
    },

    cancelChange() {
      this.changeForTableId = null
    },

    async confirmChange(targetTableId: number) {
      const srcId = this.changeForTableId
      if (srcId == null) return
      try {
        await changeTableApi(srcId, targetTableId)
        this.changeForTableId = null
        await this.loadTables()
      } catch (e) {
        console.error('[pos] 转台失败', e)
        alert((e as Error).message || '转台失败')
      }
    },

    /** 清台：调接口，失败回滚 */
    async confirmClear(tableId: number) {
      try {
        await clearTable(tableId)
        await this.loadTables()
      } catch (e) {
        console.error('[pos] 清台失败', e)
        alert((e as Error).message || '清台失败')
      }
    },

    /** 恢复既有会话进单（TicketScreen「继续点单」/ 点占用桌） */
    resumeOrder(tableId: number) {
      this.currentTableId = tableId
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

    /** M1: POST /rest/merchant/order/send-items → added 明细落库送厨打（OrderView 空壳期占位） */
    async sendItems() {
      // 占位
    },

    /** 暂存挂单：不清 cart（保留 added 标记），返回桌台图 */
    holdOrder() {
      this.setView('floor')
    }
  }
})

/** 桌卡展示态推导（Round 8 两态：busy=有进行中会话，否则 idle） */
function uiOf(t: TableInfo): TableUiState {
  if (t.session) return 'busy'
  if (t.status === 2) return 'busy'
  return 'idle'
}

/** 状态筛选文案 → 展示态 */
function statusLabelOf(label: string): TableUiState | null {
  switch (label) {
    case '空闲': return 'idle'
    case '占用': return 'busy'
    default: return null
  }
}
