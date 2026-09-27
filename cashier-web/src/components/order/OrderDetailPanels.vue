<template>
  <!--
    订单详情「左半部分」三块 —— 头部卡 / 商品明细 / 备注（2026-09-23 抽出）
    来源：原 OrderDetailView.vue 左列（700px 列）模板，样式全部内联 + CSS 变量，故可原样搬运。

    用途（两处共用，保证以后改一处两处生效）：
      1) 既有页面 /orders/detail（左列）
      2) 出品进度看板 W-14 的「点卡片 → 二级弹窗」
  -->
  <div class="flex flex-col" style="gap: 12px">
    <!-- 订单头 -->
    <div class="rounded-lg flex items-center" style="height: 60px; padding: 0 16px; gap: 12px; background: var(--pl)">
      <span class="text-[26px] font-bold leading-none" style="color: var(--tc-title)">{{ firstNo }}</span>
      <span
        class="rounded text-[12px] leading-[24px] text-center"
        style="min-width: 60px; height: 24px; padding: 0 8px; background: var(--warn-bg, #FCF5ED); color: var(--warn)"
      >{{ orderStatusText(order.status) }}</span>
      <span class="text-[12px]" style="color: var(--tc-sub)">
        下单 {{ fmtTime(order.createTime) }} · {{ sceneText }}
      </span>
    </div>

    <!-- 明细卡 -->
    <div class="rounded" style="border: 1px solid var(--tc-line)">
      <div class="flex items-center text-[13px]" style="height: 40px; padding-left: 16px; background: var(--panel, #F5F7FA); color: var(--tc-title)">
        商品明细
      </div>
      <div class="flex items-center text-[12px]" style="height: 34px; background: var(--bg); color: var(--tc-title)">
        <span class="px-2 truncate" style="width: 240px">品名 / 规格</span>
        <span class="px-2 truncate" style="width: 80px">数量</span>
        <span class="px-2 truncate" style="width: 110px">单价</span>
        <span class="px-2 truncate" style="width: 110px">小计</span>
        <span class="px-2 truncate" style="flex: 1">备注</span>
      </div>

      <div v-if="!details.length" class="text-[12px]" style="padding: 12px 16px; color: var(--tc-sub)">无明细</div>
      <div
        v-for="d in details"
        v-else
        :key="d.id"
        class="flex items-center text-[12px]"
        style="height: 40px; border-top: 1px solid var(--tc-line); color: var(--txt)"
      >
        <!-- 品名/规格允许换行完整展示（09-25 拍板：规格显示不全 → 去掉单行截断；多行时行高 1.5 保持可读） -->
        <span class="px-2" style="width: 240px; white-space: normal; word-break: break-all; line-height: 1.5">{{ d.goodsName || '—' }}{{ specText(d) ? ' · ' + specText(d) : '' }}</span>
        <span class="px-2 truncate" style="width: 80px">{{ d.number ?? 1 }}</span>
        <span class="px-2 truncate" style="width: 110px">¥{{ money(d.price) }}</span>
        <span class="px-2 truncate" style="width: 110px">¥{{ money(d.subtotal) }}</span>
        <span class="px-2 truncate" style="flex: 1; color: var(--tc-sub)">{{ d.remark || '—' }}</span>
      </div>

      <div class="flex items-center text-[13px] font-medium" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span class="px-2" style="flex: 1">商品合计 ¥{{ money(order.goodsTotalPrice ?? order.actualPrice) }}</span>
        <span class="px-2" style="width: 120px">{{ order.goodsTotalQuantity ?? details.length }} 件</span>
      </div>
    </div>

    <!-- 备注卡 -->
    <div class="rounded" style="border: 1px solid var(--tc-line)">
      <div class="flex items-center text-[13px]" style="height: 36px; padding-left: 16px; background: var(--panel, #F5F7FA); color: var(--tc-title)">
        备注
      </div>
      <div class="flex flex-col text-[12px]" style="padding: 12px; gap: 6px">
        <span style="color: var(--txt)">整单：{{ order.remark || '—' }}</span>
        <span style="color: var(--txt)">单品：{{ itemRemarks || '—' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { orderStatusText, type MerchantOrderRow, type OrderDetailRow } from '@/api/order'

const props = defineProps<{
  order: MerchantOrderRow
  details: OrderDetailRow[]
}>()

/** 头部大字编号：取餐号优先，回落台位号（与看板/列表口径一致） */
const firstNo = computed(() => props.order.queueNo ?? props.order.tableNo ?? '—')

/**
 * 就餐场景文案
 * ⚠️ 2026-09-23 修正：原实现写的是 `orderScene === 2 ? '商城' : '堂食'`，
 * 而本项目 order_scene=2 同时包含「店内自提单」（桌号快照为空）——
 * 结果外带单在详情里被显示成「商城」✗
 * 现按**有无桌号快照**判定（与顾客端 isDineIn、出品看板 headText 同一口径）。
 */
const sceneText = computed(() => {
  const t = props.order.tableName
  return t && String(t).trim() ? '堂食' : '外带自取'
})

/** 单品备注聚合：「布丁奶酒特调 —— 少冰；卡布基诺 —— 热饮」 */
const itemRemarks = computed(() =>
  props.details
    .filter(d => (d.remark ?? '').trim())
    .map(d => `${d.goodsName} —— ${d.remark}`)
    .join('；')
)

function fmtTime(v: string | null): string {
  if (!v) return '—'
  const m = String(v).match(/(\d{4})\/(\d{2})\/(\d{2})\s+(\d{2}):(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}` : String(v)
}

/** specList 是 JSON 串；跳过多选编码键（「加料2」这类后缀键） */
function specText(d: OrderDetailRow): string {
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

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}
</script>
