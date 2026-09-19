<template>
  <!--
    W-07 台位卡（Ardot 2:609~2:661）：180×140 圆角 8 内边距 14
    三态（批次 C 2026-09-19）：
      空闲   —— 白底，无操作钮
      占用   —— 浅底绿，状态字主色，「加酒」主色钮（+ 转台次钮）
      待清台 —— 灰底，状态字危险色，「清台」危险色钮
    ⚠️ 画板另外画了「预定」态（K04），但需求书 0 次提及、工程零表零接口
       → 本期不做（依据见《逐批次实施与验收计划》§4）
    ⚠️ 画板把占用卡操作画成一个「加酒 / 转台」合并钮；一个钮承载不了两个动作，
       故拆为「加酒」主钮 + 「转台」次钮，视觉主次与画板一致。
  -->
  <div
    class="relative flex flex-col items-start text-left cursor-pointer w-[180px] h-[140px] rounded-lg"
    :style="cardStyle"
    @click="pos.clickTable(table)"
  >
    <!-- 新加未下单角标 -->
    <span
      v-if="addedCount"
      class="absolute top-2 right-2 min-w-[18px] h-[18px] px-1 rounded-full text-[10px] leading-[18px] text-center text-white"
      style="background: var(--warn)"
    >{{ addedCount }}</span>

    <!-- 台位号 -->
    <span class="text-[20px] font-bold leading-none" :style="{ color: titleColor }">{{ table.tableNo }}</span>

    <!-- 信息行 -->
    <span class="text-[11px] leading-tight truncate w-full" style="color: var(--tc-sub)">{{ infoLine }}</span>

    <!-- 状态行 -->
    <span class="text-[11px] font-medium leading-tight" :style="{ color: t.fg }">{{ statusLine }}</span>

    <!-- 操作钮 152×26 圆角 4 -->
    <div v-if="ui !== 'idle'" class="mt-auto w-full flex gap-1.5" @click.stop>
      <!-- 待清台：只有「清台」 -->
      <span
        v-if="ui === 'toclean'"
        class="w-full h-[26px] rounded flex items-center justify-center text-[11px] text-white cursor-pointer"
        style="background: var(--warn)"
        @click="pos.askClear(table.id)"
      >清台</span>

      <!-- 占用：主钮「加酒」＋次钮「转台」 -->
      <template v-else>
        <span
          class="flex-1 h-[26px] rounded flex items-center justify-center text-[11px] text-white cursor-pointer"
          style="background: var(--p)"
          @click="pos.resumeOrder(table.id)"
        >加酒</span>
        <span
          class="w-[52px] h-[26px] rounded flex items-center justify-center text-[11px] cursor-pointer shrink-0"
          style="background: #fff; border: 1px solid var(--tc-line); color: var(--tc-gray)"
          @click="pos.startChange(table.id)"
        >转台</span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { usePosStore } from '@/stores/pos'
import type { TableInfo, TableUiState } from '@/types'

const pos = usePosStore()
const props = defineProps<{ table: TableInfo }>()

/** 三态色板（批次 C）：待清台用灰底 + 危险色字 */
const THEME: Record<TableUiState, { bg: string; border: string; fg: string }> = {
  idle: { bg: '#ffffff', border: 'var(--tc-line)', fg: 'var(--tc-sub)' },
  busy: { bg: 'var(--pl)', border: 'var(--p)', fg: 'var(--p)' },
  toclean: { bg: 'var(--bg)', border: 'var(--tc-line)', fg: 'var(--warn)' }
}

const ui = computed<TableUiState>(() => props.table.ui)
const t = computed(() => THEME[ui.value])
const cardStyle = computed(() => ({
  background: t.value.bg,
  border: `1px solid ${t.value.border}`,
  padding: '14px',
  gap: ui.value === 'idle' ? '8px' : '6px'
}))

const titleColor = computed(() => (ui.value === 'idle' ? 'var(--tc-gray)' : 'var(--tc-title)'))

/** 信息行 */
const infoLine = computed(() => {
  const { typeName, minSpend, session } = props.table
  if (ui.value === 'toclean') {
    return closeHHmm.value ? `${typeName} · 已结账 ${closeHHmm.value}` : `${typeName} · 已结账`
  }
  if (ui.value === 'busy' && session) {
    return `${typeName} · ${session.memberNum}人 · 低消¥${money(minSpend)}`
  }
  return `${typeName} · 低消 ¥${money(minSpend)}`
})

/** 状态行：占用带时长；待清台固定文案；空闲=可开台 */
const statusLine = computed(() => {
  const { session } = props.table
  if (ui.value === 'toclean') return '待清台'
  if (ui.value === 'busy' && session) {
    const h = Math.floor(session.openMinutes / 60)
    const m = session.openMinutes % 60
    return `占用 · ${h}h${String(m).padStart(2, '0')}m`
  }
  return '空闲 · 可开台'
})

const addedCount = computed(() => props.table.session?.addedCount ?? 0)

/** 结台时间 HH:mm（兼容 "2026/09/19 21:02:00" 与 ISO） */
const closeHHmm = computed(() => {
  const v = props.table.session?.closeTime
  if (!v) return ''
  const m = String(v).match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : ''
})

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
