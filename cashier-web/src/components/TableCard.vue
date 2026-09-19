<template>
  <!-- W-07 台位卡：两态（Round 8 回归，删待清台/预定） -->
  <button
    class="relative flex flex-col items-start text-left"
    :class="'w-[180px] h-[140px] rounded-lg'"
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

    <!-- 信息行 -->
    <span class="text-[11px] leading-tight" style="color: var(--tc-sub)">{{ infoLine }}</span>

    <!-- 状态行 -->
    <span class="text-[11px] font-medium leading-tight" :style="{ color: t.fg }">{{ statusLine }}</span>

    <!-- 操作区：占用=加酒/转台/清台；空闲=空 -->
    <div
      v-if="isBusy"
      class="mt-auto w-full flex gap-1.5"
      @click.stop
    >
      <span
        class="flex-1 h-[26px] rounded flex items-center justify-center text-[11px] text-white cursor-pointer"
        style="background: var(--p)"
        @click="pos.resumeOrder(table.id)"
      >加酒</span>
      <span
        class="flex-1 h-[26px] rounded flex items-center justify-center text-[11px] text-white cursor-pointer"
        style="background: var(--tc-gray)"
        @click="pos.startChange(table.id)"
      >转台</span>
      <span
        class="flex-1 h-[26px] rounded flex items-center justify-center text-[11px] cursor-pointer"
        style="background: #fff; border: 1px solid var(--tc-line); color: var(--tc-gray)"
        @click="pos.askClear(table.id)"
      >清台</span>
    </div>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { usePosStore } from '@/stores/pos'
import type { TableInfo, TableUiState } from '@/types'

const pos = usePosStore()
const props = defineProps<{ table: TableInfo }>()

/** 两态色板（Round 8） */
const THEME: Record<TableUiState, { bg: string; border: string; fg: string }> = {
  idle: { bg: '#ffffff', border: 'var(--tc-line)', fg: 'var(--tc-sub)' },
  busy: { bg: 'var(--pl)', border: 'var(--p)', fg: 'var(--p)' }
}

const ui = computed<TableUiState>(() => props.table.ui)
const isBusy = computed(() => ui.value === 'busy')
const t = computed(() => THEME[ui.value])
const cardStyle = computed(() => ({
  background: t.value.bg,
  border: `1px solid ${t.value.border}`,
  padding: '14px',
  gap: ui.value === 'idle' ? '8px' : '6px'
}))

const titleColor = computed(() => (isBusy.value ? 'var(--tc-title)' : 'var(--tc-gray)'))

/** 信息行：占用=「散台 · 4人 · 低消¥300」；空闲=「散台 · 低消 ¥0」 */
const infoLine = computed(() => {
  const { typeName, minSpend, session } = props.table
  if (isBusy.value && session) {
    return `${typeName} · ${session.memberNum}人 · 低消¥${minSpend}`
  }
  return `${typeName} · 低消 ¥${minSpend}`
})

/** 状态行：占用带时长 2h14m；空闲=可开台 */
const statusLine = computed(() => {
  const { session } = props.table
  if (isBusy.value && session) {
    const h = Math.floor(session.openMinutes / 60)
    const m = session.openMinutes % 60
    return `占用 · ${h}h${String(m).padStart(2, '0')}m`
  }
  return '空闲 · 可开台'
})

const addedCount = computed(() => props.table.session?.addedCount ?? 0)
</script>
