<template>
  <!--
    W-17 存酒登记（Ardot 2:1102，内容区 2:1105）
    左：登记-表单卡 700×812 r8（padding 20, gap 12，6 行 + 按钮行 42）
    右：存酒凭证卡 474×812 r8（padding 20, gap 14：凭证头 74 / 5 行信息）
  -->
  <div class="flex-1 overflow-auto p-5 flex gap-4 min-w-0">
    <!-- ===================== 左：登记表单卡 ===================== -->
    <div class="rounded-lg flex flex-col shrink-0" style="width: 700px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
      <span class="text-[16px] font-medium" style="color: var(--tc-title)">存酒登记（M-06）</span>

      <!-- 行1：顾客手机号 + 查询（查询后带出会员，避免填完才报错） -->
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
          已带出：{{ member.nickname || member.name || '会员' }} · {{ memberTag }}
          <span v-if="memberBalance !== null" style="color: var(--p)"> · 余额 ¥{{ memberBalance }}</span>
        </span>
        <span v-else-if="lookupErr" class="text-[12px]" style="color: var(--warn)">{{ lookupErr }}</span>
      </div>

      <!-- 行2：商品品名 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">商品品名</span>
        <input
          v-model.trim="form.goodsName"
          class="rounded border text-[12px] outline-none"
          style="width: 300px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="如：单一麦芽威士忌 12 年（整瓶）"
        />
      </div>

      <!-- 行3：剩余量 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">剩余量</span>
        <input
          v-model.number="form.bottleCount"
          type="number"
          :min="1"
          class="rounded border text-[12px] outline-none"
          style="width: 160px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="瓶数"
        />
        <span class="text-[12px]" style="color: var(--tc-sub)">瓶（存入即剩余，取酒时扣减）</span>
      </div>

      <!-- 行4：存放位置 -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">存放位置</span>
        <input
          v-model.trim="form.location"
          class="rounded border text-[12px] outline-none"
          style="width: 300px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          placeholder="如：A 区 · 03 柜 · 2 层"
        />
      </div>

      <!-- 行5：有效期至（画板是日期；后端收 expireDays，故由日期反算天数） -->
      <div class="flex items-center" style="gap: 10px; height: 36px">
        <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">有效期至</span>
        <input
          v-model="expireDate"
          type="date"
          class="rounded border text-[12px] outline-none"
          style="width: 220px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
        />
        <span class="text-[12px]" style="color: var(--tc-sub)">默认 90 天（{{ expireDays }} 天）</span>
      </div>

      <!-- 行6：备注 -->
      <div class="flex" style="gap: 10px">
        <span class="text-[12px] shrink-0" style="width: 90px; padding-top: 8px; color: var(--txt)">备注</span>
        <textarea
          v-model.trim="form.remark"
          class="rounded border text-[12px] outline-none"
          style="width: 500px; height: 60px; padding: 8px 10px; border-color: var(--tc-line); resize: none"
          placeholder="选填，如：瓶身有标记"
        />
      </div>

      <!-- 按钮行 -->
      <div class="flex items-center" style="gap: 12px; height: 42px; margin-top: 4px">
        <button
          class="rounded text-[13px] font-medium text-white disabled:opacity-50"
          style="width: 160px; height: 42px; background: var(--p)"
          :disabled="!canSubmit || submitting"
          @click="doCreate()"
        >{{ submitting ? '保存中…' : '登记并存酒' }}</button>
        <button
          class="rounded border text-[13px] disabled:opacity-40"
          style="width: 190px; height: 42px; border-color: var(--tc-line); color: var(--tc-gray)"
          disabled
          title="打印属批次 G（P-04 打印链路），本期未实现"
        >登记并打印（待批次 G）</button>
      </div>

      <p class="text-[11px]" style="color: var(--tc-hold)">
        登记前请先用「查询」确认手机号对应会员存在。「登记并打印」需批次 G 的打印链路（云打印账号），本期不放可用按钮。
      </p>
    </div>

    <!-- ===================== 右：存酒凭证卡 ===================== -->
    <div class="rounded-lg flex flex-col shrink-0" style="width: 474px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 14px">
      <span class="text-[16px] font-medium" style="color: var(--tc-title)">存酒凭证</span>

      <!-- 凭证头（方案 v1.1：登记不再生成存酒码，取酒码在「申请 / 代发起」时生成） -->
      <div class="rounded-md flex flex-col items-center justify-center" style="height: 74px; background: #EDF2DE; gap: 4px">
        <span class="text-[18px] font-bold" style="color: var(--tc-title)">{{ form.goodsName || '登记后生成凭证' }}</span>
        <span class="text-[12px]" style="color: var(--tc-sub)">顾客可在小程序「我的存酒」申请取酒，也可由店员代发起</span>
      </div>

      <!-- 信息 -->
      <div v-for="kv in previewPairs" :key="kv[0]" class="flex text-[12px]" style="height: 19px">
        <span style="color: var(--tc-sub)">{{ kv[0] }}</span>
        <span class="ml-auto truncate" style="color: var(--txt); max-width: 300px">{{ kv[1] }}</span>
      </div>

      <p v-if="!created" class="text-[11px]" style="color: var(--tc-hold)">
        以上为登记前预览；保存后即写入会员存酒账户，无需再出码。
      </p>
      <div v-else class="flex items-center" style="gap: 10px">
        <span class="text-[12px]" style="color: #008000">✅ 已登记并存酒</span>
        <button class="text-[12px]" style="color: var(--p)" @click="goList">去存酒列表 →</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createWine, lookupMemberByMobile, type MerchantMemberBrief } from '@/api/wine'

const router = useRouter()

const form = ref({ memberPhone: '', goodsName: '', bottleCount: 1 as number, location: '', remark: '' })
const member = ref<MerchantMemberBrief | null>(null)
const looking = ref(false)
const lookupErr = ref('')
const submitting = ref(false)
const created = ref(false)

/** 默认 90 天，与后端 DEFAULT_EXPIRE_DAYS 一致 */
const DEFAULT_DAYS = 90
const expireDate = ref(toDateInput(addDays(new Date(), DEFAULT_DAYS)))

const memberTag = computed(() => (member.value?.mobile ? member.value.mobile : '会员'))

/** 账户余额（后端 selectByMobile 自 2026-09-24 起下发；为空时不渲染该段，避免出现「余额 ¥NaN」） */
const memberBalance = computed(() =>
  member.value && member.value.balance != null ? Number(member.value.balance).toFixed(2) : null
)

/** 有效期天数由所选日期反算（后端收 expireDays，不收到期日） */
const expireDays = computed(() => {
  const d = parseDateInput(expireDate.value)
  if (!d) return DEFAULT_DAYS
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(1, Math.round((d.getTime() - today.getTime()) / 86400000))
})

const canSubmit = computed(
  () => !!member.value && !!form.value.goodsName && (form.value.bottleCount ?? 0) >= 1 && expireDays.value >= 1
)

const previewPairs = computed<[string, string][]>(() => [
  ['顾客', member.value ? `${member.value.nickname || member.value.name || '会员'} · ${member.value.mobile || ''}` : '—（先查询手机号）'],
  ['品名', form.value.goodsName || '—'],
  ['剩余量', form.value.bottleCount ? `${form.value.bottleCount} 瓶` : '—'],
  ['存放位置', form.value.location || '—'],
  ['有效期至', expireDate.value || '—']
])

async function doLookup() {
  lookupErr.value = ''
  member.value = null
  created.value = false
  looking.value = true
  try {
    member.value = await lookupMemberByMobile(form.value.memberPhone)
  } catch (e) {
    lookupErr.value = (e as Error).message || '查询失败'
  } finally {
    looking.value = false
  }
}

async function doCreate() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await createWine({
      memberPhone: form.value.memberPhone,
      goodsName: form.value.goodsName,
      bottleCount: Number(form.value.bottleCount) || 1,
      location: form.value.location || undefined,
      remark: form.value.remark || undefined,
      expireDays: expireDays.value
    })
    created.value = true
  } catch (e) {
    console.error('[wine-create] 登记失败', e)
    alert((e as Error).message || '登记失败')
  } finally {
    submitting.value = false
  }
}

function goList() {
  router.push('/wine/list')
}

/* ---------- 日期工具（避开时区陷阱，纯 yyyy-MM-dd 处理） ---------- */
function addDays(base: Date, days: number): Date {
  const d = new Date(base)
  d.setDate(d.getDate() + days)
  return d
}
function toDateInput(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
function parseDateInput(v: string): Date | null {
  const m = String(v ?? '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return null
  const d = new Date(`${m[1]}-${m[2]}-${m[3]}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d
}
</script>
