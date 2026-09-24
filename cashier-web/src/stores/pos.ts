import { defineStore } from 'pinia'
import router from '@/router'
import type { CartLine, MenuChip, PosView, TableInfo, TableUiState } from '@/types'
import { clearTable, changeTable as changeTableApi, confirmPayment as confirmPaymentApi, fetchTableList, openTable, scanPay as scanPayApi, settleTable, type ZoneRow } from '@/api/table'
import { fetchGoods, fetchMenus, fetchSpecs, updateGoodsStatus, type GoodsRow, type MenuRow, type SpecGroupMap } from '@/api/menu'
import { submitOrder, type OrderDetailSubmit } from '@/api/order'

/** 点单模式：堂食（需台位）/ 外带自提（免台位，散客入口） */
export type OrderMode = 'dinein' | 'takeaway'

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
    /** 台位抽屉（批次 C）：null = 关闭 */
    drawerTableId: null as number | null,
    // ===== 点单区（对齐 W-04 三段式：商品区 566 ｜ 做法与备注区 290）=====
    menus: [] as MenuRow[],
    /** 本店全部在售商品（status ∈ 2,4），分类切换在本地过滤以便统计计数 */
    allGoods: [] as GoodsRow[],
    /** 当前分类：null = 全部 */
    activeMenuId: null as number | null,
    loadingGoods: false,
    /** 做法区当前商品（选中商品后才显示做法与备注） */
    picked: null as GoodsRow | null,
    specGroups: {} as SpecGroupMap,
    /** 规格名 → 选中项（单选组长度为 1；多选组见 MULTI_SPEC_GROUPS） */
    specSelected: {} as Record<string, string[]>,
    /** 做法区数量 */
    qty: 1,
    /** 整单备注 */
    remark: '',
    /** 单品备注 */
    itemRemark: '',
    loadingSpecs: false,
    /**
     * 点单模式（2026-09-23 补散客入口）
     * - 'dinein'   堂食：必须先在台位图点桌（占用台=加酒 / 空闲台=开台）
     * - 'takeaway' 外带自提：**免台位**，散客带走场景；仍落 tb_order，orderScene=2
     *   顾客端对「无桌号快照」的订单会自动展示「取餐码 A-0NN」（= 取餐号 queue_no 格式化），
     *   无需新字段/新接口 —— 这正是散客走取餐号而不是自提码的原因。
     */
    orderMode: 'dinein' as OrderMode
  }),

  getters: {
    /** 是否外带自提单（免台位） */
    isTakeaway(s): boolean {
      return s.orderMode === 'takeaway'
    },
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
    /** 图例计数：空闲 / 占用 / 待清台（批次 C 三态） */
    uiCounts(s): Record<TableUiState, number> {
      const c: Record<TableUiState, number> = { idle: 0, busy: 0, toclean: 0 }
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
    /** 台位抽屉当前台位（批次 C） */
    drawerTable(s): TableInfo | null {
      return s.tables.find(t => t.id === s.drawerTableId) ?? null
    },
    cartTotal(s): number {
      return s.cart.reduce((sum, l) => sum + l.price * l.qty, 0)
    },
    addedCount(s): number {
      return s.cart.filter(l => l.added).reduce((n, l) => n + l.qty, 0)
    },

    /** 当前分类下的商品（「全部」＝本地不筛选） */
    goods(s): GoodsRow[] {
      return s.activeMenuId == null ? s.allGoods : s.allGoods.filter(g => g.menuId === s.activeMenuId)
    },

    /** 分类横条：分类名 + 该分类商品数，「全部 N」置顶（W-04 83:67） */
    menuChips(s): MenuChip[] {
      return [
        { id: null, name: '全部', count: s.allGoods.length },
        ...s.menus.map(m => ({
          id: m.id,
          name: m.name,
          count: s.allGoods.filter(g => g.menuId === m.id).length
        }))
      ]
    },

    /** 做法区选中项的加价合计 */
    pickedAddon(s): number {
      let sum = 0
      for (const [group, names] of Object.entries(s.specSelected)) {
        for (const n of names) {
          const opt = s.specGroups[group]?.find(o => o.name === n)
          if (opt) sum += Number(opt.price || 0)
        }
      }
      return sum
    },

    /** 做法区单价 = 商品价 + 规格加价 */
    pickedPrice(s): number {
      if (!s.picked) return 0
      return Number(s.picked.salePrice || s.picked.price || 0) + this.pickedAddon
    },

    /** 已点商品件数（结算栏「已点商品（N 件）」） */
    billCount(s): number {
      return s.cart.reduce((n, l) => n + l.qty, 0)
    },

    /** 应收 = 商品合计（优惠 / 抹零本期恒为 0） */
    receivable(s): number {
      return s.cart.reduce((sum, l) => sum + l.price * l.qty, 0)
    }
  },

  actions: {
    setView(v: PosView) {
      this.view = v
      // 视图状态与 vue-router 单一事实源：跳转即导航
      const path = VIEW_PATH[v]
      if (router.currentRoute.value.path !== path) void router.push(path)
    },

    /** 路由 → 视图态反向同步（router.afterEach 调用）；非三主视图则不动，避免误改状态 */
    syncViewFromPath(path: string) {
      const hit = (Object.keys(VIEW_PATH) as PosView[]).find(k => VIEW_PATH[k] === path)
      if (hit && this.view !== hit) this.view = hit
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
          // 待清台由后端算好下发（批次 C），前端不重复推导口径
          awaitingClean: r.awaitingClean === true,
          ui: r.awaitingClean === true ? 'toclean' : r.status === 2 ? 'busy' : 'idle',
          session: r.sessionId
            ? {
                sessionId: r.sessionId,
                memberNum: r.memberNum ?? 0,
                openMinutes: r.openMinutes ?? 0,
                goodsTotalPrice: r.goodsTotalPrice ?? 0,
                addedCount: r.addedCount ?? 0,
                sessionStatus: r.sessionStatus ?? 1,
                closeTime: r.closeTime ?? null
              }
            : null
        }))
        this.zones = zoneList
      } catch (e) {
        console.error('[pos] loadTables 失败', e)
      }
    },

    /**
     * 桌卡点击路由（批次 C 三态）
     * - 空闲 → 开台弹窗
     * - 占用 → 恢复进单（加酒 / 转台）
     * - 待清台 → 开台位抽屉（查看该台订单 + 只读台位码 + 清台入口）
     */
    clickTable(table: TableInfo) {
      const ui = uiOf(table)
      if (ui === 'busy') {
        this.resumeOrder(table.id)
      } else if (ui === 'toclean') {
        this.drawerTableId = table.id
      } else {
        this.openForTableId = table.id
        this.guestPick = Math.max(1, Math.round(table.capacity / 2))
      }
    },

    /** 开台位抽屉 / 关闭 */
    openDrawer(tableId: number) {
      this.drawerTableId = tableId
    },
    closeDrawer() {
      this.drawerTableId = null
    },

    /**
     * 结账（批次 C 新增）：只闭台不放台 → 台位进「待清台」
     * 真实收款属批次 H（商户号未就绪），此处先提供状态流转入口，
     * 供批次 H 把「确认收款」串成「下单 → 收款 → settle → 待清台」。
     */
    async settle(tableId: number) {
      try {
        await settleTable(tableId)
        await this.loadTables()
      } catch (e) {
        console.error('[pos] 结账失败', e)
        alert((e as Error).message || '结账失败')
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
          addedCount: 0,
          sessionStatus: 1,
          closeTime: null
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

    /** 恢复既有会话进单（W-07 点桌带入）：进入代客点单并加载点单数据 */
    resumeOrder(tableId: number) {
      const t = this.tables.find(x => x.id === tableId)
      this.currentTableId = tableId
      this.currentSessionId = t?.session?.sessionId ?? null
      if (!this.allGoods.length) void this.loadOrderData()
      this.setView('order')
    },

    /**
     * 加载点单数据：分类 + 本店在售全量商品
     * 一次拉全（不分页），分类切换在本地过滤——这样「全部 N / 威士忌 6」的计数才有依据。
     */
    async loadOrderData() {
      this.loadingGoods = true
      try {
        const [menuPage, goodsPage] = await Promise.all([fetchMenus(), fetchGoods()])
        // 注意：Menu 实体无 isDisabled 字段（后端已按门店过滤），不过滤
        this.menus = menuPage?.records ?? []
        this.allGoods = goodsPage?.records ?? []
      } catch (e) {
        console.error('[pos] 加载点单数据失败', e)
      } finally {
        this.loadingGoods = false
      }
    },

    /** 切换分类横条（null ＝ 全部） */
    selectMenu(id: number | null) {
      this.activeMenuId = id
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

    /** 点商品 → 载入做法区（W-04：做法与备注常驻右栏，不再是弹窗） */
    async pickGoods(g: GoodsRow) {
      if (g.status === 4) return // 已沽清不可点（M-07）
      this.picked = g
      this.qty = 1
      this.itemRemark = ''
      this.specGroups = {}
      this.specSelected = {}
      this.loadingSpecs = true
      try {
        const groups = await fetchSpecs(g.id)
        // 防御：快速连点不同商品时，丢弃过期响应
        if (this.picked?.id !== g.id) return
        this.specGroups = groups ?? {}
        const selected: Record<string, string[]> = {}
        for (const [name, opts] of Object.entries(this.specGroups)) {
          const first = opts.find(o => o.stock !== 2)
          if (first) selected[name] = [first.name]
        }
        this.specSelected = selected
      } catch (e) {
        console.error('[pos] 查询规格失败', e)
      } finally {
        this.loadingSpecs = false
      }
    },

    /** 切换规格选项：多选组可增减（配搭/加料），单选组替换（杯型/冰量/甜度） */
    toggleSpec(group: string, option: string) {
      const cur = this.specSelected[group] ?? []
      if (isMultiSpecGroup(group)) {
        const next = cur.includes(option) ? cur.filter(v => v !== option) : [...cur, option]
        this.specSelected = { ...this.specSelected, [group]: next }
      } else {
        this.specSelected = { ...this.specSelected, [group]: [option] }
      }
    },

    /** 做法区数量步进 */
    bumpQty(delta: number) {
      this.qty = Math.max(1, this.qty + delta)
    },

    /** 加入购物车（商品 + 规格组合 + 单品备注 相同才合并） */
    addToCart() {
      const g = this.picked
      if (!g) return
      const specs: Record<string, string[]> = {}
      for (const [k, v] of Object.entries(this.specSelected)) {
        if (v.length) specs[k] = [...v]
      }
      const key = JSON.stringify(specs) + '|' + this.itemRemark
      const found = this.cart.find(l => l.goodsId === g.id && cartLineKey(l) === key)
      if (found) {
        found.qty += this.qty
        found.added = true
      } else {
        this.cart.push({
          goodsId: g.id,
          goodsName: g.name,
          specText: Object.values(specs).flat().join(' / '),
          specs,
          remark: this.itemRemark,
          price: this.pickedPrice,
          qty: this.qty,
          added: true
        })
      }
      // 加购后收起做法区，回到商品网格，贴合收银连点动线
      this.picked = null
      this.specGroups = {}
      this.specSelected = {}
      this.qty = 1
      this.itemRemark = ''
    },

    /** 清空已点商品（结算栏「清空」） */
    clearCart() {
      this.cart = []
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

    /**
     * 切换点单模式。
     * 切到外带自提时**清空台位上下文**，避免把外带单挂到别人的台位上；
     * 切回堂食同样清空，要求重新去台位图点桌（不猜台位）。
     */
    setOrderMode(m: OrderMode) {
      if (this.orderMode === m) return
      this.orderMode = m
      this.currentTableId = null
      this.currentSessionId = null
    },

    /**
     * 下单送厨：方案 A 一次性提交购物车 → POST /rest/merchant/order/insert
     * 返回 { ok, orderNo }：orderNo 供「确认收款」在外带（无台位）时定位订单
     */
    async sendItems(): Promise<{ ok: boolean; orderNo?: string }> {
      const takeaway = this.orderMode === 'takeaway'
      const table = this.currentTable
      const sessionId = this.currentSessionId
      // 堂食单仍必须绑台位；**外带自提单免台位**（散客入口，2026-09-23）
      if (!takeaway && (!table || sessionId == null)) {
        alert('堂食单需先在台位图点桌；如需免台位下单，请切到「外带自提」模式')
        return { ok: false }
      }
      const lines = this.cart.filter(l => l.added)
      if (!lines.length) {
        alert('购物车为空')
        return { ok: false }
      }
      const details: OrderDetailSubmit[] = lines.map(l => ({
        goodsId: l.goodsId,
        goodsName: l.goodsName,
        // 规格 → 后端 specList JSON（见 toSpecListJson 的多选编码说明）
        specList: toSpecListJson(l.specs),
        number: l.qty,
        price: l.price,
        subtotal: l.price * l.qty
      }))
      const total = details.reduce((s, d) => s + d.subtotal, 0)
      try {
        const created = await submitOrder({
          orderDetailListStr: JSON.stringify(details),
          actualPrice: total,
          // 外带自提单不带台位/会话（后端 insertByMerchant 不校验，仅 setTableId(param)）
          tableId: takeaway ? null : table!.id,
          tableNo: takeaway ? null : table!.tableNo,
          tableName: takeaway ? null : table!.tableName,
          diningNum: takeaway ? 1 : (table?.session?.memberNum ?? 1),
          diningSessionId: takeaway ? null : sessionId,
          orderScene: takeaway ? 2 : 1
        })
        // 下单成功：清空已提交行（保留已下单行语义：本期无已下单行，直接全清）
        this.cart = []
        return { ok: true, orderNo: created?.orderNo }
      } catch (e) {
        console.error('[pos] 下单失败', e)
        alert((e as Error).message || '下单失败')
        return { ok: false }
      }
    },

    /**
     * 确认收款（2026-09-24 新增）：与下单同一动作完成「点单即收款」
     *
     * 堂食传 tableId（后端收该桌全部未付单，含漏收与加酒）；外带传 orderNo。
     * payMode：1=微信收款码 4=到店支付(现金)。
     * ⚠️ 目前为**人工确认收款**（线下现金/顾客扫店内收款码后由收银员点确认），
     *    系统不参与真实资金流 —— 微信网页收款需 Native/被扫通道，属批次 H（商户号未就绪）。
     * @returns 收款金额（后端口径）；已收过返回 0
     */
    async confirmPayment(param: { tableId?: number | null; orderNo?: string | null; payMode: number }) {
      try {
        const data = await confirmPaymentApi(param)
        await this.loadTables()
        return data
      } catch (e) {
        console.error('[pos] 确认收款失败', e)
        alert((e as Error).message || '确认收款失败')
        return null
      }
    },

    /**
     * 扫码收款（付款码支付 · 被扫，2026-09-24 新增）
     *
     * 扫顾客微信付款码 → 后端调微信真扣款。成功即重拉台位列表。
     * ⚠️ 失败**不 alert、不吞异常**：归一化成 {ok:false, message} 返回，
     *    由扫码弹层就地显示微信原文（「付款码已过期」「余额不足」等），
     *    收银员据此决定重扫、换码还是改用现金。
     */
    async scanPay(param: { tableId?: number | null; orderNo?: string | null; authCode: string }) {
      try {
        const data = await scanPayApi(param)
        if (data.status === 'SUCCESS') {
          await this.loadTables()
        }
        return {
          ok: data.status === 'SUCCESS',
          status: data.status,
          paidAmount: data.paidAmount,
          transactionId: data.transactionId,
          message: data.message || ''
        }
      } catch (e) {
        console.error('[pos] 扫码收款失败', e)
        return {
          ok: false,
          status: undefined as string | undefined,
          paidAmount: undefined as number | undefined,
          transactionId: undefined as string | undefined,
          message: (e as Error).message || '扫码收款失败'
        }
      }
    },

    /** 暂存挂单：不清 cart（保留 added 标记），返回桌台图 */
    holdOrder() {
      this.setView('floor')
    }
  }
})

/** 桌卡展示态推导（Round 8 两态：busy=有进行中会话，否则 idle） */
/**
 * 桌卡展示态推导（批次 C 三态）
 * - toclean：后端下发 awaitingClean（台位仍占用但会话已结台）
 * - busy：有进行中会话或台位占用
 * - idle：其余
 */
function uiOf(t: TableInfo): TableUiState {
  if (t.awaitingClean) return 'toclean'
  if (t.session) return 'busy'
  if (t.status === 2) return 'busy'
  return 'idle'
}

/** 三主视图 ↔ 路由路径（对齐 config/menu.ts，批次 A 新路由表） */
const VIEW_PATH: Record<PosView, string> = {
  floor: '/table/floor',
  order: '/order/cashier',
  ticket: '/orders/today'
}

/**
 * 多选规格组判定
 * 库中 `tb_goods_specification` 无「是否多选」标志位，按组名约定：
 * 「加料 / 配搭 / 配料 / 小料」可多选（W-04 画板写作「配搭（可多选）」），杯型 / 冰量 / 甜度 单选。
 */
const MULTI_SPEC_KEYWORDS = ['加料', '配搭', '配料', '小料']
function isMultiSpecGroup(groupName: string): boolean {
  return MULTI_SPEC_KEYWORDS.some(k => groupName.includes(k))
}

/** 购物车行合并键：同商品 + 同规格组合 + 同单品备注才合并 */
function cartLineKey(l: CartLine): string {
  return JSON.stringify(l.specs) + '|' + l.remark
}

/**
 * 规格选择 → 后端 `specList` JSON
 * 后端 `insertByMerchant` 以 `(String) map.get(key)` 逐值强转并交给
 * `selectSumPriceByGoodsIdAndName(goodsId, List<String> nameList)` 按名字求和加价，
 * 因此**值必须是字符串**（传数组会 ClassCastException）。
 * 多选组用「组名」「组名2」「组名3」… 后缀键编码，值仍是各自的选项名，加价可正确累加。
 */
function toSpecListJson(specs: Record<string, string[]>): string {
  const out: Record<string, string> = {}
  for (const [group, names] of Object.entries(specs)) {
    names.forEach((n, i) => {
      out[i === 0 ? group : `${group}${i + 1}`] = n
    })
  }
  return JSON.stringify(out)
}

/** 状态筛选文案 → 展示态 */
function statusLabelOf(label: string): TableUiState | null {
  switch (label) {
    case '空闲': return 'idle'
    case '占用': return 'busy'
    case '待清台': return 'toclean'
    default: return null
  }
}
