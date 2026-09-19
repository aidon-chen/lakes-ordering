import { defineStore } from 'pinia'
import router from '@/router'
import type { CartLine, PosView, TableInfo, TableUiState } from '@/types'
import { clearTable, changeTable as changeTableApi, fetchTableList, openTable, type ZoneRow } from '@/api/table'
import { fetchGoods, fetchMenus, fetchSpecs, updateGoodsStatus, type GoodsRow, type MenuRow, type SpecGroupMap } from '@/api/menu'
import { submitOrder, type OrderDetailSubmit } from '@/api/order'

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
    changeForTableId: null as number | null,
    // 点单数据
    menus: [] as MenuRow[],
    goods: [] as GoodsRow[],
    currentMenuId: null as number | null,
    loadingGoods: false,
    // 规格选择弹窗态（点商品若带规格 → 先选规格再加购）
    specGoods: null as GoodsRow | null,
    specGroups: {} as SpecGroupMap,
    specSelected: {} as Record<string, string>,
    loadingSpecs: false
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
        // 清台后复位点单上下文：该台位已释放，避免后续下单挂到已关闭会话
        if (this.currentTableId === tableId) {
          this.currentTableId = null
          this.currentSessionId = null
          this.cart = []
          this.setView('floor')
        }
        await this.loadTables()
      } catch (e) {
        console.error('[pos] 清台失败', e)
        alert((e as Error).message || '清台失败')
      }
    },

    /** 清台入口：有未结金额时二次确认（M-05 清台作业） */
    async askClear(tableId: number) {
      const t = this.tables.find(x => x.id === tableId)
      if (!t) return
      const unpaid = t.session?.goodsTotalPrice ?? 0
      const tip = unpaid > 0
        ? `${t.tableNo} 尚有点单未结 ¥${unpaid.toFixed(2)}，确认清台？`
        : `确认清台 ${t.tableNo}？`
      if (!window.confirm(tip)) return
      await this.confirmClear(tableId)
    },

    /** 恢复既有会话进单（点占用桌）：进入 order 视图并加载点单数据 */
    resumeOrder(tableId: number) {
      const t = this.tables.find(x => x.id === tableId)
      this.currentTableId = tableId
      this.currentSessionId = t?.session?.sessionId ?? null
      if (!this.menus.length) void this.loadMenus()
      if (this.currentMenuId != null) void this.loadGoods(this.currentMenuId)
      this.setView('order')
    },

    /** 加载商品分类（一次） */
    async loadMenus() {
      try {
        const page = await fetchMenus()
        // 注意：Menu 实体无 isDisabled 字段（后端已按门店过滤），不过滤
        this.menus = page?.records ?? []
        if (!this.menus.length) return
        const first = this.menus[0]
        this.currentMenuId = this.currentMenuId ?? first.id
        await this.loadGoods(this.currentMenuId)
      } catch (e) {
        console.error('[pos] loadMenus 失败', e)
      }
    },

    /** 按分类加载商品 */
    async loadGoods(menuId: number) {
      this.currentMenuId = menuId
      this.loadingGoods = true
      try {
        const page = await fetchGoods(menuId)
        this.goods = page?.records ?? []
      } catch (e) {
        console.error('[pos] loadGoods 失败', e)
      } finally {
        this.loadingGoods = false
      }
    },

    /**
     * M-07 沽清 / 恢复出餐（2=可出餐 4=沽清）
     * 失败不改本地态，保证界面与后端一致
     */
    async toggleSoldOut(g: GoodsRow) {
      const next: 2 | 4 = g.status === 4 ? 2 : 4
      try {
        await updateGoodsStatus(g.id, next)
        g.status = next
      } catch (e) {
        console.error('[pos] 沽清切换失败', e)
        alert((e as Error).message || '操作失败')
      }
    },

    /** 点商品：先查规格，有规格组 → 弹规格选择窗；无规格 → 直接加购（默认价） */
    async pickGoodsWithSpec(g: GoodsRow) {
      try {
        this.loadingSpecs = true
        const groups = await fetchSpecs(g.id)
        const hasSpec = groups && Object.keys(groups).length > 0
        if (!hasSpec) {
          // 无规格：直接加购，specText 为空（后端 specList 传 {}）
          this.pickGoods(g.id, g.name, Number(g.salePrice || g.price || 0), '')
          return
        }
        // 有规格：弹窗，每组默认选第一个有货项
        const selected: Record<string, string> = {}
        for (const [specName, opts] of Object.entries(groups)) {
          const firstAvail = opts.find(o => o.stock !== 2)
          if (firstAvail) selected[specName] = firstAvail.name
        }
        this.specGroups = groups
        this.specSelected = selected
        this.specGoods = g
      } catch (e) {
        console.error('[pos] 查询规格失败，按无规格加购', e)
        this.pickGoods(g.id, g.name, Number(g.salePrice || g.price || 0), '')
      } finally {
        this.loadingSpecs = false
      }
    },

    /** 规格弹窗：切换某规格组选项 */
    selectSpecOption(specName: string, optionName: string) {
      this.specSelected = { ...this.specSelected, [specName]: optionName }
    },

    /** 规格弹窗：确认加购（specText 拼 "规格:选项" 可读串；specList JSON 由下单时生成） */
    confirmSpec() {
      const g = this.specGoods
      if (!g) return
      const base = Number(g.salePrice || g.price || 0)
      // 计算加价：选中选项的 price 累加（与后端 selectSumPriceByGoodsIdAndName 一致）
      let addon = 0
      for (const [specName, optionName] of Object.entries(this.specSelected)) {
        const opt = this.specGroups[specName]?.find(o => o.name === optionName)
        if (opt) addon += Number(opt.price || 0)
      }
      const specText = Object.values(this.specSelected).join(' / ')
      this.pickGoods(g.id, g.name, base + addon, specText)
      this.specGoods = null
      this.specGroups = {}
      this.specSelected = {}
    },

    /** 取消规格弹窗 */
    cancelSpec() {
      this.specGoods = null
      this.specGroups = {}
      this.specSelected = {}
    },

    /** 加菜：同名+同规格合并 + 置 added（原型 pick 语义） */
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

    /** 下单送厨：方案 A 一次性提交购物车 → POST /rest/merchant/order/insert */
    async sendItems(): Promise<boolean> {
      const table = this.currentTable
      const sessionId = this.currentSessionId
      if (!table || sessionId == null) {
        alert('未关联台位会话，无法下单')
        return false
      }
      const lines = this.cart.filter(l => l.added)
      if (!lines.length) {
        alert('购物车为空')
        return false
      }
      const details: OrderDetailSubmit[] = lines.map(l => {
        // specList JSON：无规格传 {}；有规格时 specText 存的是「选项值」用「 / 」连接，
        // 但后端按 value(name) 查加价，需还原为 {规格名:选项值} 才能正确匹配——当前实现
        // 简化：specText 为「选项1 / 选项2」时逐值放 {规格: 选项}，适配单规格组商品。
        // ⚠️ 多规格组商品（如 规格+温度）需扩展 CartLine 存 {规格名:选项} 映射，二期处理。
        const specList = l.specText
          ? JSON.stringify(l.specText.includes(' / ') ? { 规格: l.specText } : { 规格: l.specText })
          : '{}'
        return {
          goodsId: l.goodsId,
          goodsName: l.goodsName,
          specList,
          number: l.qty,
          price: l.price,
          subtotal: l.price * l.qty
        }
      })
      const total = details.reduce((s, d) => s + d.subtotal, 0)
      try {
        await submitOrder({
          orderDetailListStr: JSON.stringify(details),
          actualPrice: total,
          tableId: table.id,
          tableNo: table.tableNo,
          tableName: table.tableName,
          diningNum: table.session?.memberNum ?? 1,
          diningSessionId: sessionId,
          orderScene: 1
        })
        // 下单成功：清空已提交行（保留已下单行语义：本期无已下单行，直接全清）
        this.cart = []
        return true
      } catch (e) {
        console.error('[pos] 下单失败', e)
        alert((e as Error).message || '下单失败')
        return false
      }
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
