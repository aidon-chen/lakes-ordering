<template>
  <!--
    W-14 出品进度看板（Ardot 2:978）— 深色厨房看板
    顶栏 1392×52 ｜ 三列 1392×700（列 453×700 r8，卡 429×96 r6，列间距 16）｜ 底部说明
    色值取自画板设计变量：列底 $2:14=#262E17，未选字 $2:16=#C9CCBF，
      卡底 $2:26=#FFFFFF，已出品卡 $2:13=#EDF2DE，取餐号 $2:24=#404A21
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-4" style="background: #171C0F">
    <!-- 顶栏 2:979 -->
    <div class="shrink-0 flex items-center" style="height: 52px">
      <span class="text-[20px] font-medium" style="color: #FFFFFF">
        出品进度看板 · 湖里Lakes（M-04 出品进度跟踪）
      </span>
      <span class="flex-1" />
      <button
        class="rounded text-[13px] disabled:opacity-50"
        style="height: 32px; padding: 0 16px; background: var(--p); color: #fff"
        :disabled="loading"
        @click="load"
      >刷新</button>
      <button
        class="ml-3 text-[13px]"
        style="color: #C9CCBF"
        @click="toggleFullscreen"
      >⛶ 全屏</button>
    </div>

    <!-- 三列 2:991 -->
    <div class="flex-1 min-h-0 grid" style="grid-template-columns: repeat(3, 1fr); gap: 16px">
      <section
        v-for="col in columns"
        :key="col.key"
        class="flex flex-col rounded-lg min-h-0"
        style="padding: 12px; background: #262E17; gap: 10px"
      >
        <h3 class="shrink-0 text-[16px] font-medium" style="color: #FFFFFF">{{ col.title }}</h3>

        <div class="flex-1 min-h-0 overflow-y-auto flex flex-col" style="gap: 10px">
          <div v-if="!col.items.length" class="text-[12px]" style="color: #C9CCBF">暂无</div>

          <!-- 卡片（2026-09-23 改造）：点卡片任意处 → 开详情弹窗；状态流转在右下角**小按钮** -->
          <div
            v-for="it in col.items"
            :key="it.order.id"
            class="text-left rounded-md cursor-pointer"
            :style="cardStyle(col.key)"
            :title="col.nextLabel"
            @click="openDetail(it)"
          >
            <div class="flex items-baseline">
              <span class="text-[20px] font-bold" style="color: #404A21">{{ headText(it) }}</span>
              <span
                class="ml-auto text-[12px] font-medium"
                :style="{ color: col.key === 'done' ? '#6B7A5A' : waitedColor(it.order) }"
              >{{ col.key === 'done' ? doneTimeText(it.order) : waitedText(it.order) }}</span>
            </div>
            <span class="shrink-0 text-[13px] leading-snug line-clamp-2" style="color: #616166">{{ itemsText(it) }}</span>
            <div class="flex justify-end" style="margin-top: 8px; gap: 6px">
              <!-- 改备注（待制作/出品中可改；已出品不改） -->
              <button
                v-if="col.key !== 'done'"
                class="rounded text-[12px]"
                style="height: 24px; padding: 0 10px; border: 1px solid #C9CCBF; color: #616166"
                @click.stop="openRemark(it)"
              >改备注</button>
              <button
                v-if="col.key !== 'done'"
                class="rounded text-[12px] disabled:opacity-50"
                style="height: 24px; padding: 0 10px; background: var(--p); color: #fff"
                :disabled="busyId === it.order.id"
                @click.stop="advance(it.order, col.key)"
              >{{ col.btnLabel }}</button>
              <button
                v-else
                class="rounded text-[12px]"
                style="height: 24px; padding: 0 10px; border: 1px solid #C9CCBF; color: #616166"
                :disabled="busyId === it.order.id"
                @click.stop="revert(it.order)"
              >↩ 撤销</button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 底部说明 2:1026 -->
    <p class="shrink-0 text-[12px]" style="color: #C9CCBF">{{ noteText }}</p>

    <!-- 详情弹窗（2026-09-23 新增）：点卡片打开，复用「今日订单」详情页的左半部分组件 -->
    <Teleport to="body">
      <div
        v-if="detailItem"
        class="flex items-center justify-center"
        style="position: fixed; inset: 0; z-index: 50; background: rgba(0, 0, 0, .45)"
        @click.self="detailItem = null"
      >
        <div class="bg-white rounded-lg flex flex-col" style="width: 760px; max-width: 92vw; max-height: 86vh; padding: 16px; gap: 12px">
          <div class="shrink-0 flex items-center">
            <span class="text-[15px] font-medium" style="color: var(--tc-title)">订单详情</span>
            <span class="ml-auto text-[13px] cursor-pointer" style="color: var(--tc-sub)" @click="detailItem = null">✕</span>
          </div>
          <div class="flex-1 min-h-0 overflow-y-auto">
            <OrderDetailPanels :order="detailItem.order" :details="detailItem.details" />
          </div>
          <div class="shrink-0 flex items-center" style="gap: 8px; border-top: 1px solid #E5E7EB; padding-top: 12px">
            <span class="text-[12px]" style="color: var(--tc-sub)">
              出品状态：<span style="color: var(--p)">{{ productionStatusText(detailItem.order.productionStatus ?? 1) }}</span>
            </span>
            <span class="flex-1" />
            <button v-if="modalStatus < 2" class="rounded text-[12px] text-white" style="height: 28px; padding: 0 12px; background: var(--p)"
              :disabled="busyId === detailItem.order.id" @click="advance(detailItem.order, 'todo')">开始出品</button>
            <button v-else-if="modalStatus < 3" class="rounded text-[12px] text-white" style="height: 28px; padding: 0 12px; background: var(--p)"
              :disabled="busyId === detailItem.order.id" @click="advance(detailItem.order, 'doing')">标记已出品</button>
            <button v-else class="rounded text-[12px]" style="height: 28px; padding: 0 12px; border: 1px solid var(--tc-line); color: var(--tc-gray)"
              :disabled="busyId === detailItem.order.id" @click="revert(detailItem.order)">↩ 撤销为出品中</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 改备注弹窗（2026-09-23）：待制作/出品中当场改整单 + 单品备注 -->
    <OrderRemarkDialog
      v-if="remarkItem"
      :order-id="remarkItem.order.id"
      :order-no="remarkItem.order.queueNo"
      :remark="remarkItem.order.remark"
      :details="remarkItem.details"
      @close="remarkItem = null"
      @done="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchMerchantOrders,
  pickupCodeText,
  productionStatusText,
  fetchOrderDetails,
  updateProductionStatus,
  type MerchantOrderRow,
  type OrderDetailRow
} from '@/api/order'
import { fetchTableList } from '@/api/table'
import OrderDetailPanels from '@/components/order/OrderDetailPanels.vue'
import OrderRemarkDialog from '@/components/order/OrderRemarkDialog.vue'

/** 看板项：订单 + 其明细（品项串） */
interface KanbanItem {
  order: MerchantOrderRow
  details: OrderDetailRow[]
}

const orders = ref<MerchantOrderRow[]>([])
const detailMap = ref<Record<number, OrderDetailRow[]>>({})
/** 台位号 → 类型名（「A03 · 散台」口径），取自 diningTable/list */
const typeByTableNo = ref<Record<string, string>>({})
const loading = ref(true)
const busyId = ref<number | null>(null)

/** 详情弹窗当前项（null = 关闭）；明细直接取内存里的 detailMap，无需再请求 */
const detailItem = ref<KanbanItem | null>(null)

function openDetail(it: KanbanItem) {
  detailItem.value = it
}

/** 改备注弹窗当前项（null = 关闭） */
const remarkItem = ref<KanbanItem | null>(null)

function openRemark(it: KanbanItem) {
  remarkItem.value = it
}

/** 弹窗里当前这条的出品状态（1/2/3） */
const modalStatus = computed(() => {
  const o = detailItem.value?.order
  return o ? (o.productionStatus ?? 1) : 1
})

/**
 * 等待时长的颜色分级（2026-09-23 新增）：常规灰 / 超 15 分琥珀 / 超 30 分红。
 * 卡片底是浅色（白或浅绿），故用深色调保证可读。
 */
function waitedColor(o: MerchantOrderRow): string {
  const t = parseTime(o.createTime)
  if (!t) return '#8F9499'
  const min = Math.floor((Date.now() - t) / 60000)
  if (min >= 30) return '#A32D2D'
  if (min >= 15) return '#854F0B'
  return '#8F9499'
}

/** 撤销：已出品(3) → 出品中(2)。原实现 `if (cur >= 3) return` 导致误点无法回退 */
async function revert(o: MerchantOrderRow) {
  busyId.value = o.id
  try {
    await updateProductionStatus(o.id, 2)
    await load()
  } catch (e) {
    console.error('[kanban] 撤销失败', e)
    alert((e as Error).message || '操作失败')
  } finally {
    busyId.value = null
  }
}

const KEY_TO_STATUS: Record<string, 1 | 2 | 3> = { todo: 1, doing: 2, done: 3 }

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [orderRes, tableRes] = await Promise.all([fetchMerchantOrders({ pageNo: 1, pageSize: 200 }), fetchTableList()])
    orders.value = orderRes?.records ?? []
    const map: Record<string, string> = {}
    for (const t of tableRes?.tables ?? []) map[t.tableNo] = t.typeName
    typeByTableNo.value = map

    // 明细（品项）按需拉取：只为看板会显示的订单取，避免全量 N+1
    // 2026-09-24：已出品(3)也拉明细 —— 否则已出品卡的品项串回落成「N 件」，出品员没法对单
    const needDetail = orders.value.filter(o => [1, 2, 3].includes(o.productionStatus ?? 1))
    const results = await Promise.all(
      needDetail.map(async o => {
        try {
          const r = await fetchOrderDetails(o.id)
          return [o.id, r?.records ?? []] as const
        } catch (e) {
          console.error('[kanban] 明细拉取失败 orderId=' + o.id, e)
          return [o.id, [] as OrderDetailRow[]] as const
        }
      })
    )
    detailMap.value = Object.fromEntries(results)
  } catch (e) {
    console.error('[kanban] 加载失败', e)
  } finally {
    loading.value = false
  }
}

/** 三列：待制作 / 出品中 / 已出品（列名与计数口径见画板 2:992~2:997） */
const columns = computed(() => {
  // ⚠️ productionStatus 可能为 null（后端列未就绪时）→ 按 DB 默认值 1=待制作 兜底，避免看板三列全空
  const psOf = (o: MerchantOrderRow) => o.productionStatus ?? 1
  const pick = (s: number) => orders.value.filter(o => psOf(o) === s).map(o => item(o))
  return [
    { key: 'todo', title: `待制作（${pick(1).length}）`, items: pick(1), nextLabel: '开始出品', btnLabel: '开始出品' },
    { key: 'doing', title: `出品中（${pick(2).length}）`, items: pick(2), nextLabel: '标记已出品', btnLabel: '标记已出品' },
    { key: 'done', title: `已出品（${pick(3).length} 今日）`, items: pick(3), nextLabel: '已出品', btnLabel: '已出品' }
  ]
})

function item(o: MerchantOrderRow): KanbanItem {
  return { order: o, details: detailMap.value[o.id] ?? [] }
}

function cardStyle(key: string) {
  const bg = key === 'done' ? '#EDF2DE' : '#FFFFFF'
  // ⚠️ 2026-09-23：卡片加了「改备注 + 状态」按钮行后，原 min-height 96px 不够 ——
  // flex 列会把中间的品项串压到 3px（实测：文字高度 3px ⇒ 看起来"那行字不见了"）。
  // 故加高到 124px；同时给品项串加 shrink-0，确保任何情况下都不被压扁。
  // 2026-09-23 对齐 Ardot 原型（file 728914844787406）的统一卡片规范：
  // 白底/浅绿底 + 描边 1px #D4DBC7 + 圆角 8 + gap 8 + min-height 124
  return `background: ${bg}; padding: 12px; min-height: 124px; display: flex; flex-direction: column; gap: 8px; border: 1px solid #D4DBC7; border-radius: 8px`
}

/**
 * 头行：「A03 · 散台」（取餐号 + 台位类型，类型来自 diningTable/list）
 *
 * 2026-09-23 修正：**外带自提单必须明确标注**。
 * 外带单的 tableNo 落的是占位「外带」→ 按台位号查类型查不到 → 原先只显示一个数字，
 * 出品员看不出「该送桌还是该喊号自取」，交接必错。
 * 判定口径与顾客端 isDineIn 保持一致：**有无桌号快照**（tableName 为空即非堂食）。
 */
function headText(it: KanbanItem): string {
  const o = it.order
  // 2026-09-23：一级界面显示**取餐码**（A-00N 形态），与顾客端/订阅消息同口径 —— 店员与顾客对得上号
  const no = o.queueNo ? pickupCodeText(o.queueNo) : (o.tableNo ?? '—')
  const isTakeaway = !o.tableName || !String(o.tableName).trim()
  if (isTakeaway) return `${no} · 外带自取`
  const type = typeByTableNo.value[o.tableNo ?? ''] ?? ''
  return type ? `${no} · ${type}` : `${no} · 台位`
}

/** 品项串：「威士忌(单杯·加冰)×2 · 果盘×1」；无明细时回落数量 */
function itemsText(it: KanbanItem): string {
  if (!it.details.length) {
    const n = it.order.goodsTotalQuantity ?? 0
    return n ? `${n} 件` : '—'
  }
  return it.details
    .slice(0, 3)
    .map(d => {
      const spec = specText(d.specList)
      return `${d.goodsName ?? '—'}${spec ? `(${spec})` : ''}×${d.number ?? 1}`
    })
    .join(' · ')
}

/** specList 是 JSON 串（如 {"规格":"单杯","加料":"加冰"}），取值为可读串 */
function specText(specList: string | null): string {
  if (!specList) return ''
  try {
    const o = JSON.parse(specList)
    return Object.keys(o)
      .filter(k => !/\d+$/.test(k)) // 跳过「加料2」这类后缀键（多选用编码键）
      .map(k => String(o[k]))
      .filter(Boolean)
      .join('·')
  } catch {
    return ''
  }
}

/** 已等：从下单至今（画板口径 2m10s） */
/**
 * 已出品卡片的「出品时间」（2026-09-24 新增，业务方要求已出品卡带时间信息）。
 *
 * ⚠️ 口径：`tb_order` **没有专门的出品时间字段**（实测列清单只有
 * create_time / payment_success_time / order_completion_time / update_time），
 * 这里用 **update_time 近似** —— 已出品单在出品后可被更新的操作极少
 * （出品状态不能再改、改备注接口已禁止已出品单 ⇒ 大多就是出品那次写入），
 * 误差可接受；若要求精确，后续加列 production_time。
 */
function doneTimeText(o: MerchantOrderRow): string {
  if (!parseTime(o.updateTime)) return ''
  return `出品 ${hhmm(o.updateTime)}`
}

function waitedText(o: MerchantOrderRow): string {
  const t = parseTime(o.createTime)
  if (!t) return ''
  const sec = Math.max(0, Math.floor((Date.now() - t) / 1000))
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return h > 0 ? `已等 ${h}h${m}m` : `已等 ${m}m${s}s`
}

/** 卡片点击推进状态：待制作→出品中→已出品（已出品不动） */
async function advance(o: MerchantOrderRow, key: string) {
  const cur = KEY_TO_STATUS[key]
  if (cur >= 3) return
  const next = (cur + 1) as 2 | 3
  busyId.value = o.id
  try {
    await updateProductionStatus(o.id, next)
    await load()
  } catch (e) {
    console.error('[kanban] 推进失败', e)
    alert((e as Error).message || '操作失败')
  } finally {
    busyId.value = null
  }
}

/** 最近完成（画板底部说明口径）：取已出品订单按更新时间倒序前 5 */
const noteText = computed(() => {
  const done = orders.value
    .filter(o => (o.productionStatus ?? 1) === 3)
    .slice(0, 5)
    .map(o => `${o.queueNo ?? o.tableNo ?? '—'} ${hhmm(o.createTime)}`)
  const head = done.length ? `最近出品：${done.join(' ｜ ')}` : '暂无已出品订单'
  return `${head} —— 卡片点击推进状态（待制作 → 出品中 → 已出品）`
})

function toggleFullscreen() {
  const el = document.documentElement
  if (document.fullscreenElement) document.exitFullscreen()
  else el.requestFullscreen?.()
}

function parseTime(v: string | null): number | null {
  if (!v) return null
  const m = String(v).match(/(\d{4})\/(\d{2})\/(\d{2})\s+(\d{2}):(\d{2}):(\d{2})/)
  if (m) return new Date(`${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6]}`).getTime()
  const t = new Date(String(v)).getTime()
  return Number.isNaN(t) ? null : t
}

function hhmm(v: string | null): string {
  const m = String(v ?? '').match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : '—'
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
