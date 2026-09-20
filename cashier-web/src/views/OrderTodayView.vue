<template>
  <!--
    W-10 今日订单（Ardot 2:734，内容区 2:737）
    工具条 40（购物方式150 / 取餐号150 / 联系电话150 / 日期240 / 查询88×36）
    表格 1170×330（表头 40 + 行 52），列宽 110/300/110/110/110/110/180/140 = 1170
    分页条 32
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 2:739 -->
    <div class="shrink-0 flex items-center gap-3">
      <select
        v-model="shoppingWay"
        class="rounded border text-[12px] outline-none"
        style="width: 150px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
      >
        <option :value="null">堂食 / 自提</option>
        <option :value="1">自取</option>
      </select>

      <input
        v-model.trim="queueKeyword"
        class="rounded border text-[12px] outline-none"
        style="width: 150px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="取餐号"
        title="后端无该筛选参数，按已加载数据筛选"
      />
      <input
        v-model.trim="phone"
        class="rounded border text-[12px] outline-none"
        style="width: 150px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="联系电话"
      />

      <div
        class="flex items-center gap-2 rounded border"
        style="width: 240px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
      >
        <input v-model="dateFrom" type="date" class="text-[12px] outline-none flex-1 min-w-0" style="color: var(--tc-gray)" />
        <span class="text-[12px]" style="color: var(--tc-hold)">至</span>
        <input v-model="dateTo" type="date" class="text-[12px] outline-none flex-1 min-w-0" style="color: var(--tc-gray)" />
      </div>

      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 36px; padding: 0 22px; background: var(--p)"
        @click="reload"
      >查询</button>
    </div>

    <!-- 表格 2:749 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <!-- 表头 2:750 -->
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!paged.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">没有符合条件的订单</div>

        <div
          v-for="o in paged"
          v-else
          :key="o.id"
          class="flex items-center text-[12px]"
          style="height: 52px; border-top: 1px solid var(--tc-line)"
        >
          <!-- 取餐号（堂食单=取餐号；历史单为空则回落台位号） -->
          <span class="px-2 truncate" style="width: 110px; color: var(--p)">{{ o.queueNo ?? o.tableNo ?? '—' }}</span>
          <!-- 订单描述 + 件数 -->
          <span class="px-2 truncate" style="width: 300px; color: var(--txt)">
            {{ descText(o) }}
          </span>
          <!-- 实付款 -->
          <span class="px-2 truncate" style="width: 110px; color: var(--txt)">¥{{ money(o.actualPrice) }}</span>
          <!-- 支付方式 -->
          <span class="px-2 truncate" style="width: 110px" :style="{ color: o.paymentMode ? 'var(--txt)' : 'var(--tc-hold)' }">
            {{ paymentModeText(o.paymentMode) }}
          </span>
          <!-- 台位 -->
          <span class="px-2 truncate" style="width: 110px; color: var(--txt)">{{ o.tableName || o.tableNo || '—' }}</span>
          <!-- 状态（颜色按语义：待处理=警示 / 已完成=绿 / 取消=灰） -->
          <span class="px-2 truncate font-medium" style="width: 110px" :style="{ color: statusColor(o.status) }">
            {{ orderStatusText(o.status) }}
          </span>
          <!-- 下单时间 -->
          <span class="px-2 truncate" style="width: 180px; color: var(--tc-sub)">{{ fmtTime(o.createTime) }}</span>
          <!-- 操作：详情（W-12 已落地；改单/退单/补打待实现，不放死按钮） -->
          <span class="px-2 flex items-center" style="width: 140px">
            <button
              class="text-[12px]"
              style="color: var(--p)"
              @click="goDetail(o.id)"
            >详情</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 分页条 2:795 -->
    <div class="shrink-0 flex items-center gap-2 text-[12px]" style="height: 32px">
      <span style="color: var(--tc-sub)">共 {{ filtered.length }} 条</span>
      <span class="flex-1" />
      <button
        class="rounded border disabled:opacity-40"
        style="height: 28px; padding: 0 12px; border-color: var(--tc-line); color: var(--tc-gray)"
        :disabled="page <= 1"
        @click="page--"
      >上一页</button>
      <span style="color: var(--tc-gray)">{{ page }} / {{ pageCount }}</span>
      <button
        class="rounded border disabled:opacity-40"
        style="height: 28px; padding: 0 12px; border-color: var(--tc-line); color: var(--tc-gray)"
        :disabled="page >= pageCount"
        @click="page++"
      >下一页</button>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      操作列仅「详情」（W-12 已落地）；改单/退单/补打待实现。取餐号 / 日期为客户端筛选（后端暂无这两个查询参数）。
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchMerchantOrders,
  orderStatusText,
  paymentModeText,
  type MerchantOrderRow
} from '@/api/order'

/** 列宽严格对齐画板 2:750~2:758（合计 1170） */
const COLS = [
  { key: 'queueNo', label: '取餐号', w: 110 },
  { key: 'desc', label: '订单描述', w: 300 },
  { key: 'actual', label: '实付款', w: 110 },
  { key: 'payment', label: '支付方式', w: 110 },
  { key: 'table', label: '台位', w: 110 },
  { key: 'status', label: '状态', w: 110 },
  { key: 'time', label: '下单时间', w: 180 },
  { key: 'op', label: '操作', w: 140 }
] as const

const PAGE_SIZE = 10
/** 取餐号/日期为客户端筛选，故一次多取（后端无对应参数） */
const FETCH_SIZE = 200

const router = useRouter()
const rows = ref<MerchantOrderRow[]>([])
const loading = ref(true)
const page = ref(1)

const shoppingWay = ref<number | null>(null)
const phone = ref('')
const queueKeyword = ref('')
const dateFrom = ref('')
const dateTo = ref('')

onMounted(load)

// 服务端支持的筛选（shoppingWay / contactPhone）变化即重新拉取
watch([shoppingWay, phone], () => reload())

async function load() {
  loading.value = true
  try {
    const res = await fetchMerchantOrders({
      pageNo: 1,
      pageSize: FETCH_SIZE,
      shoppingWay: shoppingWay.value ?? undefined,
      contactPhone: phone.value || undefined
    })
    rows.value = res?.records ?? []
    page.value = 1
  } catch (e) {
    console.error('[orders] 加载失败', e)
  } finally {
    loading.value = false
  }
}

function reload() {
  load()
}

/** 客户端筛选：取餐号 + 下单日期区间 */
const filtered = computed(() =>
  rows.value.filter(o => {
    if (queueKeyword.value && !String(o.queueNo ?? o.tableNo ?? '').includes(queueKeyword.value)) {
      return false
    }
    const day = (o.createTime ?? '').slice(0, 10).replace(/\//g, '-')
    if (dateFrom.value && day && day < dateFrom.value) return false
    if (dateTo.value && day && day > dateTo.value) return false
    return true
  })
)

const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))
const paged = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

// 筛选变化后回到第一页，避免停在空页
watch([queueKeyword, dateFrom, dateTo], () => {
  page.value = 1
})

/**
 * 订单描述。
 * ⚠️ 两个实测坑：
 *  ① 后端 description 里带 **HTML 实体**（如「布丁奶酒特调&nbsp;&nbsp;等2件」），
 *     而 Vue 的 `{{ }}` 会转义 → 不处理就会在页面上原样看到 `&nbsp;`。
 *     只做实体解码，**不使用 v-html**（避免 XSS）。
 *  ② description 本身可能已含件数（如「…等2件」）→ 此时不再追加「· N 件」，避免「等2件 · 2 件」这类重复。
 */
function descText(o: MerchantOrderRow): string {
  const n = o.goodsTotalQuantity ?? 0
  const d = decodeEntities(o.description ?? '').trim()
  if (!d) return n ? `${n} 件` : '—'
  // 描述里已表达件数就原样展示；否则补上件数（对齐画板「…等 3 件」的信息量）
  return /件/.test(d) || !n ? d : `${d} · ${n} 件`
}

/** 仅解码常见实体 + 折叠连续空白；不引入任何 HTML 解析 */
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

/** 状态色：2=待处理用警示色、6=已完成为绿、10/11 取消为灰 */
function statusColor(status: number): string {
  switch (status) {
    case 2: return 'var(--warn)'
    case 6: return 'var(--p)'
    case 10:
    case 11: return 'var(--tc-hold)'
    default: return 'var(--txt)'
  }
}

/** 后端返回 "2026/09/17 20:15:00"，统一成画板口径 "2026-09-17 20:15" */
function fmtTime(v: string | null): string {
  if (!v) return '—'
  const m = String(v).match(/(\d{4})\/(\d{2})\/(\d{2})\s+(\d{2}):(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}` : String(v)
}

/** 进订单详情（W-12） */
function goDetail(id: number) {
  router.push({ path: '/orders/detail', query: { id } })
}

function money(v: number | null): string {
  return Number(v || 0).toFixed(2)
}
</script>
