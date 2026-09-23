<template>
  <!-- W-04 结算栏-深色 290（Ardot 83:1：padding 14、圆角 8、gap 10、底 = 侧栏底深色） -->
  <div
    class="shrink-0 w-[230px] min-[1440px]:w-[290px] 2xl:w-[320px] rounded-lg flex flex-col overflow-hidden"
    style="padding: 14px; gap: 10px; background: var(--dark)"
  >
    <!-- 台位号 83:2 -->
    <div class="text-[17px] font-medium text-white leading-tight">{{ tableLine }}</div>

    <!-- 低消进度 83:3~83:6（低消仅展示提示，不参与结算校验 —— V7 变更一） -->
    <template v-if="minSpend > 0">
      <div class="rounded-full overflow-hidden" style="height: 8px; background: var(--dark2)">
        <div class="h-full rounded-full" :style="`width: ${progress}%; background: var(--p)`" />
      </div>
      <div class="text-[12px] leading-tight" style="color: #aab2a0">
        低消 ¥{{ money(minSpend) }}（仅展示提示）· 已点 ¥{{ money(pos.receivable) }}
      </div>
      <div class="text-[12px] leading-tight" style="color: #f09595">低消不参与结算校验（V7 变更一）</div>
    </template>

    <div class="h-px" style="background: var(--dark2)" />

    <!-- 已点商品标题行 83:8 -->
    <div class="flex items-center text-[12px]" style="color: #aab2a0">
      <span>已点商品（{{ pos.billCount }} 件）</span>
      <button
        v-if="pos.cart.length"
        class="ml-auto hover:text-white"
        @click="pos.clearCart()"
      >清空</button>
    </div>

    <!-- 明细行 83:11（272×64 圆角 6，块底 = 侧栏当前色） -->
    <div class="flex-1 overflow-y-auto flex flex-col" style="gap: 8px">
      <div v-if="!pos.cart.length" class="text-[12px] text-center py-6" style="color: #8f967f">
        尚未点单
      </div>

      <div
        v-for="(l, i) in pos.cart"
        :key="i"
        class="rounded-md"
        style="background: var(--dark2); padding: 10px"
      >
        <div class="text-[13px] text-white leading-snug break-all">{{ l.goodsName }}</div>
        <div class="mt-1 text-[11px] leading-tight" style="color: #aab2a0">
          {{ l.specText || '默认' }} ×{{ l.qty }}<span v-if="l.remark"> · 备注：{{ l.remark }}</span>
        </div>
        <div class="mt-1 flex items-center">
          <span class="text-[11px]" style="color: #c0dd97">小计 ¥{{ money(l.price * l.qty) }}</span>
          <button
            class="ml-auto text-[11px] hover:text-white"
            style="color: #8f967f"
            title="移除该行"
            @click="pos.changeQty(i, -l.qty)"
          >✕</button>
        </div>
      </div>
    </div>

    <!-- 整单备注（2026-09-23 从「做法与备注区」移来）：
         整单备注是**订单级**信息，与结算同层；做法区只保留**单品级**的单品备注，避免两处填错 -->
    <div class="shrink-0">
      <div class="text-[12px] mb-2" style="color: #aab2a0">整单备注</div>
      <input
        v-model="pos.remark"
        class="w-full rounded text-[12px] outline-none"
        style="height: 30px; padding: 0 10px; background: var(--dark2); border: 1px solid #3a4230; color: #fff"
        placeholder="如：打包两杯分开装"
      />
      <div class="mt-2 flex flex-wrap" style="gap: 6px">
        <button
          v-for="q in QUICK_REMARKS"
          :key="q"
          class="rounded-[12px] border text-[11px]"
          style="height: 24px; padding: 0 8px; border-color: #3a4230; color: #aab2a0"
          @click="appendRemark(q)"
        >{{ q }}</button>
      </div>
    </div>

    <!-- 优惠 / 抹零 83:20（本期恒为 0） -->
    <div class="flex items-center text-[12px]" style="color: #aab2a0">
      <span>优惠 / 抹零</span>
      <span class="ml-auto">-¥0.00</span>
    </div>

    <!-- 应收行 83:23 -->
    <div class="flex items-center">
      <span class="text-[13px] text-white">应收</span>
      <span class="ml-auto text-[20px] font-medium" style="color: #c0dd97">¥{{ money(pos.receivable) }}</span>
    </div>

    <!-- 支付方式 92:1~92:7（画板仅两项：微信收款码 / 现金） -->
    <div>
      <div class="text-[12px] mb-2" style="color: #aab2a0">支付方式</div>
      <div class="flex items-center gap-2">
        <button
          v-for="m in PAY_MODES"
          :key="m"
          class="h-[30px] rounded-[15px] border text-[12px]"
          :style="payMode === m
            ? 'background: var(--p); border-color: var(--p); color: #fff'
            : 'background: transparent; border-color: #3a4230; color: #aab2a0'"
          :class="m === '微信收款码' ? 'flex-1' : ''"
          @click="payMode = m"
        >{{ m }}</button>
      </div>
    </div>

    <!-- 实收（可改）92:9~92:11 -->
    <div class="rounded flex items-center" style="height: 32px; border: 1px solid #3a4230; padding: 0 10px">
      <span class="text-[12px]" style="color: #c0dd97">实收 ¥</span>
      <input
        v-model="received"
        class="flex-1 min-w-0 bg-transparent text-[12px] outline-none"
        style="color: #c0dd97"
        inputmode="decimal"
      />
      <span class="text-[11px] ml-2" style="color: #8f967f">可改</span>
    </div>

    <!-- 确认收款 83:26（272×44 圆角 4） -->
    <button
      class="h-11 rounded text-[14px] font-medium text-white disabled:opacity-50"
      style="background: var(--p)"
      :disabled="submitting || !pos.cart.length"
      @click="onConfirm"
    >{{ submitting ? '提交中…' : `确认收款 ¥${money(pos.receivable)}` }}</button>

    <!-- 挂单 92:12（未收款不下厨） -->
    <button
      class="h-[30px] rounded text-[12px]"
      style="border: 1px solid #3a4230; color: #aab2a0"
      @click="pos.holdOrder()"
    >挂单（暂存，未收款不下厨）</button>

    <!-- 底注 83:28 -->
    <div class="text-[11px] leading-relaxed" style="color: #8f967f">
      {{ pos.isTakeaway
        ? '收款后：订单→待处理 · 顾客凭取餐号取餐 · 自动打印小票（P-03）'
        : '收款后：订单→已完成 · 台位→待清台 · 自动打印结账小票（P-03）' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { usePosStore } from '@/stores/pos'

const pos = usePosStore()

/** 支付方式：画板仅这两项（92:3 / 92:7） */
const PAY_MODES = ['微信收款码', '现金'] as const
const payMode = ref<(typeof PAY_MODES)[number]>('微信收款码')

/** 常用备注（2026-09-23 随整单备注从做法区移来） */
const QUICK_REMARKS = ['多加冰', '果盘后上', '拼桌']

/** 常用备注追加到整单备注（去重） */
function appendRemark(text: string) {
  const parts = pos.remark.split(/[，,]/).map(s => s.trim()).filter(Boolean)
  if (parts.includes(text)) return
  parts.push(text)
  pos.remark = parts.join('，')
}

const submitting = ref(false)
const received = ref('0.00')

const minSpend = computed(() => pos.currentTable?.minSpend ?? 0)

/** 台位行：「散台 A03 · 就餐 4 人」（83:2）；外带自提单无台位（2026-09-23 补） */
const tableLine = computed(() => {
  if (pos.isTakeaway) return '外带自提 · 免台位'
  const t = pos.currentTable
  if (!t) return '未选台位'
  return `${t.typeName} ${t.tableNo} · 就餐 ${t.session?.memberNum ?? 0} 人`
})

/** 低消达标进度（仅展示） */
const progress = computed(() => {
  if (minSpend.value <= 0) return 0
  return Math.min(100, Math.round((pos.receivable / minSpend.value) * 100))
})

// 应收变化时同步实收默认值
watch(
  () => pos.receivable,
  v => {
    received.value = v.toFixed(2)
  },
  { immediate: true }
)

/**
 * 确认收款
 * ⚠️ 批次 B 仅落地界面与订单提交：微信收款码（Native 扫码）在下单后发起，
 *    真实支付通道属**批次 H**（商户号尚未就绪）。此处先走既有的下单接口，
 *    待批次 H 接入后改为「下单 → 拉起收款码 → 回调置 is_payment=true」。
 */
async function onConfirm() {
  submitting.value = true
  try {
    const ok = await pos.sendItems()
    if (ok) {
      alert(`已下单 ¥${money(Number(received.value) || pos.receivable)}\n\n微信收款码支付将在批次 H 接入（商户号就绪后）`)
    }
  } finally {
    submitting.value = false
  }
}

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
