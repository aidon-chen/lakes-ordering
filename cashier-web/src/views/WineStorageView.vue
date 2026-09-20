<template>
  <!--
    W-18 存酒列表与取酒核销（Ardot 2:1162，内容区 2:1165）
    工具条 40（存酒码190 / 手机号170 / 状态150 / 查询88×36）
    表格 1170×300：表头 40 + 行 52；列宽 170/140/230/110/150/130/100/140 = 1170
    分页条 32
    行底色按状态：在存=白 ｜ 临期=$2:25 #FCF5ED ｜ 已过期=$2:8 #FCE3E3
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 2:1167 -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="code"
        class="rounded border text-[12px] outline-none"
        style="width: 190px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="存酒码"
      />
      <input
        v-model.trim="phone"
        class="rounded border text-[12px] outline-none"
        style="width: 170px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="顾客手机号"
      />
      <select
        v-model="statusFilter"
        class="rounded border text-[12px] outline-none"
        style="width: 150px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
      >
        <option value="">全部状态 ▾</option>
        <option v-for="s in STATUS_OPTIONS" :key="s.code" :value="String(s.code)">{{ s.text }}</option>
      </select>
      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 36px; padding: 0 22px; background: var(--p)"
        @click="load"
      >查询</button>
      <span class="ml-auto text-[12px]" style="color: var(--tc-sub)">共 {{ filtered.length }} 条</span>
    </div>

    <!-- 表格 2:1175 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!paged.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">没有符合条件的存酒记录</div>

        <div
          v-for="r in paged"
          v-else
          :key="r.id"
          class="flex items-center text-[12px]"
          :style="{ height: '52px', borderTop: '1px solid var(--tc-line)', background: rowBg(r) }"
        >
          <span class="px-2 truncate" style="width: 170px; color: var(--p)">{{ r.storageCode }}</span>
          <span class="px-2 truncate" style="width: 140px; color: var(--txt)">
            {{ r.memberName || '—' }} {{ r.memberPhone || '' }}
          </span>
          <span class="px-2 truncate" style="width: 230px; color: var(--txt)">{{ r.goodsName }}</span>
          <span class="px-2 truncate" style="width: 110px; color: var(--txt)">
            {{ r.remainCount }}<template v-if="r.bottleCount && r.remainCount !== r.bottleCount">/{{ r.bottleCount }}</template> 瓶
          </span>
          <span class="px-2 truncate" style="width: 150px; color: var(--txt)">{{ r.location || '—' }}</span>
          <span class="px-2 truncate" style="width: 130px" :style="{ color: expireColor(r) }">{{ r.expireDate }}</span>
          <span class="px-2 truncate" style="width: 100px" :style="{ color: statusColor(r) }">{{ statusLabel(r) }}</span>

          <!-- 操作：只放有接口的（取酒核销 / 详情）；转赠与销毁登记工程无接口，见页脚说明 -->
          <span class="px-2 flex items-center gap-2" style="width: 140px">
            <button
              v-if="r.statusCode !== 2 && (r.remainCount ?? 0) > 0"
              class="text-[12px]"
              :style="{ color: 'var(--p)' }"
              @click="openTake(r)"
            >取酒核销</button>
            <button class="text-[12px]" style="color: var(--p)" @click="openDetail(r)">详情</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 分页条 2:1212 -->
    <div class="shrink-0 flex items-center gap-2 text-[12px]" style="height: 32px">
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
      状态口径与顾客端 C-16 完全一致（存续中 / 将过期 / 已过期 / 已取完，服务端计算后下发）。
      状态筛选为客户端筛选（后端 list 只支持 存酒码 / 手机号）。转赠与销毁登记工程暂无接口，故不放按钮。
    </p>

    <!-- ============ 取酒核销弹窗 ============ -->
    <Teleport to="body">
      <div
        v-if="takeRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="takeRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(420px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">取酒核销</span>
            <span class="text-[12px]" style="color: var(--tc-sub)">{{ takeRow.storageCode }}</span>
          </div>
          <p class="mt-2 text-[12px]" style="color: var(--tc-sub)">
            {{ takeRow.goodsName }} · 当前剩余 {{ takeRow.remainCount }} 瓶
          </p>
          <div class="mt-4 flex items-center gap-3">
            <span class="text-[12px]" style="color: var(--txt)">本次取出</span>
            <input
              v-model.number="takeCount"
              type="number"
              :min="1"
              :max="takeRow.remainCount ?? 0"
              class="rounded border text-[13px] outline-none"
              style="width: 100px; height: 34px; padding: 0 10px; border-color: var(--tc-line)"
            />
            <span class="text-[12px]" style="color: var(--tc-sub)">瓶（最多 {{ takeRow.remainCount }}）</span>
          </div>
          <p class="mt-2 text-[11px]" style="color: var(--tc-hold)">
            扣减为原子操作（防超扣）；扣到 0 自动置「已取完」并写取酒记录
          </p>
          <div class="mt-5 flex justify-end gap-2">
            <button
              class="rounded border text-[13px]"
              style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
              @click="takeRow = null"
            >取消</button>
            <button
              class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="busy || !validTake"
              @click="doTake"
            >确认核销</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ============ 详情弹窗（只读，数据全在列表行里） ============ -->
    <Teleport to="body">
      <div
        v-if="detailRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="detailRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(460px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">存酒详情</span>
            <span class="text-[12px]" style="color: var(--p)">{{ detailRow.storageCode }}</span>
          </div>
          <div class="mt-4 flex flex-col gap-2 text-[12px]">
            <div v-for="kv in detailPairs" :key="kv[0]" class="flex">
              <span style="color: var(--tc-sub)">{{ kv[0] }}</span>
              <span class="ml-auto" style="color: var(--txt)">{{ kv[1] }}</span>
            </div>
          </div>
          <div class="mt-5 flex justify-end">
            <button
              class="rounded border text-[13px]"
              style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
              @click="detailRow = null"
            >关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  fetchWineStorageList,
  takeWine,
  type WineStorageRow
} from '@/api/wine'

const COLS = [
  { key: 'code', label: '存酒码', w: 170 },
  { key: 'member', label: '顾客', w: 140 },
  { key: 'goods', label: '品名', w: 230 },
  { key: 'remain', label: '剩余瓶数', w: 110 },
  { key: 'location', label: '存放位置', w: 150 },
  { key: 'expire', label: '有效期', w: 130 },
  { key: 'status', label: '状态', w: 100 },
  { key: 'op', label: '操作', w: 140 }
] as const

/** 状态码口径来自后端 WineStorageVo：1 存续中 / 2 已取完 / 3 已过期 / 4 将过期 */
const STATUS_OPTIONS = [
  { code: 1, text: '在存' },
  { code: 4, text: '临期' },
  { code: 3, text: '已过期' },
  { code: 2, text: '已取完' }
] as const

const PAGE_SIZE = 10

const rows = ref<WineStorageRow[]>([])
const loading = ref(true)
const page = ref(1)
const code = ref('')
const phone = ref('')
const statusFilter = ref('')
const busy = ref(false)

const takeRow = ref<WineStorageRow | null>(null)
const takeCount = ref(1)
const detailRow = ref<WineStorageRow | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    // 后端 list 支持 存酒码 / 手机号（已核实现）；状态在本地筛
    const res = await fetchWineStorageList({ storageCode: code.value || undefined, memberPhone: phone.value || undefined })
    rows.value = Array.isArray(res) ? res : []
    page.value = 1
  } catch (e) {
    console.error('[wine] 加载失败', e)
  } finally {
    loading.value = false
  }
}

const filtered = computed(() =>
  statusFilter.value ? rows.value.filter(r => String(r.statusCode) === statusFilter.value) : rows.value
)

const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))
const paged = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

watch(statusFilter, () => { page.value = 1 })

/** 行底色：临期=警示浅底 $2:25，已过期=危险浅底 $2:8 */
function rowBg(r: WineStorageRow): string {
  if (r.statusCode === 3) return '#FCE3E3'
  if (r.statusCode === 4) return '#FCF5ED'
  return '#FFFFFF'
}

function statusColor(r: WineStorageRow): string {
  if (r.statusCode === 3) return 'var(--warn)'
  if (r.statusCode === 4) return '#E5A33D'
  return 'var(--p)'
}

function expireColor(r: WineStorageRow): string {
  if (r.statusCode === 3) return 'var(--warn)'
  if (r.statusCode === 4) return '#E5A33D'
  return 'var(--txt)'
}

/**
 * 状态文案加剩余天数（画板口径「临期（剩 5 天）」「已过期（14 天）」）
 * 后端只给 statusText（存续中/将过期/已过期/已取完），天数由 expireDate 本地算
 */
function statusLabel(r: WineStorageRow): string {
  const base = r.statusCode === 4 ? '临期' : r.statusText || '—'
  const days = daysFromToday(r.expireDate)
  if (days == null) return base
  if (r.statusCode === 4) return `${base}（剩 ${days} 天）`
  if (r.statusCode === 3) return `${base}（${Math.abs(days)} 天）`
  return base
}

function daysFromToday(d: string | null): number | null {
  const m = String(d ?? '').match(/(\d{4})-(\d{2})-(\d{2})/)
  if (!m) return null
  const t = new Date(`${m[1]}-${m[2]}-${m[3]}T00:00:00`).getTime()
  if (Number.isNaN(t)) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((t - today.getTime()) / 86400000)
}

const validTake = computed(() => {
  const r = takeRow.value
  if (!r) return false
  const n = Number(takeCount.value)
  return Number.isInteger(n) && n >= 1 && n <= (r.remainCount ?? 0)
})

function openTake(r: WineStorageRow) {
  takeRow.value = r
  takeCount.value = 1
}

async function doTake() {
  const r = takeRow.value
  if (!r || !validTake.value) return
  busy.value = true
  try {
    await takeWine(r.storageCode, Number(takeCount.value))
    takeRow.value = null
    await load()
  } catch (e) {
    console.error('[wine] 取酒失败', e)
    alert((e as Error).message || '取酒失败')
  } finally {
    busy.value = false
  }
}

function openDetail(r: WineStorageRow) {
  detailRow.value = r
}

const detailPairs = computed(() => {
  const r = detailRow.value
  if (!r) return [] as [string, string][]
  return [
    ['顾客', `${r.memberName || '—'} ${r.memberPhone || ''}`],
    ['品名', r.goodsName || '—'],
    ['瓶数', `${r.remainCount ?? 0} / ${r.bottleCount ?? 0} 瓶`],
    ['存放位置', r.location || '—'],
    ['存入日期', r.storageDate || '—'],
    ['有效期至', r.expireDate || '—'],
    ['状态', statusLabel(r)],
    ['所属门店', r.shopName || '—']
  ]
})
</script>
