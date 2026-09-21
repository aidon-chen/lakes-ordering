<template>
  <!--
    W-19 存酒临期与过期提醒（Ardot 2:1213，内容区 2:1216）
    面包屑 40 ｜ 指标行 100（3×362, gap 18）｜ 提醒规则卡 90 ｜ 任务表 300（表头 40 + 行 52）
    7 列：存酒码170 / 顾客160 / 品名240 / 有效期140 / 剩余天数130 / 通知状态170 / 操作160 = 1170
    ⚠️ 与画板的三处差异（都有依据，不放假按钮/不编文案）：
      ① 「已发短信 09-13」→ 显示「已弹窗 09-21」：通知方式按拍板 = **小程序端弹窗**，非短信
      ② 操作列**不放「立即通知 / 再次通知」**：弹窗发生在顾客自己的手机上，收银端无法主动触发
         （顾客打开小程序「我的存酒」时由客户端自动弹窗并回报，本页状态随之更新）
      ③ 「销毁登记」不做：工程无对应接口（同批次 F 的「转赠」处理）
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 指标行 2:1218 -->
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

    <!-- 提醒规则卡 2:1222 -->
    <div class="rounded flex items-center shrink-0" style="height: 90px; padding: 16px; background: #fff; border: 1px solid var(--tc-line); gap: 16px">
      <span class="text-[13px]" style="color: var(--txt)">提醒规则</span>
      <span class="text-[13px]" style="color: var(--txt)">到期前</span>
      <input
        v-model.number="aheadDays"
        type="number"
        :min="1"
        :max="90"
        class="rounded border text-[13px] outline-none"
        style="width: 90px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
      />
      <span class="text-[13px]" style="color: var(--txt)">天</span>
      <span class="text-[12px]" style="color: var(--tc-sub)">
        小程序端「我的存酒」进页弹窗提示（拍板口径；短信通道未接）
      </span>
      <button
        class="ml-auto rounded text-[13px] font-medium text-white disabled:opacity-50"
        style="height: 36px; padding: 0 20px; background: var(--p)"
        :disabled="savingRule"
        @click="saveRule"
      >{{ savingRule ? '保存中…' : '保存' }}</button>
    </div>

    <!-- 任务表 2:1231 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>
      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!rows.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">
          暂无临期或过期的存酒 —— 剩余天数大于「提前天数」的记录不会出现在这里
        </div>
        <div
          v-for="r in rows"
          v-else
          :key="r.storageId"
          class="flex items-center text-[12px]"
          :style="{ height: '52px', borderTop: '1px solid var(--tc-line)', background: r.expired ? '#FCE3E3' : '#fff' }"
        >
          <span class="px-2 truncate" style="width: 170px; color: var(--p)">{{ r.storageCode || '—' }}</span>
          <span class="px-2 truncate" style="width: 160px; color: var(--txt)">
            {{ r.customerName || '—' }}<template v-if="r.customerPhone"> {{ r.customerPhone }}</template>
          </span>
          <span class="px-2 truncate" style="width: 240px; color: var(--txt)">{{ r.goodsName || '—' }}</span>
          <span class="px-2 truncate" style="width: 140px; color: var(--txt)">{{ r.expireDate || '—' }}</span>
          <span class="px-2 truncate" style="width: 130px" :style="{ color: r.expired ? 'var(--warn)' : '#B8860B' }">
            {{ r.daysRemaining < 0 ? (r.daysRemaining + ' 天') : (r.daysRemaining + ' 天') }}
          </span>
          <span class="px-2 truncate" style="width: 170px" :style="{ color: r.notified ? '#008000' : '#B8860B' }">
            {{ r.notified ? ('已弹窗 ' + mmdd(r.lastNotifyTime)) : '待提示' }}
          </span>
          <span class="px-2 flex items-center" style="width: 160px">
            <button class="text-[12px]" style="color: var(--p)" @click="detailRow = r">查看</button>
          </span>
        </div>
      </div>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      通知方式：顾客打开小程序「我的存酒」时自动弹窗（同一存酒每天最多提醒一次）；
      收银端无法主动触发顾客端弹窗，故不设「立即通知」。弹窗时间记录见
      <code>tb_wine_remind_log</code>。画板的「销毁登记」无对应接口，本期不做。
    </p>

    <!-- ============ 只读详情弹窗 ============ -->
    <Teleport to="body">
      <div v-if="detailRow" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="detailRow = null">
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(440px, calc(100vw - 32px))">
          <div class="flex items-baseline justify-between">
            <span class="text-[16px] font-bold" style="color: var(--tc-title)">存酒详情</span>
            <span class="text-[12px]" style="color: var(--p)">{{ detailRow.storageCode || '—' }}</span>
          </div>
          <div class="mt-4 flex flex-col gap-2 text-[12px]">
            <div v-for="kv in detailPairs" :key="kv[0]" class="flex">
              <span style="color: var(--tc-sub)">{{ kv[0] }}</span>
              <span class="ml-auto text-right" style="color: var(--txt); max-width: 260px">{{ kv[1] }}</span>
            </div>
          </div>
          <div class="mt-5 flex justify-end">
            <button class="rounded border text-[13px]" style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)" @click="detailRow = null">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchWineRemindList,
  fetchWineRemindSetting,
  fetchWineRemindSummary,
  mmdd,
  saveWineRemindSetting,
  type WineRemindRow
} from '@/api/wineRemind'

const COLS = [
  { key: 'code', label: '存酒码', w: 170 },
  { key: 'customer', label: '顾客', w: 160 },
  { key: 'goods', label: '品名', w: 240 },
  { key: 'expire', label: '有效期', w: 140 },
  { key: 'days', label: '剩余天数', w: 130 },
  { key: 'notify', label: '通知状态', w: 170 },
  { key: 'op', label: '操作', w: 160 }
] as const

const rows = ref<WineRemindRow[]>([])
const loading = ref(true)
const summary = ref({ expiring: 0, expired: 0, notified: 0 })
const aheadDays = ref(7)
const savingRule = ref(false)
const detailRow = ref<WineRemindRow | null>(null)
const status = ref('')

const metrics = computed(() => [
  { label: '临期（剩余 ≤ 提前天数）', value: summary.value.expiring, color: summary.value.expiring ? '#B8860B' : 'var(--p)' },
  { label: '已过期', value: summary.value.expired, color: summary.value.expired ? 'var(--warn)' : 'var(--p)' },
  { label: '已通知（弹窗）', value: summary.value.notified, color: '#008000' }
])

const detailPairs = computed(() => {
  const r = detailRow.value
  if (!r) return [] as [string, string][]
  return [
    ['存酒码', r.storageCode || '—'],
    ['顾客', (r.customerName || '—') + (r.customerPhone ? ' ' + r.customerPhone : '')],
    ['品名', r.goodsName || '—'],
    ['剩余瓶数', String(r.remainCount ?? '—')],
    ['有效期', r.expireDate || '—'],
    ['剩余天数', r.daysRemaining < 0 ? ('已过期 ' + Math.abs(r.daysRemaining) + ' 天') : ('剩 ' + r.daysRemaining + ' 天')],
    ['通知状态', r.notified ? ('已弹窗 ' + mmdd(r.lastNotifyTime)) : '待提示']
  ]
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [list, sum, setting] = await Promise.all([
      fetchWineRemindList(),
      fetchWineRemindSummary(),
      fetchWineRemindSetting()
    ])
    rows.value = list ?? []
    summary.value = sum ?? summary.value
    const d = Number(setting?.wineRemindAheadDays)
    if (Number.isFinite(d) && d >= 1) aheadDays.value = d
  } catch (e) {
    status.value = `加载失败：${(e as Error).message || ''}`
    console.error('[wine-remind] 加载失败', e)
  } finally {
    loading.value = false
  }
}

async function saveRule() {
  if (savingRule.value) return
  savingRule.value = true
  try {
    await saveWineRemindSetting(Math.max(1, Number(aheadDays.value) || 7))
    status.value = '提醒规则已保存（顾客端下次进页即按新天数判断）'
    await load()
  } catch (e) {
    status.value = `保存失败：${(e as Error).message || ''}`
  } finally {
    savingRule.value = false
  }
}
</script>
