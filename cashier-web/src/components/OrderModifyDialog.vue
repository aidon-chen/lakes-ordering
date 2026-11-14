<template>
  <!--
    W-13 改单弹窗（Ardot 2:932，540×600）
    标题「改单 · SO…」+ 明细行（品名 + 数量）+「＋ 添加新品（加酒）」+ 说明 + 合计行 + 取消/保存改单
    ⚠️ 与画板一致的关键口径：「改单不影响已支付金额；差额走补收 / 退款，并触发新小票」
       —— 本项目一期**只记录差额**（待补收/待退），不触发真实收退款；小票属批次 G，另行接入。
  -->
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="close">
      <div class="rounded-lg bg-white flex flex-col" style="width: min(540px, calc(100vw - 32px)); max-height: 600px">
        <!-- 标题行 -->
        <div class="flex items-baseline shrink-0" style="padding: 20px 20px 0">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">改单 · {{ orderNo }}</span>
          <button class="ml-auto text-[14px]" style="color: var(--tc-sub)" @click="close">✕</button>
        </div>

        <!-- 明细区（可滚动） -->
        <div class="flex-1 min-h-0 overflow-y-auto flex flex-col" style="padding: 16px 20px; gap: 8px">
          <div
            v-for="(row, i) in rows"
            :key="row.detailId ?? 'new-' + i"
            class="rounded flex items-center"
            style="min-height: 48px; padding: 8px 12px; background: #F5F7FA; gap: 10px"
          >
            <span class="text-[13px] flex-1 truncate" style="color: var(--txt)">{{ row.goodsName || '—' }}</span>
            <span class="text-[12px] shrink-0" style="color: var(--tc-sub)">¥{{ money(row.price) }}</span>
            <button
              class="rounded border text-[13px] shrink-0"
              style="width: 26px; height: 26px; border-color: var(--tc-line); color: var(--txt)"
              @click="row.number = Math.max(0, row.number - 1)"
            >−</button>
            <input
              v-model.number="row.number"
              type="number"
              :min="0"
              class="rounded border text-[13px] text-center outline-none shrink-0"
              style="width: 52px; height: 26px; border-color: var(--tc-line)"
            />
            <button
              class="rounded border text-[13px] shrink-0"
              style="width: 26px; height: 26px; border-color: var(--tc-line); color: var(--txt)"
              @click="row.number = (row.number || 0) + 1"
            >＋</button>
            <button class="text-[12px] shrink-0" style="color: var(--warn)" @click="removeRow(i)">删除</button>
          </div>

          <!-- 添加新品（加酒） -->
          <div class="rounded flex items-center" style="height: 44px; padding: 0 12px; border: 1px dashed var(--tc-line); gap: 8px">
            <span class="text-[13px] shrink-0" style="color: var(--p)">＋ 添加新品（加酒）</span>
            <select
              v-model="pickGoodsId"
              class="rounded border text-[12px] outline-none flex-1"
              style="height: 30px; padding: 0 8px; border-color: var(--tc-line)"
              @change="addGoods"
            >
              <option :value="null">选择商品…</option>
              <option v-for="g in goodsOptions" :key="g.id" :value="g.id">
                {{ g.name }} · ¥{{ money(g.salePrice || g.price) }}
              </option>
            </select>
          </div>

          <p class="text-[12px] mt-1" style="color: var(--tc-sub)">
            改单不影响已支付金额；差额走补收 / 退款，并触发新小票（一期只记录差额，不触发真实收退款）
          </p>
        </div>

        <!-- 合计行 + 按钮 -->
        <div class="shrink-0" style="padding: 0 20px 20px">
          <div class="flex items-center" style="height: 26px">
            <span class="text-[13px]" style="color: var(--txt)">改单后应收</span>
            <span class="ml-auto text-[15px] font-bold" style="color: var(--p)">
              ¥{{ money(afterAmount) }}<template v-if="diff !== 0">
                （{{ diff > 0 ? '补收' : '退款' }} ¥{{ money(Math.abs(diff)) }}）</template>
            </span>
          </div>
          <p v-if="err" class="text-[12px] mt-1" style="color: var(--warn)">{{ err }}</p>
          <div class="flex justify-end mt-3" style="gap: 12px">
            <button class="rounded border text-[13px]" style="height: 36px; padding: 0 20px; border-color: var(--tc-line); color: var(--tc-gray)" @click="close">取消</button>
            <button
              class="rounded text-[13px] font-medium text-white disabled:opacity-50"
              style="height: 36px; padding: 0 22px; background: var(--p)"
              :disabled="saving || !rows.length"
              @click="save"
            >{{ saving ? '保存中…' : '保存改单' }}</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchGoods, type GoodsRow } from '@/api/menu'
import { modifyOrder, type ChangeItem } from '@/api/orderChange'
import type { OrderDetailRow } from '@/api/order'

const props = defineProps<{
  visible: boolean
  orderId: number
  orderNo: string
  /** 订单当前应收（用于算差额） */
  actualPrice: number
  details: OrderDetailRow[]
}>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'done', diff: number): void }>()

interface Row extends ChangeItem {
  goodsName: string | null
  mainImage?: string | null
  specList?: string | null
  price: number
}

const rows = ref<Row[]>([])
const goodsOptions = ref<GoodsRow[]>([])
const pickGoodsId = ref<number | null>(null)
const saving = ref(false)
const err = ref('')

/** 改单后应收 = 原应收 + Σ(新小计 − 旧小计)，与服务端口径一致 */
const afterAmount = computed(() => {
  const newTotal = rows.value.reduce((s, r) => s + (Number(r.price) || 0) * (Number(r.number) || 0), 0)
  const oldTotal = props.details.reduce((s, d) => s + (Number(d.subtotal) || 0), 0)
  return (Number(props.actualPrice) || 0) + newTotal - oldTotal
})
const diff = computed(() => afterAmount.value - (Number(props.actualPrice) || 0))

onMounted(loadGoods)

async function loadGoods() {
  try {
    const page = await fetchGoods()
    goodsOptions.value = page?.records ?? []
  } catch (e) {
    console.error('[modify] 商品加载失败', e)
  }
}

/** 打开时由父组件调用（父组件用 v-if 控制挂载，这里做一次快照） */
function init() {
  rows.value = props.details.map(d => ({
    detailId: d.id,
    goodsId: d.goodsId,
    goodsName: d.goodsName,
    mainImage: d.mainImage ?? '',
    specList: d.specList ?? null,
    price: Number(d.price) || 0,
    number: Number(d.number) || 0
  }))
  err.value = ''
}
defineExpose({ init })

function addGoods() {
  const g = goodsOptions.value.find(x => x.id === pickGoodsId.value)
  if (!g) return
  rows.value.push({
    detailId: null,
    goodsId: g.id,
    goodsName: g.name,
    mainImage: g.mainImage ?? '',
    specList: g.specList ?? null,
    price: Number(g.salePrice || g.price) || 0,
    number: 1
  })
  pickGoodsId.value = null
}

function removeRow(i: number) {
  rows.value.splice(i, 1)
}

async function save() {
  if (saving.value) return
  saving.value = true
  err.value = ''
  try {
    const res = await modifyOrder(
      props.orderId,
      rows.value.map(r => ({
        detailId: r.detailId ?? null,
        goodsId: r.goodsId ?? null,
        goodsName: r.goodsName,
        mainImage: r.mainImage ?? '',
        specList: r.specList ?? null,
        price: r.price,
        number: Number(r.number) || 0
      }))
    )
    emit('done', Number(res?.diffAmount ?? 0))
    emit('close')
  } catch (e) {
    err.value = (e as Error).message || '改单失败'
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
