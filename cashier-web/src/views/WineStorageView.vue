<template>
  <!--
    W-18 存酒列表与取酒核销（Ardot 2:1162，内容区 2:1165）· 方案 v1.1
    工具条：取酒码(120)+核销 / 手机号(170) / 状态(150) / 查询 / 待核销申请(N)
    表格 1170×300：表头 40 + 行 52；列宽 180/250/110/180/130/120/200 = 1170（原「存酒码」列已删）
    行底色按状态：在存=白 ｜ 临期=$2:25 #FCF5ED ｜ 已过期=$2:8 #FCE3E3
    取酒统一为「申请 → 取酒码 → 核销」：顾客自助 或 店员代发起，核销走同一入口。
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 2:1167 -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="takeCodeInput"
        class="rounded border text-[12px] outline-none"
        style="width: 120px; height: 32px; padding: 0 10px; border-color: var(--tc-line); letter-spacing: 1px"
        maxlength="6"
        placeholder="取酒码"
        @keyup.enter="onCodeConfirm"
      />
      <button
        class="rounded border text-[12px] disabled:opacity-40"
        style="height: 32px; padding: 0 14px; border-color: var(--tc-line); color: var(--p)"
        :disabled="busy || takeCodeInput.length !== 6"
        @click="onCodeConfirm"
      >核销</button>
      <input
        v-model.trim="phone"
        class="rounded border text-[12px] outline-none"
        style="width: 170px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="顾客手机号"
      />
      <select
        v-model="statusFilter"
        class="rounded border text-[12px] outline-none"
        style="width: 130px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
      >
        <option value="">全部状态 ▾</option>
        <option v-for="s in STATUS_OPTIONS" :key="s.code" :value="String(s.code)">{{ s.text }}</option>
      </select>
      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 36px; padding: 0 22px; background: var(--p)"
        @click="load"
      >查询</button>

      <button
        class="rounded border text-[12px] ml-auto"
        style="height: 32px; padding: 0 14px; border-color: var(--p); color: var(--p)"
        @click="openPending"
      >待核销申请（{{ pendingList.length }}）</button>
      <span
        v-if="expiringCount > 0"
        class="rounded text-[12px]"
        style="height: 32px; line-height: 32px; padding: 0 12px; background: #FCF5ED; color: #B8720E"
      >临期提醒：{{ expiringCount }} 条</span>
      <span class="text-[12px]" style="color: var(--tc-sub)">共 {{ filtered.length }} 条</span>
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
          <span class="px-2 truncate" style="width: 180px; color: var(--txt)">
            {{ r.memberName || '—' }} {{ r.memberPhone || '' }}
          </span>
          <span class="px-2 truncate" style="width: 250px; color: var(--txt)">{{ r.goodsName }}</span>
          <span class="px-2 truncate" style="width: 110px; color: var(--txt)">
            {{ r.remainCount }}<template v-if="r.bottleCount && r.remainCount !== r.bottleCount">/{{ r.bottleCount }}</template> 瓶
          </span>
          <span class="px-2 truncate" style="width: 180px; color: var(--txt)">{{ r.location || '—' }}</span>
          <span class="px-2 truncate" style="width: 130px" :style="{ color: expireColor(r) }">{{ r.expireDate }}</span>
          <span class="px-2 truncate" style="width: 120px" :style="{ color: statusColor(r) }">{{ statusLabel(r) }}</span>

          <!-- 操作：代发起取酒（兜底）/ 详情。直接"取酒核销"通道已取消（单一模型）。 -->
          <span class="px-2 flex items-center gap-2" style="width: 200px">
            <template v-if="r.statusCode !== 2 && (r.remainCount ?? 0) > 0">
              <button
                v-if="!r.pendingApply"
                class="text-[12px]"
                :style="{ color: 'var(--p)' }"
                @click="openCreate(r)"
              >代发起取酒</button>
              <span v-else class="text-[12px]" style="color: var(--tc-hold)">已有待核销</span>
            </template>
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
      取酒统一为「申请 → 取酒码 → 核销」：顾客在小程序申请，或店员「代发起取酒」出码；核销走取酒码。
      同一存酒同时最多 1 笔待核销。状态口径与顾客端 C-16 一致（服务端计算下发），状态筛选为客户端筛选。
    </p>

    <!-- ============ 待核销申请弹层 ============ -->
    <Teleport to="body">
      <div
        v-if="pendingOpen"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="pendingOpen = false"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(720px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">待核销取酒申请（{{ pendingList.length }}）</span>
            <button class="text-[13px]" style="color: var(--tc-gray)" @click="pendingOpen = false">关闭</button>
          </div>
          <div class="mt-3 text-[11px]" style="color: var(--tc-hold)">点行 → 确认核销（扣瓶不可逆，两段式确认）</div>
          <div class="mt-3 max-h-[420px] overflow-y-auto" style="border: 1px solid var(--tc-line); border-radius: 8px">
            <div v-if="!pendingList.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">当前没有待核销申请</div>
            <div
              v-for="a in pendingList"
              :key="a.id"
              class="flex items-center gap-2 text-[12px] cursor-pointer hover:bg-black/[.03]"
              style="height: 48px; border-top: 1px solid var(--tc-line)"
              @click="openConfirm(a)"
            >
              <span class="px-2" style="width: 96px; color: var(--p); font-weight: 600">{{ a.takeCode }}</span>
              <span class="px-2 truncate" style="width: 170px; color: var(--txt)">{{ a.memberName || '—' }} {{ a.memberPhone || '' }}</span>
              <span class="px-2 truncate" style="width: 180px; color: var(--txt)">{{ a.goodsName }}</span>
              <span class="px-2" style="width: 90px; color: var(--txt)">{{ a.takeCount }} 瓶</span>
              <span class="px-2 truncate" style="width: 140px; color: var(--tc-sub)">{{ a.applySourceText }}</span>
              <span class="px-2 truncate" style="width: 130px; color: var(--tc-sub)">{{ a.applyTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ============ 核销确认弹窗（两段式第二步） ============ -->
    <Teleport to="body">
      <div
        v-if="previewRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="previewRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(440px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">取酒核销</span>
            <span class="text-[13px] font-bold" style="color: var(--p); letter-spacing: 1px">{{ previewRow.takeCode }}</span>
          </div>
          <div class="mt-4 flex flex-col gap-2 text-[12px]">
            <div class="flex"><span style="color: var(--tc-sub)">顾客</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.memberName || '—' }} {{ previewRow.memberPhone || '' }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">品名</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.goodsName || '—' }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">存放位置</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.location || '—' }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">本次取出</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.takeCount }} 瓶</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">来源</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.applySourceText }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">有效期至</span><span class="ml-auto" style="color: var(--txt)">{{ previewRow.expireTime || '—' }}</span></div>
          </div>
          <p class="mt-3 text-[11px]" style="color: var(--tc-hold)">
            扣减为原子操作（防超扣）；扣到 0 自动置「已取完」并写取酒记录（含申请单号）。
          </p>
          <div class="mt-5 flex justify-end gap-2">
            <button class="rounded border text-[13px]" style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)" @click="previewRow = null">取消</button>
            <button
              class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="busy"
              @click="doConfirm"
            >确认核销</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ============ 代发起取酒（填瓶数） ============ -->
    <Teleport to="body">
      <div
        v-if="createRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="createRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(420px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">代发起取酒</span>
            <span class="text-[12px]" style="color: var(--tc-sub)">{{ createRow.memberName || '' }} {{ createRow.memberPhone || '' }}</span>
          </div>
          <p class="mt-2 text-[12px]" style="color: var(--tc-sub)">
            {{ createRow.goodsName }} · 当前剩余 {{ createRow.remainCount }} 瓶
          </p>
          <div class="mt-4 flex items-center gap-3">
            <span class="text-[12px]" style="color: var(--txt)">取出瓶数</span>
            <input
              v-model.number="createCount"
              type="number"
              :min="1"
              :max="createRow.remainCount ?? 0"
              class="rounded border text-[13px] outline-none"
              style="width: 100px; height: 34px; padding: 0 10px; border-color: var(--tc-line)"
            />
            <span class="text-[12px]" style="color: var(--tc-sub)">瓶（最多 {{ createRow.remainCount }}）</span>
          </div>
          <p class="mt-2 text-[11px]" style="color: var(--tc-hold)">
            生成取酒码后交给顾客出示，或当场核销；24 小时内有效。
          </p>
          <div class="mt-5 flex justify-end gap-2">
            <button class="rounded border text-[13px]" style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)" @click="createRow = null">取消</button>
            <button
              class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="busy || !validCreate"
              @click="doCreate"
            >生成取酒码</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ============ 出码弹窗（代发起成功后） ============ -->
    <Teleport to="body">
      <div
        v-if="createdApply"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="createdApply = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl text-center" style="width: min(420px, calc(100vw - 32px))">
          <div class="text-[16px] font-bold" style="color: var(--tc-title)">取酒码</div>
          <div class="mt-4 text-[40px] font-bold" style="color: var(--p); letter-spacing: 6px">{{ createdApply.takeCode }}</div>
          <p class="mt-3 text-[12px]" style="color: var(--tc-sub)">
            {{ createdApply.goodsName }} · {{ createdApply.takeCount }} 瓶 · 请顾客出示或当场核销
          </p>
          <p class="mt-1 text-[11px]" style="color: var(--tc-hold)">有效期至 {{ createdApply.expireTime }}</p>
          <div class="mt-5 flex justify-center gap-2">
            <button class="rounded border text-[13px]" style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)" @click="createdApply = null">关闭</button>
            <button
              class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="busy"
              @click="confirmCreated"
            >当场核销</button>
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
            <span class="text-[12px]" style="color: var(--txt)">{{ detailRow.goodsName || '' }}</span>
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
  fetchApplyList,
  previewTake,
  confirmTake,
  createApplyByShop,
  type WineStorageRow,
  type WineTakeApplyRow
} from '@/api/wine'

const COLS = [
  { key: 'member', label: '顾客', w: 180 },
  { key: 'goods', label: '品名', w: 250 },
  { key: 'remain', label: '剩余瓶数', w: 110 },
  { key: 'location', label: '存放位置', w: 180 },
  { key: 'expire', label: '有效期', w: 130 },
  { key: 'status', label: '状态', w: 120 },
  { key: 'op', label: '操作', w: 200 }
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
const pendingList = ref<WineTakeApplyRow[]>([])
const loading = ref(true)
const page = ref(1)
const phone = ref('')
const statusFilter = ref('')
const busy = ref(false)

const takeCodeInput = ref('')
const previewRow = ref<WineTakeApplyRow | null>(null)
const pendingOpen = ref(false)
const createRow = ref<WineStorageRow | null>(null)
const createCount = ref(1)
const createdApply = ref<WineTakeApplyRow | null>(null)
const detailRow = ref<WineStorageRow | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    // 后端 list 支持 手机号（已核实现）；状态在本地筛
    const res = await fetchWineStorageList({ memberPhone: phone.value || undefined })
    rows.value = Array.isArray(res) ? res : []
    page.value = 1
    await loadPending()
  } catch (e) {
    console.error('[wine] 加载失败', e)
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  try {
    const res = await fetchApplyList()
    pendingList.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('[wine] 待核销加载失败', e)
    pendingList.value = []
  }
}

const filtered = computed(() =>
  statusFilter.value ? rows.value.filter(r => String(r.statusCode) === statusFilter.value) : rows.value
)

/** 临期提醒：将过期（状态码 4）条数 */
const expiringCount = computed(() => rows.value.filter(r => r.statusCode === 4).length)

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

/** 状态文案加剩余天数（画板口径「临期（剩 5 天）」「已过期（14 天）」） */
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

/* ---------- 取酒码核销（两段式：预览 → 确认） ---------- */
async function onCodeConfirm() {
  const code = takeCodeInput.value.trim()
  if (code.length !== 6) return
  busy.value = true
  try {
    previewRow.value = await previewTake(code)
  } catch (e) {
    console.error('[wine] 取酒码预览失败', e)
    alert((e as Error).message || '取酒码无效')
  } finally {
    busy.value = false
  }
}

async function openPending() {
  await loadPending()
  pendingOpen.value = true
}

function openConfirm(a: WineTakeApplyRow) {
  pendingOpen.value = false
  previewRow.value = a
}

async function doConfirm() {
  const a = previewRow.value
  if (!a || !a.takeCode) return
  busy.value = true
  try {
    await confirmTake(a.takeCode)
    previewRow.value = null
    takeCodeInput.value = ''
    await load()
  } catch (e) {
    console.error('[wine] 取酒核销失败', e)
    alert((e as Error).message || '取酒核销失败')
  } finally {
    busy.value = false
  }
}

/* ---------- 代发起取酒 ---------- */
const validCreate = computed(() => {
  const r = createRow.value
  if (!r) return false
  const n = Number(createCount.value)
  return Number.isInteger(n) && n >= 1 && n <= (r.remainCount ?? 0)
})

function openCreate(r: WineStorageRow) {
  createRow.value = r
  createCount.value = 1
}

async function doCreate() {
  const r = createRow.value
  if (!r || !validCreate.value) return
  busy.value = true
  try {
    createdApply.value = await createApplyByShop({ storageId: r.id, takeCount: Number(createCount.value) })
    createRow.value = null
    await load()
  } catch (e) {
    console.error('[wine] 代发起取酒失败', e)
    alert((e as Error).message || '代发起取酒失败')
  } finally {
    busy.value = false
  }
}

async function confirmCreated() {
  const a = createdApply.value
  if (!a || !a.takeCode) return
  busy.value = true
  try {
    await confirmTake(a.takeCode)
    createdApply.value = null
    await load()
  } catch (e) {
    console.error('[wine] 当场核销失败', e)
    alert((e as Error).message || '核销失败')
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
