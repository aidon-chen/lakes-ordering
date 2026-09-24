<template>
  <!--
    W-27 会员充值（现金）
    左：充值表单卡 —— 手机号定位会员 → 填实收现金 → 可勾选赠送 → 提交
    右：本次充值预览 / 提交结果卡
    说明：顾客到店付现金办卡或储值，店员当场录入；不走微信支付。
  -->
  <div class="flex-1 overflow-auto p-5 flex gap-4 min-w-0">
    <!-- ===================== 左：充值表单卡 ===================== -->
    <div
      class="rounded-lg flex flex-col shrink-0"
      style="width: 700px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px"
    >
      <span class="text-[16px] font-medium" style="color: var(--tc-title)">会员充值（现金）· W-27</span>
      <span class="text-[12px]" style="color: var(--tc-sub)">
        手机号定位会员 → 填写本次实收现金 → 提交后余额立即到账。不收现金请走小程序充值面额流程。
      </span>

      <!-- 行1：顾客手机号 + 查询 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">顾客手机号</span>
        <input
          v-model.trim="form.memberPhone"
          class="rounded border text-[12px] outline-none"
          style="width: 220px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="11 位手机号"
          @keyup.enter="doLookup"
        />
        <button
          class="rounded text-[12px] text-white disabled:opacity-50"
          style="width: 96px; height: 36px; background: var(--p)"
          :disabled="looking || !/^1\d{10}$/.test(form.memberPhone)"
          @click="doLookup"
        >{{ looking ? '查询中…' : '查询' }}</button>
        <span v-if="member" class="text-[12px]" style="color: #008000">
          已带出：{{ member.nickname || member.name || '会员' }} · {{ member.mobile }}
        </span>
        <span v-else-if="lookupErr" class="text-[12px]" style="color: var(--warn)">{{ lookupErr }}</span>
      </div>

      <!-- 行2：会员当前状态（查到后才有值） -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">当前余额</span>
        <span class="text-[18px] font-medium" style="color: var(--p)">
          ¥ {{ member ? money(member.balance) : '—' }}
        </span>
        <span v-if="member" class="text-[12px]" style="color: var(--tc-sub)">
          {{ member.type === 2 ? 'VIP 会员' : '普通用户' }} · 成长值 {{ member.growth ?? 0 }}
          <template v-if="member.vipNo"> · 卡号 {{ member.vipNo }}</template>
        </span>
        <span v-else class="text-[12px]" style="color: var(--tc-sub)">请先查询会员</span>
      </div>

      <!-- 行3：本次实收现金 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">实收现金</span>
        <input
          v-model.number="form.amount"
          type="number"
          min="0.01"
          step="0.01"
          class="rounded border text-[12px] outline-none"
          style="width: 180px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="如 500"
        />
        <span class="text-[12px]" style="color: var(--tc-sub)">元（单笔上限 5000）</span>
      </div>

      <!-- 行4：赠送（手工勾选） -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">赠送余额</span>
        <label class="flex items-center text-[12px]" style="gap: 6px; color: var(--txt)">
          <input v-model="form.withGive" type="checkbox" />
          本次赠送
        </label>
        <input
          v-model.number="form.giveBalance"
          type="number"
          min="0"
          step="0.01"
          class="rounded border text-[12px] outline-none disabled:opacity-40"
          style="width: 160px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="赠送金额"
          :disabled="!form.withGive"
        />
        <span class="text-[12px]" style="color: var(--tc-sub)">元（不勾选则不赠送）</span>
      </div>

      <!-- 行5：备注 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">备注</span>
        <input
          v-model.trim="form.remark"
          class="rounded border text-[12px] outline-none"
          style="width: 420px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="选填，如：开业活动充 500 送 50"
        />
      </div>

      <!-- 提交行 -->
      <div class="flex items-center" style="gap: 12px; margin-top: 4px">
        <button
          class="rounded text-[13px] text-white disabled:opacity-50"
          style="width: 160px; height: 42px; background: var(--p)"
          :disabled="!canSubmit"
          @click="doSubmit"
        >{{ submitting ? '提交中…' : '确认收款并充值' }}</button>
        <button
          class="rounded text-[13px] disabled:opacity-50"
          style="width: 110px; height: 42px; background: #fff; border: 1px solid var(--tc-line); color: var(--txt)"
          :disabled="submitting"
          @click="resetForm"
        >重置</button>
        <span v-if="submitErr" class="text-[12px]" style="color: var(--warn)">{{ submitErr }}</span>
      </div>
    </div>

    <!-- ===================== 右：预览 / 结果卡 ===================== -->
    <div
      class="rounded-lg flex flex-col shrink-0"
      style="width: 474px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 14px"
    >
      <span class="text-[16px] font-medium" style="color: var(--tc-title)">{{ result ? '充值完成' : '本次充值预览' }}</span>

      <!-- 结果态 -->
      <template v-if="result">
        <div class="flex flex-col" style="gap: 10px">
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">充值单号</span>
            <span style="color: var(--txt)">{{ result.orderNo }}</span>
          </div>
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">实收现金</span>
            <span style="color: var(--txt)">¥ {{ money(result.amount) }}</span>
          </div>
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">赠送</span>
            <span style="color: var(--txt)">¥ {{ money(result.giveBalance) }}</span>
          </div>
          <div class="flex justify-between text-[14px]">
            <span style="color: var(--txt)">充值后余额</span>
            <span class="font-medium" style="color: var(--p)">¥ {{ money(result.balanceAfter) }}</span>
          </div>
        </div>
        <span class="text-[12px]" style="color: var(--tc-sub)">
          已写入充值记录与余额流水，可在管理端「会员充值记录」核对（渠道=商家端）。
        </span>
        <button
          class="rounded text-[13px] text-white"
          style="width: 160px; height: 42px; background: var(--p)"
          @click="resetForm"
        >继续下一笔</button>
      </template>

      <!-- 预览态 -->
      <template v-else>
        <div class="flex flex-col" style="gap: 10px">
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">会员</span>
            <span style="color: var(--txt)">
              {{ member ? (member.nickname || member.name || '会员') + ' · ' + member.mobile : '—' }}
            </span>
          </div>
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">现有余额</span>
            <span style="color: var(--txt)">{{ member ? '¥ ' + money(member.balance) : '—' }}</span>
          </div>
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">本次实收</span>
            <span style="color: var(--txt)">¥ {{ money(form.amount) }}</span>
          </div>
          <div class="flex justify-between text-[12px]">
            <span style="color: var(--tc-sub)">赠送</span>
            <span style="color: var(--txt)">¥ {{ money(giveAmount) }}</span>
          </div>
          <div class="flex justify-between text-[14px]">
            <span style="color: var(--txt)">充值后余额</span>
            <span class="font-medium" style="color: var(--p)">
              {{ member ? '¥ ' + money(previewBalanceAfter) : '—' }}
            </span>
          </div>
        </div>
        <span class="text-[12px]" style="color: var(--tc-sub)">
          提交前请当面点清现金；提交后余额立即到账，如需撤销请在管理端走人工流程。
        </span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { lookupMemberForRecharge, cashRecharge, money, type MerchantMemberForRecharge, type CashRechargeResult } from '@/api/member'

/** 单笔上限，与服务端 MerchantMemberController.MAX_SINGLE_AMOUNT 保持一致 */
const MAX_SINGLE_AMOUNT = 5000

const form = ref({
  memberPhone: '',
  amount: undefined as number | undefined,
  withGive: false,
  giveBalance: undefined as number | undefined,
  remark: ''
})
const member = ref<MerchantMemberForRecharge | null>(null)
const looking = ref(false)
const lookupErr = ref('')
const submitting = ref(false)
const submitErr = ref('')
const result = ref<CashRechargeResult | null>(null)

/** 实际赠送额：未勾选一律按 0 计 */
const giveAmount = computed(() => (form.value.withGive ? Number(form.value.giveBalance) || 0 : 0))

const previewBalanceAfter = computed(() => (Number(member.value?.balance) || 0) + (Number(form.value.amount) || 0) + giveAmount.value)

const canSubmit = computed(() => {
  const amt = Number(form.value.amount) || 0
  return !!member.value && amt > 0 && amt <= MAX_SINGLE_AMOUNT && giveAmount.value >= 0 && !submitting.value
})

async function doLookup() {
  if (!/^1\d{10}$/.test(form.value.memberPhone)) return
  looking.value = true
  lookupErr.value = ''
  member.value = null
  try {
    member.value = await lookupMemberForRecharge(form.value.memberPhone)
  } catch (e) {
    lookupErr.value = e instanceof Error ? e.message : '查询失败'
  } finally {
    looking.value = false
  }
}

async function doSubmit() {
  if (!canSubmit.value || !member.value) return
  if (form.value.withGive && giveAmount.value < 0) {
    submitErr.value = '赠送金额不能为负数'
    return
  }
  submitting.value = true
  submitErr.value = ''
  try {
    result.value = await cashRecharge({
      memberId: member.value.id,
      amount: Number(form.value.amount),
      giveBalance: giveAmount.value,
      remark: form.value.remark
    })
  } catch (e) {
    submitErr.value = e instanceof Error ? e.message : '充值失败'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.value = { memberPhone: '', amount: undefined, withGive: false, giveBalance: undefined, remark: '' }
  member.value = null
  lookupErr.value = ''
  submitErr.value = ''
  result.value = null
}
</script>
