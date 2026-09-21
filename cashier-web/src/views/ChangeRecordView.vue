<template>
  <!--
    W-13 改单 / 退单记录（收银端 /orders/aftersales）
    ⚠️ 画板 W-13 本体是**一对弹窗**（改单 2:932 / 退单 2:933），入口在**订单详情页**；
       本页是配套的**留痕查询**，数据源 tb_order_change_log（改单/退单各记一条）。
       菜单项「售后与退单」原本指向占位页，现落到这里，不再有死链。
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 -->
    <div class="shrink-0 flex items-center gap-3">
      <select
        v-model="changeType"
        class="rounded border text-[12px] outline-none"
        style="width: 140px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
        @change="load"
      >
        <option :value="0">全部类型</option>
        <option :value="1">仅改单</option>
        <option :value="2">仅退单</option>
      </select>
      <input
        v-model.trim="orderNo"
        class="rounded border text-[12px] outline-none"
        style="width: 220px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="订单号"
        @keyup.enter="load"
      />
      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 32px; padding: 0 18px; background: var(--p)"
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

    <!-- 表格 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>
      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!rows.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">
          暂无改单 / 退单记录 —— 在「订单详情」页点「改单」「退单」后，这里会出现留痕
        </div>
        <div
          v-for="r in rows"
          v-else
          :key="r.id"
          class="flex items-center text-[12px]"
          :style="{ height: '48px', borderTop: '1px solid var(--tc-line)', background: r.changeType === 2 ? '#FCE3E3' : '#fff' }"
        >
          <span class="px-2 truncate" style="width: 180px; color: var(--p)">{{ r.orderNo || '—' }}</span>
          <span class="px-2 truncate" style="width: 100px" :style="{ color: r.changeType === 2 ? 'var(--warn)' : 'var(--txt)' }">
            {{ r.changeType === 2 ? '退单' : '改单' }}
          </span>
          <span class="px-2 truncate" style="width: 200px; color: var(--tc-sub)">
            <template v-if="r.changeType === 1">¥{{ money(r.beforeAmount) }} → ¥{{ money(r.afterAmount) }}</template>
            <template v-else>{{ r.refundWay === 1 ? '整单退' : '部分退' }} ¥{{ money(r.refundAmount) }}</template>
          </span>
          <span class="px-2 truncate" style="width: 150px" :style="{ color: diffColor(r) }">{{ diffText(r) }}</span>
          <span class="px-2 truncate" style="width: 160px; color: var(--tc-sub)">
            {{ r.changeType === 2 ? refundReasonText(r.reasonCode) : (r.reasonText || '—') }}
          </span>
          <span class="px-2 truncate" style="width: 180px; color: var(--tc-sub)">{{ fmtTime(r.createTime) }}</span>
          <span class="px-2 flex items-center" style="width: 200px">
            <button class="text-[12px]" style="color: var(--p)" @click="detailRow = r">明细</button>
            <router-link
              class="ml-3 text-[12px]"
              style="color: var(--p)"
              :to="`/orders/detail?id=${r.orderId}`"
            >查看订单</router-link>
          </span>
        </div>
      </div>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      共 {{ rows.length }} 条 ｜ 数据源 <code>tb_order_change_log</code>；
      退单同时会写 <code>tb_order_refund</code>（复用既有退款模型，状态=退款成功）。
      演示环境的退款为**模拟成功**（与收款 MockPay 同口径），无真实资金动作。
    </p>

    <!-- 明细弹窗 -->
    <Teleport to="body">
      <div v-if="detailRow" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="detailRow = null">
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(520px, calc(100vw - 32px))">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">
            {{ detailRow.changeType === 2 ? '退单' : '改单' }}明细
          </span>
          <p class="mt-1 text-[12px]" style="color: var(--tc-sub)">{{ detailRow.orderNo }}</p>
          <div class="mt-4 flex flex-col gap-2 text-[12px]">
            <div class="flex"><span style="color: var(--tc-sub)">变更前/后金额</span>
              <span class="ml-auto" style="color: var(--txt)">¥{{ money(detailRow.beforeAmount) }} / ¥{{ money(detailRow.afterAmount) }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">差额</span>
              <span class="ml-auto" :style="{ color: diffColor(detailRow) }">{{ diffText(detailRow) }}</span></div>
            <div v-if="detailRow.refundAmount != null" class="flex"><span style="color: var(--tc-sub)">退款金额</span>
              <span class="ml-auto" style="color: var(--warn)">¥{{ money(detailRow.refundAmount) }}</span></div>
            <div class="flex"><span style="color: var(--tc-sub)">操作人 id</span>
              <span class="ml-auto" style="color: var(--txt)">{{ detailRow.operatorId ?? '—' }}</span></div>
            <div class="flex"><span class="shrink-0" style="color: var(--tc-sub)">明细快照</span>
              <span class="ml-auto text-right break-all" style="color: var(--txt); max-width: 340px">{{ detailRow.detailSnapshot || '—' }}</span></div>
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
import { onMounted, ref } from 'vue'
import { fetchChangeRecords, refundReasonText, type ChangeRecordRow } from '@/api/orderChange'

const COLS = [
  { key: 'orderNo', label: '订单号', w: 180 },
  { key: 'type', label: '类型', w: 100 },
  { key: 'amount', label: '金额变动', w: 200 },
  { key: 'diff', label: '差额', w: 150 },
  { key: 'reason', label: '原因', w: 160 },
  { key: 'time', label: '时间', w: 180 },
  { key: 'op', label: '操作', w: 200 }
] as const

const rows = ref<ChangeRecordRow[]>([])
const loading = ref(true)
const changeType = ref(0)
const orderNo = ref('')
const detailRow = ref<ChangeRecordRow | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    rows.value = (await fetchChangeRecords({
      changeType: changeType.value || undefined,
      orderNo: orderNo.value || undefined
    })) ?? []
  } catch (e) {
    console.error('[change-record] 加载失败', e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

function diffText(r: ChangeRecordRow): string {
  if (r.changeType === 2) return '退款 ¥' + money(r.refundAmount)
  const d = Number(r.diffAmount) || 0
  if (d === 0) return '无差额'
  return (d > 0 ? '需补收 ¥' : '应退 ¥') + money(Math.abs(d))
}

function diffColor(r: ChangeRecordRow): string {
  if (r.changeType === 2) return 'var(--warn)'
  const d = Number(r.diffAmount) || 0
  if (d === 0) return 'var(--tc-sub)'
  return d > 0 ? '#B8860B' : 'var(--warn)'
}

function fmtTime(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{4})\D(\d{2})\D(\d{2})\D+(\d{1,2}):(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4].padStart(2, '0')}:${m[5]}` : '—'
}

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}

function exportCsv() {
  const head = ['订单号', '类型', '变更前', '变更后', '差额', '退款额', '原因', '时间']
  const lines = rows.value.map(r => [
    r.orderNo ?? '',
    r.changeType === 2 ? '退单' : '改单',
    money(r.beforeAmount),
    money(r.afterAmount),
    money(r.diffAmount),
    money(r.refundAmount),
    r.changeType === 2 ? refundReasonText(r.reasonCode) : (r.reasonText ?? ''),
    fmtTime(r.createTime)
  ].map(v => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const csv = '\uFEFF' + [head.join(','), ...lines].join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `改单退单记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
</script>
