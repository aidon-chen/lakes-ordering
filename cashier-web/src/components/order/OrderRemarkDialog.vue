<template>
  <!--
    改单备注弹窗（2026-09-23 新增）
    用途：出品看板「待制作 / 出品中」两列的「改备注」入口 —— 出品前/出品中发现备注不对可当场改。
    改两层：整单备注（tb_order.remark）+ 逐条单品备注（tb_order_detail.remark）。
  -->
  <Teleport to="body">
    <div
      class="flex items-center justify-center"
      style="position: fixed; inset: 0; z-index: 60; background: rgba(0, 0, 0, .45)"
      @click.self="emit('close')"
    >
      <div class="bg-white rounded-lg flex flex-col" style="width: 520px; max-width: 92vw; max-height: 86vh; padding: 16px; gap: 12px">
        <div class="shrink-0 flex items-baseline" style="gap: 8px">
          <span class="text-[15px] font-medium" style="color: var(--tc-title)">改备注</span>
          <span class="text-[12px]" style="color: var(--tc-sub)">{{ pickupCodeText(orderNo) }} · {{ shopName }}</span>
          <span class="ml-auto text-[13px] cursor-pointer" style="color: var(--tc-sub)" @click="emit('close')">✕</span>
        </div>

        <div class="flex-1 min-h-0 overflow-y-auto flex flex-col" style="gap: 14px">
          <div class="flex flex-col" style="gap: 6px">
            <span class="text-[13px]" style="color: var(--tc-title)">整单备注</span>
            <textarea
              v-model="orderRemark"
              rows="2"
              placeholder="如：打包两杯分开装"
              class="w-full rounded text-[12px]"
              style="border: 1px solid var(--tc-line); padding: 8px; resize: vertical; color: var(--txt)"
            />
          </div>

          <div class="flex flex-col" style="gap: 8px">
            <span class="text-[13px]" style="color: var(--tc-title)">单品备注</span>
            <div v-if="!rows.length" class="text-[12px]" style="color: var(--tc-sub)">该单无明细</div>
            <div v-for="r in rows" :key="r.id" class="flex flex-col" style="gap: 4px">
              <span class="text-[12px]" style="color: var(--txt)">
                {{ r.goodsName || '商品' }} ×{{ r.number }}
                <span v-if="r.specText" style="color: var(--tc-sub)"> · {{ r.specText }}</span>
              </span>
              <input
                v-model="r.remark"
                type="text"
                placeholder="该菜品的备注，如：少冰"
                class="w-full rounded text-[12px]"
                style="border: 1px solid var(--tc-line); padding: 6px 8px; color: var(--txt)"
              />
            </div>
          </div>
        </div>

        <div class="shrink-0 flex items-center" style="gap: 8px; border-top: 1px solid #E5E7EB; padding-top: 12px">
          <span class="text-[12px]" style="color: var(--tc-sub)">保存后立即生效，顾客端订单详情可见</span>
          <span class="flex-1" />
          <button class="rounded text-[12px]" style="height: 28px; padding: 0 12px; border: 1px solid var(--tc-line); color: var(--tc-gray)" @click="emit('close')">取消</button>
          <button
            class="rounded text-[12px] text-white disabled:opacity-50"
            style="height: 28px; padding: 0 14px; background: var(--p)"
            :disabled="saving"
            @click="save"
          >{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { pickupCodeText, updateOrderRemark, type OrderDetailRow } from '@/api/order'

const props = defineProps<{
  orderId: number
  /** 取餐号，用于标题展示（显示为 A-00N） */
  orderNo: number | null
  shopName?: string | null
  /** 当前整单备注 */
  remark: string | null
  details: OrderDetailRow[]
}>()

const emit = defineEmits<{ (e: 'close'): void; (e: 'done'): void }>()

const orderRemark = ref(props.remark ?? '')
const saving = ref(false)

/** 逐条明细的备注编辑行（specList 走与详情页一致的可读化） */
const rows = ref(
  props.details.map(d => ({
    id: d.id,
    goodsName: d.goodsName,
    number: d.number,
    specText: specTextOf(d),
    remark: d.remark ?? ''
  }))
)

function specTextOf(d: OrderDetailRow): string {
  if (!d.specList) return ''
  try {
    const o = JSON.parse(d.specList)
    return Object.keys(o)
      .filter(k => !/\d+$/.test(k))
      .map(k => String(o[k]))
      .filter(Boolean)
      .join('·')
  } catch {
    return ''
  }
}

async function save() {
  saving.value = true
  try {
    await updateOrderRemark({
      id: props.orderId,
      remark: orderRemark.value.trim() || null,
      // 只提交备注有变化的行，减少无谓写入
      details: rows.value
        .filter(r => (r.remark ?? '').trim() !== (props.details.find(d => d.id === r.id)?.remark ?? '').trim())
        .map(r => ({ id: r.id, remark: r.remark.trim() || null }))
    })
    emit('done')
    emit('close')
  } catch (e) {
    console.error('[remark] 保存失败', e)
    alert((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
