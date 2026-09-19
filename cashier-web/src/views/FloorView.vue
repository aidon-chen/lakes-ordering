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
        <option>待清台</option>
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
        开台记录
      </button>
    </div>

    <!-- 图例行 2:597（批次 C 三态：空闲 / 占用 / 待清台） -->
    <div class="flex items-center gap-6">
      <span v-for="g in legends" :key="g.label" class="flex items-center gap-2">
        <i class="w-[14px] h-[14px] rounded-[3px] inline-block" :style="{ background: g.bg, border: `1px solid ${g.border}` }" />
        <span class="text-[12px]" style="color: var(--tc-sub)">{{ g.label }} {{ g.count }}</span>
      </span>
      <span class="text-[12px] ml-auto" style="color: var(--tc-gray)">
        共 {{ pos.tables.length }} 个台位 · 当前开台 {{ pos.busyTables.length + pos.uiCounts.toclean }} 桌 · 就餐 {{ pos.diningCount }} 人
      </span>
    </div>

    <!-- 台位网格 2:608：6 列 180×140，间距 16 -->
    <div class="flex flex-wrap gap-4 content-start">
      <TableCard v-for="t in pos.filteredTables" :key="t.id" :table="t" />
    </div>

    <p class="text-[12px] mt-1" style="color: var(--tc-hold)">
      点台位卡开抽屉：查看该台订单 + 只读台位码（生成/作废在管理端 A-09）
    </p>
  </div>

  <!--
    台位抽屉（画板 2:584 底部说明要求「点台位卡开抽屉」）
    ⚠️ 本期实现：台位档案 + 会话摘要 + 状态相关动作
    ⬜ 待补（需后端配合）：
       ① 「该台订单」列表 —— 需按 diningSessionId 查订单的 merchant 接口
       ② 「只读台位码」—— 列表接口未下发 qr_code_url / qr_scene，需给 TableFloorVo 补字段
       两项均记入《逐批次实施与验收计划》§4 的 C 未完成项
  -->
  <Teleport to="body">
    <div
      v-if="pos.drawerTable"
      class="fixed inset-0 z-40 flex justify-end"
      style="background: rgba(23,28,15,.35)"
      @click.self="pos.closeDrawer()"
    >
      <aside class="h-full bg-white shadow-2xl flex flex-col" style="width: min(380px, 92vw)">
        <header class="flex items-center px-5" style="height: 56px; border-bottom: 1px solid var(--tc-line)">
          <span class="text-[18px] font-bold" style="color: var(--tc-title)">{{ pos.drawerTable.tableNo }}</span>
          <span class="ml-3 text-[12px]" style="color: var(--tc-sub)">
            {{ pos.drawerTable.typeName }} · {{ pos.drawerTable.zone }}
          </span>
          <button class="ml-auto text-[18px] leading-none" style="color: var(--tc-sub)" @click="pos.closeDrawer()">✕</button>
        </header>

        <div class="flex-1 overflow-y-auto p-5 flex flex-col gap-3">
          <!-- 状态徽标 -->
          <div class="flex items-center gap-2">
            <span
              class="h-[24px] px-3 rounded-full text-[12px] leading-[24px]"
              :style="drawerStateStyle"
            >{{ drawerStateLabel }}</span>
            <span class="text-[12px]" style="color: var(--tc-sub)">容纳 {{ pos.drawerTable.capacity }} 人</span>
          </div>

          <!-- 会话摘要 -->
          <div v-if="pos.drawerTable.session" class="rounded-lg p-4" style="background: var(--bg)">
            <div class="text-[12px] mb-2" style="color: var(--tc-gray)">当前会话</div>
            <div class="flex justify-between text-[12px] py-1">
              <span style="color: var(--tc-sub)">就餐人数</span>
              <span style="color: var(--txt)">{{ pos.drawerTable.session.memberNum }} 人</span>
            </div>
            <div class="flex justify-between text-[12px] py-1">
              <span style="color: var(--tc-sub)">已开台</span>
              <span style="color: var(--txt)">{{ openDurationText }}</span>
            </div>
            <div class="flex justify-between text-[12px] py-1">
              <span style="color: var(--tc-sub)">低消</span>
              <span style="color: var(--txt)">¥{{ Number(pos.drawerTable.minSpend || 0).toFixed(2) }}</span>
            </div>
          </div>

          <p v-else class="text-[12px]" style="color: var(--tc-sub)">该台位当前无会话</p>

          <!-- 占位说明（不假装有功能） -->
          <div class="rounded border p-3 text-[11px] leading-relaxed" style="border-color: var(--tc-line); color: var(--tc-sub)">
            该台订单列表与只读台位码待后端补接口后接入（台位码的生成/作废在管理端 A-09）。
          </div>
        </div>

        <!-- 状态相关动作 -->
        <footer class="p-4 flex gap-2" style="border-top: 1px solid var(--tc-line)">
          <template v-if="pos.drawerTable.ui === 'toclean'">
            <button
              class="flex-1 h-[36px] rounded text-[13px] text-white"
              style="background: var(--warn)"
              @click="pos.askClear(pos.drawerTable!.id)"
            >清台</button>
          </template>
          <template v-else-if="pos.drawerTable.ui === 'busy'">
            <button
              class="flex-1 h-[36px] rounded text-[13px] text-white"
              style="background: var(--p)"
              @click="pos.resumeOrder(pos.drawerTable!.id)"
            >加酒</button>
            <button
              class="h-[36px] px-4 rounded border text-[13px]"
              style="border-color: var(--tc-line); color: var(--tc-gray)"
              @click="pos.startChange(pos.drawerTable!.id)"
            >转台</button>
          </template>
          <template v-else>
            <button
              class="flex-1 h-[36px] rounded text-[13px] text-white"
              style="background: var(--p)"
              @click="pos.clickTable(pos.drawerTable!)"
            >开台</button>
          </template>
        </footer>
      </aside>
    </div>
  </Teleport>

  <!-- 开台弹窗（空闲共用） -->
  <Teleport to="body">
    <div v-if="openTable" class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(23,28,15,.45)" @click.self="pos.cancelOpen()">
      <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(380px, calc(100vw - 32px))">
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
      <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(420px, calc(100vw - 32px))">
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

/** 三态图例（对齐画板 2:598~2:605；「预定」态本期不做，见批次 C 说明） */
const legends = computed(() => [
  { label: '空闲', count: pos.uiCounts.idle, bg: '#ffffff', border: 'var(--tc-line)' },
  { label: '占用', count: pos.uiCounts.busy, bg: 'var(--pl)', border: 'var(--p)' },
  { label: '待清台', count: pos.uiCounts.toclean, bg: 'var(--bg)', border: 'var(--tc-line)' }
])

/** 抽屉：状态徽标 */
const drawerStateLabel = computed(() => {
  switch (pos.drawerTable?.ui) {
    case 'busy': return '占用'
    case 'toclean': return '待清台'
    default: return '空闲'
  }
})
const drawerStateStyle = computed(() => {
  switch (pos.drawerTable?.ui) {
    case 'busy': return 'background: var(--pl); color: var(--p)'
    case 'toclean': return 'background: #fdeceb; color: var(--warn)'
    default: return 'background: var(--bg); color: var(--tc-sub)'
  }
})

/** 抽屉：已开台时长文本 */
const openDurationText = computed(() => {
  const m = pos.drawerTable?.session?.openMinutes ?? 0
  const h = Math.floor(m / 60)
  return h > 0 ? `${h} 小时 ${m % 60} 分钟` : `${m} 分钟`
})
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
