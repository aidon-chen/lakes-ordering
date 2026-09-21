<template>
  <!--
    W-25 打印队列与异常告警（Ardot 2:1312，内容区 2:1315）
    告警条 40 ｜ 指标行 100（3×362, gap 18）｜ 任务表 320（表头 40 + 行 52）｜ 重试策略卡 90
    表格 8 列：订单号190 / 类型130 / 打印机170 / 下发160 / 重试130 / 状态140 / 原因130 / 操作120 = 1170
    ⚠️ 「立即重试」只把任务置回待打印 —— 真正的下发依赖云打印平台账号（P-01），未就绪（页脚已注明）
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 告警条 2:1317 -->
    <div
      class="rounded flex items-center shrink-0"
      :style="{ height: '40px', paddingLeft: '12px', gap: '10px', background: alertBar.bg }"
    >
      <span class="shrink-0" :style="{ width: '3px', height: '20px', borderRadius: '2px', background: alertBar.bar }" />
      <span class="text-[12px]" :style="{ color: alertBar.fg }">{{ alertBar.text }}</span>
    </div>

    <!-- 指标行 2:1318 -->
    <div class="shrink-0 flex" style="gap: 18px">
      <div
        v-for="m in metrics"
        :key="m.label"
        class="rounded flex flex-col justify-center"
        style="width: 362px; height: 100px; padding-left: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 6px"
      >
        <span class="text-[12px]" style="color: var(--tc-sub)">{{ m.label }}</span>
        <span class="text-[28px] font-bold" :style="{ color: m.color }">{{ m.value }}</span>
      </div>
    </div>

    <!-- 任务表 2:1322 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>
      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!rows.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">
          队列为空 —— 目前没有待打印或失败的打印任务
        </div>
        <div
          v-for="t in rows"
          v-else
          :key="t.id"
          class="flex items-center text-[12px]"
          :style="{ height: '52px', borderTop: '1px solid var(--tc-line)', background: t.status === 4 ? '#FCE3E3' : '#fff' }"
        >
          <span class="px-2 truncate" style="width: 190px" :style="{ color: t.status === 4 ? 'var(--warn)' : 'var(--p)' }">
            {{ t.orderNo || '—' }}
          </span>
          <span class="px-2 truncate" style="width: 130px; color: var(--txt)">{{ ticketTypeText(t.ticketType) }}</span>
          <span class="px-2 truncate" style="width: 170px; color: var(--txt)">{{ t.printerName || '—' }}</span>
          <span class="px-2 truncate" style="width: 160px; color: var(--tc-sub)">{{ hms(t.dispatchTime) }}</span>
          <span class="px-2 truncate" style="width: 130px; color: var(--txt)">
            {{ t.retryCount ?? 0 }} / {{ t.maxRetry ?? 3 }}
          </span>
          <span class="px-2 truncate" style="width: 140px" :style="{ color: statusColor(t.status) }">
            {{ taskStatusText(t.status) }}
          </span>
          <span class="px-2 truncate" style="width: 130px; color: var(--tc-sub)">{{ t.failReason || '—' }}</span>
          <span class="px-2 flex items-center gap-2" style="width: 120px">
            <button
              v-if="t.status === 4 || t.status === 1"
              class="text-[12px] disabled:opacity-40"
              style="color: var(--p)"
              :disabled="busy"
              @click="doRetry(t)"
            >立即重试</button>
            <button
              v-if="t.status !== 5 && t.status !== 3"
              class="text-[12px] disabled:opacity-40"
              style="color: var(--tc-gray)"
              :disabled="busy"
              @click="doIgnore(t)"
            >忽略</button>
            <button
              v-if="t.status === 3 || t.status === 4 || t.status === 5"
              class="text-[12px] disabled:opacity-40"
              style="color: var(--p)"
              :disabled="busy"
              @click="doReprint(t)"
            >补打</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 重试策略卡 2:1350 -->
    <div class="rounded flex items-center shrink-0" style="height: 90px; padding: 16px; background: #fff; border: 1px solid var(--tc-line); gap: 16px">
      <span class="text-[13px]" style="color: var(--txt)">重试策略（4.5 可配置）</span>
      <span class="text-[13px]" style="color: var(--txt)">自动重试 {{ policyMax }} 次</span>
      <span class="text-[13px]" style="color: var(--txt)">间隔 {{ policyInterval }} 秒</span>
      <span class="text-[13px]" :style="{ color: policyAlert ? 'var(--warn)' : 'var(--tc-sub)' }">
        失败告警：{{ policyAlert ? '开' : '关' }}{{ policyAlert ? '（侧栏角标 + 页面顶栏常驻）' : '' }}
      </span>
      <button
        class="ml-auto rounded border text-[12px]"
        style="height: 36px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
        @click="openPolicy"
      >编辑策略</button>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      ⚠️「立即重试」只把任务置回「待打印」并刷新下次重试时间；**真正的下发依赖云打印平台账号（P-01，未就绪）**，
      因此重试后任务会停在「待打印」，这是当前预期行为。失败告警的「侧栏角标」需外壳接入，本期仅存设置未接角标。
    </p>

    <!-- ============ 编辑策略弹窗 ============ -->
    <Teleport to="body">
      <div v-if="editPolicy" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="editPolicy = false">
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(420px, calc(100vw - 32px))">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">编辑重试策略</span>
          <div class="mt-4 flex flex-col" style="gap: 12px">
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">自动重试次数</span>
              <input v-model.number="draftMax" type="number" :min="0" :max="10" class="rounded border outline-none flex-1" style="height: 34px; padding: 0 10px; border-color: var(--tc-line)" />
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">重试间隔(秒)</span>
              <input v-model.number="draftInterval" type="number" :min="5" :max="600" class="rounded border outline-none flex-1" style="height: 34px; padding: 0 10px; border-color: var(--tc-line)" />
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">失败告警</span>
              <input v-model="draftAlert" type="checkbox" />
              <span style="color: var(--tc-sub)">开启后侧栏角标 + 页面顶栏常驻（角标需外壳接入）</span>
            </label>
          </div>
          <div class="mt-5 flex justify-end gap-2">
            <button class="rounded border text-[13px]" style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)" @click="editPolicy = false">取消</button>
            <button class="rounded text-[13px] text-white" style="height: 34px; padding: 0 18px; background: var(--p)" @click="savePolicy">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchPrintSummary,
  fetchPrintTasks,
  ignorePrintTask,
  reprintTask,
  retryPrintTask,
  taskStatusText,
  ticketTypeText,
  type PrintTaskRow
} from '@/api/print'
import { fetchShopSettings, saveShopSettings } from '@/api/shopSetting'

const COLS = [
  { key: 'orderNo', label: '订单号', w: 190 },
  { key: 'type', label: '小票类型', w: 130 },
  { key: 'printer', label: '目标打印机', w: 170 },
  { key: 'dispatch', label: '下发时间', w: 160 },
  { key: 'retry', label: '重试次数', w: 130 },
  { key: 'status', label: '状态', w: 140 },
  { key: 'reason', label: '失败原因', w: 130 },
  { key: 'op', label: '操作', w: 120 }
] as const

const rows = ref<PrintTaskRow[]>([])
const loading = ref(true)
const busy = ref(false)
const summary = ref({ waitRetry: 0, todayFail: 0, todaySuccess: 0 })
const status = ref('')

/** 重试策略（存在 tb_shop_setting，与 W-23 共用同一套设置存储） */
const policyMax = ref(3)
const policyInterval = ref(30)
const policyAlert = ref(true)
const editPolicy = ref(false)
const draftMax = ref(3)
const draftInterval = ref(30)
const draftAlert = ref(true)

const metrics = computed(() => [
  { label: '待重试任务', value: summary.value.waitRetry, color: summary.value.waitRetry ? 'var(--warn)' : 'var(--p)' },
  { label: '今日失败', value: summary.value.todayFail, color: summary.value.todayFail ? 'var(--warn)' : 'var(--p)' },
  { label: '今日成功', value: summary.value.todaySuccess, color: '#008000' }
])

const alertBar = computed(() => {
  if (summary.value.waitRetry > 0) {
    return { bg: '#FCE3E3', bar: '#F56B6B', fg: '#791F1F', text: `${summary.value.waitRetry} 个打印任务待重试（含失败任务）—— 请检查打印机缺纸 / 离线，或点「立即重试」` }
  }
  if (summary.value.todayFail > 0) {
    return { bg: '#FCF5ED', bar: '#E5A33D', fg: '#854F0B', text: `今日有 ${summary.value.todayFail} 次打印失败，当前队列已清空` }
  }
  return { bg: '#EDF2DE', bar: '#66C23B', fg: '#404A21', text: '打印队列正常，无待重试任务' }
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [page, sum, settings] = await Promise.all([
      fetchPrintTasks({ onlyPendingOrFail: false, pageSize: 100 }),
      fetchPrintSummary(),
      fetchShopSettings().catch(() => ({}) as Record<string, string>)
    ])
    rows.value = page?.records ?? []
    summary.value = sum ?? summary.value
    if (settings.printRetryMax) policyMax.value = Number(settings.printRetryMax)
    if (settings.printRetryIntervalSec) policyInterval.value = Number(settings.printRetryIntervalSec)
    if (settings.printFailAlert) policyAlert.value = settings.printFailAlert === 'true'
  } catch (e) {
    status.value = `加载失败：${(e as Error).message || ''}（若为「表不存在」，请先执行 sql/mysql/cloudrun/23-打印模块表.sql）`
    console.error('[print-queue] 加载失败', e)
  } finally {
    loading.value = false
  }
}

async function doRetry(t: PrintTaskRow) {
  busy.value = true
  try {
    await retryPrintTask(t.id)
    status.value = `已把 ${t.orderNo || t.id} 置回待打印（下发需云打印账号就绪）`
    await load()
  } catch (e) {
    alert((e as Error).message || '重试失败')
  } finally {
    busy.value = false
  }
}

async function doIgnore(t: PrintTaskRow) {
  if (!confirm(`确定忽略打印任务「${t.orderNo || t.id}」？忽略后不再重试，如需打印请用「补打」。`)) return
  busy.value = true
  try {
    await ignorePrintTask(t.id)
    await load()
  } catch (e) {
    alert((e as Error).message || '忽略失败')
  } finally {
    busy.value = false
  }
}

async function doReprint(t: PrintTaskRow) {
  busy.value = true
  try {
    await reprintTask(t.id)
    status.value = `已为 ${t.orderNo || t.id} 新建补打任务（在 W-21 补打记录可见）`
    await load()
  } catch (e) {
    alert((e as Error).message || '补打失败')
  } finally {
    busy.value = false
  }
}

function openPolicy() {
  draftMax.value = policyMax.value
  draftInterval.value = policyInterval.value
  draftAlert.value = policyAlert.value
  editPolicy.value = true
}

async function savePolicy() {
  try {
    await saveShopSettings({
      printRetryMax: String(draftMax.value),
      printRetryIntervalSec: String(draftInterval.value),
      printFailAlert: String(draftAlert.value)
    })
    policyMax.value = draftMax.value
    policyInterval.value = draftInterval.value
    policyAlert.value = draftAlert.value
    editPolicy.value = false
    status.value = '重试策略已保存'
  } catch (e) {
    alert((e as Error).message || '保存失败')
  }
}

function statusColor(s: number | null): string {
  if (s === 4) return 'var(--warn)'
  if (s === 3) return '#008000'
  if (s === 5) return 'var(--tc-hold)'
  return 'var(--txt)'
}

function hms(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{1,2}):(\d{2})(?::(\d{2}))?/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}${m[3] ? ':' + m[3] : ''}` : '—'
}
</script>
