<template>
  <!--
    W-16 核销记录（Ardot 2:1060，内容区 2:1063）
    面包屑 40 ｜ 工具条 40（自提码200 / 核销人140 / 日期240 / 查询88×36 / 空隙 / 导出88×32）｜ 表格 1170×280（表头 40 + 行 44）
    ⚠️ 与画板的一处差异：**去掉「核销方式」列** —— 工程无该字段（未持久化），
      故保留 6 列并按 1170 重新分配：自提码180 / 订单摘要410 / 金额120 / 核销人130 / 核销时间180 / 操作150
    ⚠️ 「查看订单」→ 改为只读「详情」弹窗：收银端**没有商城订单详情页**，不放跳不过去的链接
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 2:1065 -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="qCode"
        class="rounded border text-[12px] outline-none"
        style="width: 200px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="自提码"
        @keyup.enter="load"
      />
      <select
        v-model="qVerifier"
        class="rounded border text-[12px] outline-none"
        style="width: 140px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
      >
        <option value="">全部核销人 ▾</option>
        <option v-for="v in verifierOptions" :key="v.id" :value="String(v.id)">{{ v.name }}</option>
      </select>
      <!-- 日期区间（画板为一个 240×32 的区间控件；用两个 date 输入实现，语义相同） -->
      <div class="flex items-center gap-1" style="width: 260px">
        <input
          v-model="qDateFrom"
          type="date"
          class="rounded border text-[12px] outline-none flex-1"
          style="height: 32px; padding: 0 6px; border-color: var(--tc-line)"
        />
        <span class="text-[12px]" style="color: var(--tc-sub)">至</span>
        <input
          v-model="qDateTo"
          type="date"
          class="rounded border text-[12px] outline-none flex-1"
          style="height: 32px; padding: 0 6px; border-color: var(--tc-line)"
        />
      </div>
      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 36px; padding: 0 22px; background: var(--p)"
        @click="load"
      >查询</button>
      <span class="flex-1" />
      <button
        class="rounded border text-[12px] disabled:opacity-40"
        style="height: 32px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
        :disabled="!rows.length"
        @click="exportCsv"
      >导出</button>
    </div>

    <!-- 表格 2:1076 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>
      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!filtered.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">
          没有符合条件的核销记录
        </div>
        <div
          v-for="r in filtered"
          v-else
          :key="r.id"
          class="flex items-center text-[12px]"
          style="height: 44px; border-top: 1px solid var(--tc-line)"
        >
          <span class="px-2 truncate" style="width: 180px; color: var(--p)">{{ r.pickupCode || '—' }}</span>
          <span class="px-2 truncate" style="width: 410px; color: var(--txt)">{{ r.description || '—' }}</span>
          <span class="px-2 truncate" style="width: 120px; color: var(--txt)">¥{{ money(r.actualPrice) }}</span>
          <span class="px-2 truncate" style="width: 130px; color: var(--txt)">{{ r.verifierName || '—' }}</span>
          <span class="px-2 truncate" style="width: 180px; color: var(--tc-sub)">{{ fmtTime(r.verifyTime) }}</span>
          <span class="px-2 flex items-center" style="width: 150px">
            <button class="text-[12px]" style="color: var(--p)" @click="detailRow = r">详情</button>
          </span>
        </div>
      </div>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      共 {{ filtered.length }} 条 ｜ 核销记录取「已完成且 verify_time 非空」的商城自提单；
      「核销方式（扫码/手输）」工程无此字段，故本表不展示该列。
    </p>

    <!-- ============ 只读详情弹窗 ============ -->
    <Teleport to="body">
      <div
        v-if="detailRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="detailRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(460px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">核销详情</span>
            <span class="text-[12px]" style="color: var(--p)">{{ detailRow.pickupCode || '—' }}</span>
          </div>
          <div class="mt-4 flex flex-col gap-2 text-[12px]">
            <div v-for="kv in detailPairs" :key="kv[0]" class="flex">
              <span style="color: var(--tc-sub)">{{ kv[0] }}</span>
              <span class="ml-auto text-right" style="color: var(--txt); max-width: 300px">{{ kv[1] }}</span>
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
import { computed, onMounted, ref } from 'vue'
import { fetchVerifyRecordList, type PickupOrderRow } from '@/api/pickup'

/** 去掉画板的「核销方式」列后，按 1170 重新分配（180+410+120+130+180+150） */
const COLS = [
  { key: 'code', label: '自提码', w: 180 },
  { key: 'desc', label: '订单摘要', w: 410 },
  { key: 'amount', label: '金额', w: 120 },
  { key: 'verifier', label: '核销人', w: 130 },
  { key: 'time', label: '核销时间', w: 180 },
  { key: 'op', label: '操作', w: 150 }
] as const

const rows = ref<PickupOrderRow[]>([])
const loading = ref(true)
const qCode = ref('')
const qVerifier = ref('')
const qDateFrom = ref('')
const qDateTo = ref('')
const detailRow = ref<PickupOrderRow | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await fetchVerifyRecordList({
      pickupCode: qCode.value || undefined,
      verifierId: qVerifier.value ? Number(qVerifier.value) : undefined,
      startVerifyTime: qDateFrom.value ? `${qDateFrom.value} 00:00:00` : undefined,
      endVerifyTime: qDateTo.value ? `${qDateTo.value} 23:59:59` : undefined
    })
    rows.value = res?.records ?? []
  } catch (e) {
    console.error('[pickup] 核销记录加载失败', e)
  } finally {
    loading.value = false
  }
}

/** 核销人下拉：从已加载的记录里去重派生（后端无「核销人列表」端点，不另造接口） */
const verifierOptions = computed(() => {
  const seen = new Map<number, string>()
  for (const r of rows.value) {
    if (r.verifierId != null && !seen.has(r.verifierId)) {
      seen.set(r.verifierId, r.verifierName || `#${r.verifierId}`)
    }
  }
  return [...seen.entries()].map(([id, name]) => ({ id, name }))
})

/** 核销人下拉在服务端筛选；日期与自提码也在服务端筛（后端已支持），此处只兜一次客户端一致性 */
const filtered = computed(() => {
  let list = rows.value
  if (qVerifier.value) list = list.filter(r => String(r.verifierId) === qVerifier.value)
  return list
})

const detailPairs = computed(() => {
  const r = detailRow.value
  if (!r) return [] as [string, string][]
  return [
    ['自提码', r.pickupCode || '—'],
    ['订单号', r.orderNo || '—'],
    ['订单摘要', r.description || '—'],
    ['金额', `¥${money(r.actualPrice)}`],
    ['核销人', r.verifierName || '—'],
    ['核销时间', fmtTime(r.verifyTime)],
    ['下单时间', fmtTime(r.createTime)],
    ['联系手机', r.contactPhone || '—']
  ]
})

/** 导出 CSV（前端生成，不需要后端接口；带 BOM 便于 Excel 打开中文） */
function exportCsv() {
  const head = ['自提码', '订单摘要', '金额', '核销人', '核销时间']
  const lines = filtered.value.map(r =>
    [r.pickupCode ?? '', (r.description ?? '').replace(/,/g, '，'), money(r.actualPrice), r.verifierName ?? '', fmtTime(r.verifyTime)]
      .map(v => `"${String(v).replace(/"/g, '""')}"`)
      .join(',')
  )
  const csv = '\uFEFF' + [head.join(','), ...lines].join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `核销记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function fmtTime(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{4})\D(\d{2})\D(\d{2})\D+(\d{1,2}):(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4].padStart(2, '0')}:${m[5]}` : '—'
}

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}
</script>
