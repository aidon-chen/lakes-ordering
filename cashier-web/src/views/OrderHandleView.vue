<template>
  <!--
    W-11 订单处理（Ardot 2:796，内容区 2:799）
    标签页 620×40（gap 28）｜ 工具条 40（搜索220 + 空隙 + 轮询提示）｜ 表格 1170×300（表头40 + 行52）
    列宽 110/320/120/130/180/110/(操作 flex)
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 标签页 2:801（标签名取自组件库 C-标签页 2:203） -->
    <div class="shrink-0 flex items-center" style="height: 40px; gap: 28px">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="relative h-full text-[14px]"
        :style="activeTab === t.key ? 'color: var(--p)' : 'color: var(--txt)'"
        @click="activeTab = t.key"
      >
        {{ t.label }}<template v-if="t.key !== 'all' && t.key !== 'done'">({{ t.count }})</template>
        <i
          v-if="activeTab === t.key"
          class="absolute left-0 right-0 bottom-0 mx-auto block"
          style="height: 3px; width: 56px; background: var(--p)"
        />
      </button>
    </div>

    <!-- 工具条 2:802 -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="keyword"
        class="rounded border text-[12px] outline-none"
        style="width: 220px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="取餐号 / 手机号"
      />
      <span class="flex-1" />
      <span class="text-[12px]" style="color: var(--tc-sub)">30 秒自动刷新 · 新订单语音提醒：关</span>
    </div>

    <!-- 表格 2:807 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w ? c.w + 'px' : undefined, flex: c.w ? undefined : 1 }">{{ c.label }}</span>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!filtered.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">该标签下暂无订单</div>

        <div
          v-for="o in filtered"
          v-else
          :key="o.id"
          class="flex items-center text-[12px]"
          style="height: 52px; border-top: 1px solid var(--tc-line)"
        >
          <span class="px-2 truncate" style="width: 110px; color: var(--p)">{{ o.queueNo ?? o.tableNo ?? '—' }}</span>
          <span class="px-2 truncate" style="width: 320px; color: var(--txt)">{{ descText(o) }}</span>
          <span class="px-2 truncate" style="width: 120px; color: var(--txt)">¥{{ money(o.actualPrice) }}</span>
          <span class="px-2 truncate" style="width: 130px; color: var(--txt)">{{ o.tableName || o.tableNo || '—' }}</span>
          <span class="px-2 truncate" style="width: 180px; color: var(--tc-sub)">{{ hhmm(o.createTime) }}</span>
          <span class="px-2 truncate" style="width: 110px" :style="{ color: waitedColor(o) }">{{ waitedText(o) }}</span>

          <span class="px-2 flex items-center gap-3 min-w-0" style="flex: 1">
            <!-- 待处理(2) → 接单（动作码 1：2→3） -->
            <button
              v-if="o.status === 2"
              class="rounded text-[12px] text-white disabled:opacity-50"
              style="height: 28px; padding: 0 14px; background: var(--p)"
              :disabled="busyId === o.id"
              @click="doAccept(o)"
            >接单</button>
            <!-- 待自取(3) → 标记完成（动作码 2：3→6） -->
            <button
              v-else-if="o.status === 3"
              class="rounded text-[12px] text-white disabled:opacity-50"
              style="height: 28px; padding: 0 14px; background: var(--p)"
              :disabled="busyId === o.id"
              @click="doFinish(o)"
            >完成</button>
            <!-- 出品中 → 出品完成（批次D 新增字段驱动） -->
            <button
              v-if="o.productionStatus === 2"
              class="rounded border text-[12px] disabled:opacity-50"
              style="height: 28px; padding: 0 10px; border-color: var(--tc-line); color: var(--p)"
              :disabled="busyId === o.id"
              @click="doProductionDone(o)"
            >出品完成</button>
            <!-- 详情（W-12 已落地） -->
            <button class="text-[12px]" style="color: var(--p)" @click="goDetail(o.id)">详情</button>
            <!-- 出品状态只读展示（非「出品中」时不显示按钮，避免无意义按钮） -->
            <span class="truncate text-[11px]" style="color: var(--tc-hold)">
              {{ otherActionText(o) }}
            </span>
          </span>
        </div>
      </div>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      标签计数为**本地自算**（不使用后端 selectAllTabWaitHandleNum —— 它会把堂食算进「自取待处理」，见计划 §6.3）。
      「待自提核销」标签未实现：其数据在 tb_points_mall_order（另一张表），属批次 E 范围。
      30 秒轮询本期未开启（避免无谓后端压力），提示文案已如实标为「关」。
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchMerchantOrders,
  updateOrderStatus,
  updateProductionStatus,
  type MerchantOrderRow
} from '@/api/order'

const COLS = [
  { key: 'queueNo', label: '取餐号', w: 110 },
  { key: 'desc', label: '订单描述', w: 320 },
  { key: 'actual', label: '实付款', w: 120 },
  { key: 'table', label: '台位', w: 130 },
  { key: 'time', label: '下单时间', w: 180 },
  { key: 'waited', label: '已等待', w: 110 },
  { key: 'op', label: '操作', w: 0 } // flex
] as const

/**
 * 标签页：名称取自画板组件库（C-标签页 2:203）
 * ⚠️ 只实现**有数据源**的 4 个：
 *   - 待接单 ⟵ order.status=2
 *   - 出品中 ⟵ order.production_status=2（批次D 新增列）
 *   - 已完成 ⟵ order.status=6
 *   - 全部订单 ⟵ 全部
 * 「待自提核销」不实现：数据在 tb_points_mall_order，属批次 E（并行会话已实现其接口）
 */
type TabKey = 'pending' | 'producing' | 'done' | 'all'

const router = useRouter()
const rows = ref<MerchantOrderRow[]>([])
const loading = ref(true)
const activeTab = ref<TabKey>('pending')
const keyword = ref('')
const busyId = ref<number | null>(null)

/** 一次多取，标签计数与搜索均在本地算（后端无对应参数） */
const FETCH_SIZE = 200

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await fetchMerchantOrders({ pageNo: 1, pageSize: FETCH_SIZE })
    rows.value = res?.records ?? []
  } catch (e) {
    console.error('[order-handle] 加载失败', e)
  } finally {
    loading.value = false
  }
}

/** 标签计数：本地自算（见模板底部说明） */
const counts = computed(() => ({
  pending: rows.value.filter(o => o.status === 2).length,
  producing: rows.value.filter(o => o.productionStatus === 2).length, // 只有明确「出品中」才计入
  done: rows.value.filter(o => o.status === 6).length,
  all: rows.value.length
}))

const tabs = computed(() => [
  { key: 'pending' as TabKey, label: '待接单', count: counts.value.pending },
  { key: 'producing' as TabKey, label: '出品中', count: counts.value.producing },
  { key: 'done' as TabKey, label: '已完成', count: counts.value.done },
  { key: 'all' as TabKey, label: '全部订单', count: counts.value.all }
])

const filtered = computed(() => {
  const kw = keyword.value.toLowerCase()
  return rows.value.filter(o => {
    if (activeTab.value === 'pending' && o.status !== 2) return false
    if (activeTab.value === 'producing' && o.productionStatus !== 2) return false
    if (activeTab.value === 'done' && o.status !== 6) return false
    if (kw) {
      const hay = `${o.queueNo ?? ''}${o.tableNo ?? ''}${o.contactPhone ?? ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})

async function doAccept(o: MerchantOrderRow) {
  await run(o, () => updateOrderStatus(o.id, 1))
}
async function doFinish(o: MerchantOrderRow) {
  await run(o, () => updateOrderStatus(o.id, 2))
}
async function doProductionDone(o: MerchantOrderRow) {
  await run(o, () => updateProductionStatus(o.id, 3))
}

/** 统一：置忙 → 调接口 → 重载（失败如实提示，不乐观改本地态） */
async function run(o: MerchantOrderRow, fn: () => Promise<unknown>) {
  busyId.value = o.id
  try {
    await fn()
    await load()
  } catch (e) {
    console.error('[order-handle] 操作失败', e)
    alert((e as Error).message || '操作失败')
  } finally {
    busyId.value = null
  }
}

/** 已等待 = 下单至今（对齐画板 1h05m / 0h52m 口径） */
function waitedText(o: MerchantOrderRow): string {
  const t = parseTime(o.createTime)
  if (!t) return '—'
  const mins = Math.max(0, Math.floor((Date.now() - t) / 60000))
  const h = Math.floor(mins / 60)
  return h > 0 ? `${h}h${String(mins % 60).padStart(2, '0')}m` : `0h${String(mins).padStart(2, '0')}m`
}

/** 超 30 分钟未接单标警示色（无画板依据的纯展示增强，故仅在待接单标签下生效） */
function waitedColor(o: MerchantOrderRow): string {
  if (activeTab.value !== 'pending') return 'var(--txt)'
  const t = parseTime(o.createTime)
  if (!t) return 'var(--txt)'
  return Date.now() - t > 30 * 60000 ? 'var(--warn)' : 'var(--txt)'
}

/** 进订单详情（W-12） */
function goDetail(id: number) {
  router.push({ path: '/orders/detail', query: { id } })
}

function otherActionText(o: MerchantOrderRow): string {
  if (o.status === 2 || o.status === 3 || o.productionStatus === 2) return ''
  return o.productionStatus ? `出品：${['', '待制作', '出品中', '已出品'][o.productionStatus] ?? '—'}` : ''
}

/** 后端返回 "2026/09/17 20:15:00" */
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

function descText(o: MerchantOrderRow): string {
  const n = o.goodsTotalQuantity ?? 0
  const d = decodeEntities(o.description ?? '').trim()
  if (!d) return n ? `${n} 件` : '—'
  return /件/.test(d) || !n ? d : `${d} · ${n} 件`
}

function decodeEntities(s: string): string {
  return s
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ')
}

function money(v: number | null): string {
  return Number(v || 0).toFixed(2)
}
</script>
