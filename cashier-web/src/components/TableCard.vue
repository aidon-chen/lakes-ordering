<template>
  <!-- W-07 台位卡（2:608 网格子项）：180x140 · 圆角 8 · padding 14 · 四态配色 -->
  <button
    class="relative flex flex-col items-start text-left"
    :class="[heights, 'rounded-lg']"
    :style="cardStyle"
    @click="pos.clickTable(table)"
  >
    <!-- 占用角标：新加未下单 -->
    <span
      v-if="addedCount"
      class="absolute top-2 right-2 min-w-[18px] h-[18px] px-1 rounded-full text-[10px] leading-[18px] text-center text-white"
      style="background: var(--warn)"
    >{{ addedCount }}</span>

    <!-- 台位号 20px Bold -->
    <span class="text-[20px] font-bold leading-none" :style="{ color: titleColor }">{{ table.tableNo }}</span>

    <!-- 信息行 11~12px -->
    <span class="text-[11px] leading-tight" style="color: var(--tc-sub)">{{ infoLine }}</span>

    <!-- 状态行 11px Medium -->
    <span class="text-[11px] font-medium leading-tight" :style="{ color: statusColor }">{{ statusLine }}</span>

    <!-- 操作钮 26px 高（空闲卡无） -->
    <span
      v-if="actionText"
      class="mt-auto w-full h-[26px] rounded flex items-center justify-center text-[11px] text-white whitespace-nowrap overflow-hidden"
      :style="{ background: statusColor }"
    >{{ actionText }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { usePosStore } from '@/stores/pos'
import type { TableInfo, TableUiState } from '@/types'

const pos = usePosStore()
const props = defineProps<{ table: TableInfo }>()

/** W-07 图例（2:597）四态色板 */
const THEME: Record<TableUiState, { bg: string; border: string; fg: string }> = {
  idle: { bg: '#ffffff', border: 'var(--tc-line)', fg: 'var(--tc-sub)' },
  busy: { bg: 'var(--pl)', border: 'var(--p)', fg: 'var(--p)' },
  reserved: { bg: 'var(--rsv-bg)', border: 'var(--rsv)', fg: 'var(--rsv)' },
  cleaning: { bg: 'var(--clean-bg)', border: 'var(--clean)', fg: 'var(--clean)' }
}

const ui = computed<TableUiState>(() => props.table.ui)
const t = computed(() => THEME[ui.value])
const cardStyle = computed(() => ({
  background: t.value.bg,
  border: `1px solid ${t.value.border}`,
  padding: '14px',
  gap: ui.value === 'idle' ? '8px' : '6px'
}))

const heights = computed(() => (ui.value === 'idle' ? 'w-[180px] h-[140px]' : 'w-[180px] h-[140px]'))

const titleColor = computed(() => (ui.value === 'busy' ? 'var(--tc-title)' : 'var(--tc-gray)'))

/** 信息行：空闲=「散台 · 低消 ¥0」；占用=「散台 · 4人 · 低消¥300」；待清台=「散台 · 已结账 21:02」；预定=「卡座 · 预计 8人」 */
const infoLine = computed(() => {
  const { typeName, minSpend, session, settledAt, reservation } = props.table
  if (ui.value === 'cleaning') return `${typeName} · 已结账 ${settledAt ?? ''}`
  if (ui.value === 'reserved') return `${typeName} · 预计 ${reservation?.expectedGuests ?? '-'}人`
  if (ui.value === 'busy' && session) {
    return `${typeName} · ${session.memberNum}人 · 低消¥${minSpend}`
  }
  return `${typeName} · 低消 ¥${minSpend}`
})

/** 状态行：占用带时长 2h14m */
const statusLine = computed(() => {
  const { session } = props.table
  if (ui.value === 'cleaning') return '待清台'
  if (ui.value === 'reserved') return `预定 · ${props.table.reservation?.arriveTime ?? ''} 到店`
  if (ui.value === 'busy' && session) {
    const h = Math.floor(session.openMinutes / 60)
    const m = session.openMinutes % 60
    return `占用 · ${h}h${String(m).padStart(2, '0')}m`
  }
  return '空闲 · 可开台'
})

const actionText = computed(() => {
  switch (ui.value) {
    case 'busy': return '加酒 / 转台 / 结账'
    case 'reserved': return '改期 / 取消预定'
    case 'cleaning': return '清台'
    default: return ''
  }
})

const addedCount = computed(() => props.table.session?.addedCount ?? 0)
</script>
