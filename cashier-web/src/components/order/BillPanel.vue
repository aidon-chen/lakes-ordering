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

    <!-- 底注 83:28：口径＝「点单即收款」（先付后吃，2026-09-24 拍板并替换画板原「结账」口径） -->
    <div class="text-[11px] leading-relaxed" style="color: #8f967f">
      {{ pos.isTakeaway
        ? '确认收款后：订单→待处理 · 顾客凭取餐号取餐 · 自动打印小票（P-03）'
        : '确认收款后：订单→待处理（下厨）· 台位保持占用 · 用餐结束点「清台」' }}
    </div>
  </div>

  <!-- 扫码收款弹层（2026-09-24）：与面板平级的多根节点，position:fixed 脱离文档流，
       避免父级 flex 布局受影响；也避开面板 overflow:hidden 的裁剪 -->
  <ScanPayDialog
    :visible="scanVisible"
    :amount="scanAmount"
    :busy="scanBusy"
    :message="scanMessage"
    :message-kind="scanMessageKind"
    @close="closeScan"
    @scan="onScan"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { usePosStore } from '@/stores/pos'
import ScanPayDialog from './ScanPayDialog.vue'

const pos = usePosStore()

/** 支付方式：画板仅这两项（92:3 / 92:7） */
const PAY_MODES = ['微信收款码', '现金'] as const
const payMode = ref<(typeof PAY_MODES)[number]>('微信收款码')

/**
 * 现金落库的 payment_mode（口径对齐 A-07 营收看板「payment_mode 为空视为到店支付=4」）
 * 1 = 微信支付：由后端扫码收款链路写死（不需要前端传），故此处只定义现金档
 */
const PAY_MODE_OFFLINE = 4

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
 * 确认收款（2026-09-24 改：由「只下单」改为真正的收款动作；同日再改为**分两档**）
 *
 * 口径＝「点单即收款」（先付后吃，用户 2026-09-24 拍板）：下单 + 收款一个动作完成；
 * 后端置 is_payment=1 + payment_mode + status 1→2（待处理=下厨）；台位保持占用，
 * 顾客离店由桌卡「清台」一步完成「结台 + 放台」。
 *
 * 两档支付方式（正好对应画板 92:3 / 92:7）：
 *   - 微信收款码 → 打开扫码弹层，扫顾客付款码走**真扣款**（后端 /pay/micropay）
 *   - 现金       → 人工确认记账（后端 confirmPayment，payment_mode=4 到店支付）
 *
 * ⚠️ 微信为什么必须扫码而不是记账：收银台是网页，调不了小程序专属的
 *    wx.requestPayment；付款码支付（被扫）是唯一能在网页侧发起真扣款的通道。
 *    真实扣款需要商户号（WXPAY_APPID / WXPAY_MCHID / WXPAY_MCHKEY）且已开通
 *    「付款码支付」产品权限；未配置时后端会返回明确提示，不会静默失败。
 *
 * ⚠️ 收款金额以后端统计为准：现金档用 paidAmount，扫码档用微信返回的 paidAmount。
 *    画板「实收（可改）」的找零/折扣差额本期不落库，仅作展示。
 */
async function onConfirm() {
  submitting.value = true
  try {
    // 先在清空购物车前记住应收（sendItems 会把 cart 清空 → receivable 归零）
    const amount = pos.receivable
    const res = await pos.sendItems()
    if (!res.ok) return

    if (payMode.value === '现金') {
      await payByCash(res.orderNo)
    } else {
      openScan(res.orderNo, amount)
    }
  } finally {
    submitting.value = false
  }
}

/* ==================== 现金档：人工确认记账 ==================== */

async function payByCash(orderNo?: string) {
  const data = await pos.confirmPayment({
    tableId: pos.isTakeaway ? null : (pos.currentTable?.id ?? null),
    orderNo: pos.isTakeaway ? (orderNo ?? null) : null,
    payMode: PAY_MODE_OFFLINE
  })
  if (!data) return
  if (data.paidCount === 0) {
    alert('已下单，但该单此前已确认收款，无需重复收款')
  } else {
    alert(
      `已收款 ¥${money(data.paidAmount)}（现金）\n\n` +
        (pos.isTakeaway
          ? '订单→待处理，顾客凭取餐号取餐'
          : '订单→待处理（已下厨）；用餐结束后请在桌卡点「清台」')
    )
  }
}

/* ==================== 微信档：扫码收款弹层 ==================== */

const scanVisible = ref(false)
const scanAmount = ref(0)
const scanBusy = ref(false)
const scanMessage = ref('')
const scanMessageKind = ref<'info' | 'warn' | 'error' | 'ok'>('info')
/** 本次要收的订单号（下单返回）；外带场景后端靠它定位订单 */
const scanOrderNo = ref<string | null>(null)

function openScan(orderNo: string | undefined, amount: number) {
  scanOrderNo.value = orderNo ?? null
  scanAmount.value = amount
  scanMessage.value = ''
  scanVisible.value = true
}

function closeScan() {
  if (scanBusy.value) return
  scanVisible.value = false
}

/** 扫码层提交：一个付款码只发一次请求，结果就地反馈（不清弹层，便于连续重扫） */
async function onScan(code: string) {
  scanBusy.value = true
  try {
    const res = await pos.scanPay({
      tableId: pos.isTakeaway ? null : (pos.currentTable?.id ?? null),
      orderNo: scanOrderNo.value,
      authCode: code
    })
    if (res.ok) {
      scanMessageKind.value = 'ok'
      scanMessage.value =
        `收款成功 ¥${money(res.paidAmount ?? scanAmount.value)}` +
        (res.transactionId ? `（微信流水 ${res.transactionId}）` : '')
      // 停留片刻让收银员看清结果，再自动关闭
      window.setTimeout(() => {
        if (scanMessageKind.value === 'ok') scanVisible.value = false
      }, 1000)
    } else if (res.status === 'USERPAYING') {
      // 顾客还在输密码：未收款，提示重扫（微信对同单号同参数幂等，重扫安全）
      scanMessageKind.value = 'warn'
      scanMessage.value = res.message || '顾客正在支付中，请稍候片刻后再次扫描同一付款码'
    } else {
      scanMessageKind.value = 'error'
      scanMessage.value = res.message || '收款失败，请重试或改用现金收款'
    }
  } finally {
    scanBusy.value = false
  }
}

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
