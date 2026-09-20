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
        出品进度看板 · 声呐 SONAR（M-04 出品进度跟踪）
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

          <button
            v-for="it in col.items"
            :key="it.order.id"
            class="text-left rounded-md disabled:opacity-60"
            :style="cardStyle(col.key)"
            :disabled="busyId === it.order.id"
            :title="col.nextLabel"
            @click="advance(it.order, col.key)"
          >
            <div class="flex items-baseline">
              <span class="text-[18px] font-medium" style="color: #404A21">{{ headText(it) }}</span>
              <span class="ml-auto text-[12px]" style="color: #8F9499">{{ waitedText(it.order) }}</span>
            </div>
            <span class="text-[12px] leading-snug line-clamp-2" style="color: #616166">{{ itemsText(it) }}</span>
          </button>
        </div>
      </section>
    </div>

    <!-- 底部说明 2:1026 -->
    <p class="shrink-0 text-[12px]" style="color: #C9CCBF">{{ noteText }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchMerchantOrders,
  fetchOrderDetails,
  updateProductionStatus,
  type MerchantOrderRow,
  type OrderDetailRow
} from '@/api/order'
import { fetchTableList } from '@/api/table'

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
    const needDetail = orders.value.filter(o => o.productionStatus === 1 || o.productionStatus === 2)
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
  const pick = (s: number) => orders.value.filter(o => o.productionStatus === s).map(o => item(o))
  return [
    { key: 'todo', title: `待制作（${pick(1).length}）`, items: pick(1), nextLabel: '点击开始出品' },
    { key: 'doing', title: `出品中（${pick(2).length}）`, items: pick(2), nextLabel: '点击标记已出品' },
    { key: 'done', title: `已出品（${pick(3).length} 今日）`, items: pick(3), nextLabel: '已出品' }
  ]
})

function item(o: MerchantOrderRow): KanbanItem {
  return { order: o, details: detailMap.value[o.id] ?? [] }
}

function cardStyle(key: string) {
  const bg = key === 'done' ? '#EDF2DE' : '#FFFFFF'
  return `background: ${bg}; padding: 12px; min-height: 96px; display: flex; flex-direction: column; gap: 6px`
}

/** 头行：「A03 · 散台」（取餐号 + 台位类型，类型来自 diningTable/list） */
function headText(it: KanbanItem): string {
  const o = it.order
  const no = o.queueNo ?? o.tableNo ?? '—'
  const type = it.order.tableNo ? typeByTableNo.value[it.order.tableNo] : ''
  return type ? `${no} · ${type}` : String(no)
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
    .filter(o => o.productionStatus === 3)
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
