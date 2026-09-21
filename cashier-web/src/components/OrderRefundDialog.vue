<template>
  <!--
    W-13 退单弹窗（Ardot 2:933，540×600）
    标题「退单 · SO…」+ 整单退/部分退 + 勾选明细（危险浅底）+ 退单原因 + 退款金额（原路退回）+ 取消/确认退单
    ⚠️ 退款执行与收款同构：微信路径最终调 wxPayService.refund()，该方法**非 prod profile 下为模拟成功**
       （与收款 MockPay 同口径）→ 弹窗内明确标注「演示环境为模拟退款」，不让人误以为真扣了钱。
  -->
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="close">
      <div class="rounded-lg bg-white flex flex-col" style="width: min(540px, calc(100vw - 32px)); max-height: 600px">
        <div class="flex items-baseline shrink-0" style="padding: 20px 20px 0">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">退单 · {{ orderNo }}</span>
          <button class="ml-auto text-[14px]" style="color: var(--tc-sub)" @click="close">✕</button>
        </div>

        <div class="flex-1 min-h-0 overflow-y-auto flex flex-col" style="padding: 16px 20px; gap: 12px">
          <!-- 退单类型 -->
          <div class="flex" style="gap: 10px">
            <button
              v-for="w in [{ v: 1, t: '整单退' }, { v: 2, t: '部分退' }]"
              :key="w.v"
              class="rounded text-[13px]"
              :style="{
                width: '110px', height: '34px',
                background: way === w.v ? 'var(--p)' : '#fff',
                color: way === w.v ? '#fff' : 'var(--tc-gray)',
                border: '1px solid var(--tc-line)'
              }"
              @click="way = w.v as 1 | 2"
            >{{ w.t }}</button>
          </div>

          <!-- 部分退：勾选明细 -->
          <template v-if="way === 2">
            <div
              v-for="(d, i) in picked"
              :key="d.detailId"
              class="rounded flex items-center"
              style="height: 48px; padding: 0 12px; background: #FCE3E3; gap: 10px"
            >
              <input v-model="d.checked" type="checkbox" />
              <span class="text-[13px] flex-1 truncate" style="color: var(--txt)">
                {{ d.goodsName }} ×{{ d.maxNumber }}
              </span>
              <span class="text-[13px] font-medium shrink-0" style="color: var(--warn)">¥{{ money(d.price) }}</span>
              <template v-if="d.checked && (d.maxNumber || 0) > 1">
                <input
                  v-model.number="d.number"
                  type="number"
                  :min="1"
                  :max="d.maxNumber || 1"
                  class="rounded border text-[12px] text-center outline-none shrink-0"
                  style="width: 52px; height: 26px; border-color: var(--tc-line)"
                />
                <span class="text-[12px] shrink-0" style="color: var(--tc-sub)">件</span>
              </template>
              <span v-else class="text-[11px] shrink-0" style="color: var(--tc-hold)">#{{ i + 1 }}</span>
            </div>
            <p v-if="!picked.length" class="text-[12px]" style="color: var(--tc-sub)">该订单没有可退明细</p>
          </template>

          <!-- 退单原因 -->
          <div class="flex flex-col" style="gap: 6px">
            <span class="text-[13px]" style="color: var(--txt)">退单原因</span>
            <select
              v-model.number="reasonCode"
              class="rounded border text-[13px] outline-none"
              style="height: 36px; padding: 0 10px; border-color: var(--tc-line)"
            >
              <option v-for="r in REFUND_REASONS" :key="r.code" :value="r.code">{{ r.text }}</option>
            </select>
            <input
              v-model.trim="reasonText"
              class="rounded border text-[12px] outline-none"
              style="height: 34px; padding: 0 10px; border-color: var(--tc-line)"
              placeholder="补充说明（选填）"
            />
          </div>
        </div>

        <div class="shrink-0" style="padding: 0 20px 20px">
          <div class="flex items-center" style="height: 29px">
            <span class="text-[13px]" style="color: var(--txt)">退款金额（原路退回）</span>
            <span class="ml-auto text-[16px] font-bold" style="color: var(--warn)">¥{{ money(refundAmount) }}</span>
          </div>
          <p class="text-[11px] mt-1" style="color: var(--tc-hold)">
            ⚠️ 当前为演示环境：微信退款为**模拟成功**（与收款走 MockPay 同口径），无真实资金动作；
            商户号与 apiclient_cert 证书齐备后自动转为真实退款。
          </p>
          <p v-if="err" class="text-[12px] mt-1" style="color: var(--warn)">{{ err }}</p>
          <div class="flex justify-end mt-3" style="gap: 12px">
            <button class="rounded border text-[13px]" style="height: 36px; padding: 0 20px; border-color: var(--tc-line); color: var(--tc-gray)" @click="close">取消</button>
            <button
              class="rounded text-[13px] font-medium text-white disabled:opacity-50"
              style="height: 36px; padding: 0 22px; background: var(--warn)"
              :disabled="saving || refundAmount <= 0"
              @click="doConfirm"
            >{{ saving ? '处理中…' : '确认退单' }}</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { REFUND_REASONS, refundOrder } from '@/api/orderChange'
import type { OrderDetailRow } from '@/api/order'

const props = defineProps<{
  visible: boolean
  orderId: number
  orderNo: string
  actualPrice: number
  details: OrderDetailRow[]
}>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'done', amount: number): void }>()

interface PickRow {
  detailId: number
  goodsName: string | null
  price: number
  maxNumber: number
  number: number
  checked: boolean
}

const way = ref<1 | 2>(1)
const reasonCode = ref<number>(4)
const reasonText = ref('')
const saving = ref(false)
const err = ref('')
const picked = ref<PickRow[]>([])

const refundAmount = computed(() => {
  if (way.value === 1) {
    return Number(props.actualPrice) || 0
  }
  return picked.value
    .filter(p => p.checked)
    .reduce((s, p) => s + (Number(p.price) || 0) * (Number(p.number) || 1), 0)
})

function init() {
  way.value = 1
  reasonCode.value = 4
  reasonText.value = ''
  err.value = ''
  picked.value = props.details.map(d => ({
    detailId: d.id,
    goodsName: d.goodsName,
    price: Number(d.price) || 0,
    maxNumber: Number(d.number) || 1,
    number: Number(d.number) || 1,
    checked: false
  }))
}
defineExpose({ init })

async function doConfirm() {
  if (saving.value) return
  const amount = refundAmount.value
  if (amount <= 0) {
    err.value = way.value === 2 ? '请勾选要退的明细' : '退款金额为 0，无法退单'
    return
  }
  if (!confirm(`确认退单 ¥${amount.toFixed(2)}？\n（演示环境为模拟退款，无真实资金动作）`)) return
  saving.value = true
  err.value = ''
  try {
    const items = way.value === 2
      ? picked.value.filter(p => p.checked).map(p => ({ detailId: p.detailId, number: Number(p.number) || 1 }))
      : []
    const res = await refundOrder(props.orderId, way.value, reasonCode.value, reasonText.value, items)
    emit('done', Number(res?.refundAmount ?? amount))
    emit('close')
  } catch (e) {
    err.value = (e as Error).message || '退单失败'
  } finally {
    saving.value = false
  }
}

function close() {
  emit('close')
}

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}
</script>
