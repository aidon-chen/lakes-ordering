<template>
  <!-- W-07 台位图与开台（2:581 内容区）：p-5 · 纵向 gap 12 -->
  <div class="flex-1 overflow-auto p-5 flex flex-col gap-3 min-w-0">
    <!-- 工具条 2:586 -->
    <div class="flex items-center gap-3">
      <select v-model="pos.zone" class="toolbar-select w-[140px]">
        <option>全部区域</option>
        <option v-for="z in pos.zones" :key="z.id" :value="z.name">{{ z.name }}</option>
      </select>
      <select v-model="pos.statusFilter" class="toolbar-select w-[140px]">
        <option>全部状态</option>
        <option>空闲</option>
        <option>占用</option>
      </select>
      <input
        v-model="pos.keyword"
        class="toolbar-select w-[200px]"
        style="color: var(--tc-gray)"
        placeholder="搜索台位号"
      />
      <button class="h-[36px] px-5 rounded text-[13px] font-medium text-white" style="background: var(--p)" @click="pos.keyword = pos.keyword">
        查询
      </button>
      <button
        class="ml-auto h-[32px] px-3 rounded border bg-white text-[12px]"
        style="border-color: var(--tc-line); color: var(--tc-gray)"
      >
        扫码开台记录
      </button>
    </div>

    <!-- 图例行 2:597（Round 8 两态） -->
    <div class="flex items-center gap-6">
      <span v-for="g in legends" :key="g.label" class="flex items-center gap-2">
        <i class="w-[14px] h-[14px] rounded-[3px] inline-block" :style="{ background: g.bg, border: `1px solid ${g.border}` }" />
        <span class="text-[12px]" style="color: var(--tc-sub)">{{ g.label }} {{ g.count }}</span>
      </span>
      <span class="text-[12px]" style="color: var(--tc-gray)">
        共 {{ pos.tables.length }} 个台位 · 当前开台 {{ pos.busyTables.length }} 桌 · 就餐 {{ pos.diningCount }} 人
      </span>
    </div>

    <!-- 台位网格 2:608：wrap gap 16 -->
    <div class="flex flex-wrap gap-4 content-start">
      <TableCard v-for="t in pos.filteredTables" :key="t.id" :table="t" />
    </div>

    <p class="text-[12px] mt-1" style="color: var(--tc-hold)">
      点台位卡进单/开台；占用卡可「加酒」或「转台」
    </p>
  </div>

  <!-- 开台弹窗（空闲共用） -->
  <Teleport to="body">
    <div v-if="openTable" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="pos.cancelOpen()">
      <div class="w-[380px] rounded-xl bg-white p-6 shadow-xl">
        <div class="flex items-baseline justify-between">
          <span class="text-[20px] font-bold" style="color: var(--tc-title)">{{ openTable.tableNo }}</span>
          <span class="text-[12px]" style="color: var(--tc-sub)">{{ openTable.typeName }} · {{ openTable.zone }} · 低消 ¥{{ openTable.minSpend }}</span>
        </div>
        <p class="mt-1 text-[12px]" style="color: var(--tc-sub)">
          空闲 · 确认后进入点单
        </p>

        <div class="mt-5">
          <div class="text-[13px] font-medium mb-2">就餐人数</div>
          <div class="flex items-center gap-3">
            <button class="stepper-btn" @click="pos.guestPick = Math.max(1, pos.guestPick - 1)">−</button>
            <span class="w-12 text-center text-[18px] font-bold">{{ pos.guestPick }}</span>
            <button class="stepper-btn" @click="pos.guestPick = Math.min(openTable.capacity, pos.guestPick + 1)">+</button>
            <span class="text-[12px]" style="color: var(--tc-sub)">容量 {{ openTable.capacity }} 人</span>
          </div>
        </div>

        <div class="mt-6 flex gap-3 justify-end">
          <button class="h-[36px] px-4 rounded border text-[13px]" style="border-color: var(--tc-line); color: var(--tc-gray)" @click="pos.cancelOpen()">
            取消
          </button>
          <button class="h-[36px] px-5 rounded text-[13px] font-medium text-white" style="background: var(--p)" @click="pos.confirmOpen()">
            确认开台
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 转台弹窗（Round 5 拍板：弹窗选空闲目标台） -->
  <Teleport to="body">
    <div v-if="changeTable" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="pos.cancelChange()">
      <div class="w-[420px] rounded-xl bg-white p-6 shadow-xl">
        <div class="flex items-baseline justify-between">
          <span class="text-[20px] font-bold" style="color: var(--tc-title)">转台 · {{ changeTable.tableNo }}</span>
          <span class="text-[12px]" style="color: var(--tc-sub)">{{ changeTable.typeName }} · {{ changeTable.zone }}</span>
        </div>
        <p class="mt-1 text-[12px]" style="color: var(--tc-sub)">选择目标空闲台位（就餐与会话将迁移）</p>

        <div class="mt-4 max-h-[320px] overflow-auto flex flex-wrap gap-2">
          <button
            v-for="t in pos.changeTargets"
            :key="t.id"
            class="h-[44px] px-4 rounded border text-[13px]"
            style="border-color: var(--tc-line); color: var(--tc-gray)"
            @click="pos.confirmChange(t.id)"
          >{{ t.tableNo }} · {{ t.zone }}</button>
          <span v-if="!pos.changeTargets.length" class="text-[12px]" style="color: var(--tc-gray)">暂无空闲台位</span>
        </div>

        <div class="mt-6 flex gap-3 justify-end">
          <button class="h-[36px] px-4 rounded border text-[13px]" style="border-color: var(--tc-line); color: var(--tc-gray)" @click="pos.cancelChange()">
            取消
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { usePosStore } from '@/stores/pos'
import TableCard from '@/components/TableCard.vue'
import type { TableInfo } from '@/types'

const pos = usePosStore()
onMounted(() => pos.loadTables())

const openTable = computed<TableInfo | null>(() =>
  pos.tables.find(t => t.id === pos.openForTableId) ?? null
)

/** 转台弹窗当前源台 */
const changeTable = computed<TableInfo | null>(() =>
  pos.tables.find(t => t.id === pos.changeForTableId) ?? null
)

const legends = computed(() => [
  { label: '空闲', count: pos.uiCounts.idle, bg: '#ffffff', border: 'var(--tc-line)' },
  { label: '占用', count: pos.uiCounts.busy, bg: 'var(--pl)', border: 'var(--p)' }
])
</script>

<style scoped>
.toolbar-select {
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--tc-line);
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: var(--tc-gray);
  outline: none;
}
.toolbar-select:focus {
  border-color: var(--p);
}
.stepper-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--tc-line);
  border-radius: 4px;
  font-size: 16px;
  color: var(--tc-gray);
}
.stepper-btn:hover {
  border-color: var(--p);
  color: var(--p);
}
</style>
