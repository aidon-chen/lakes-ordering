<template>
  <!-- 侧栏 230px（对位 Ardot C-侧栏 2:84：7 一级 / 19 二级） -->
  <aside class="w-[230px] shrink-0 flex flex-col overflow-y-auto pt-3 pb-4" style="background: var(--dark)">
    <div v-for="group in MENU" :key="group.label" class="mb-0.5">
      <!-- 一级 -->
      <button
        class="w-full h-11 px-4 flex items-center gap-1 text-left text-[13px] transition-colors"
        :style="isGroupActive(group) ? 'background: var(--dark2); color: #fff' : 'color: #aab2a0'"
        @click="toggle(group.label)"
      >
        <span class="truncate">{{ group.label }}</span>
        <span class="text-[10px] leading-none ml-0.5" style="opacity: .7">{{ isOpen(group.label) ? '▾' : '▸' }}</span>

        <!-- 打印管理告警角标位（对位 2:99 侧-打印角标；数据源在批次 G 接入） -->
        <span
          v-if="group.badged && printAlarmCount > 0"
          class="ml-auto min-w-[18px] h-[18px] px-1 rounded-full text-[10px] leading-[18px] text-center text-white"
          style="background: var(--warn)"
        >{{ printAlarmCount > 99 ? '99+' : printAlarmCount }}</span>
      </button>

      <!-- 二级 -->
      <template v-if="isOpen(group.label)">
        <router-link
          v-for="leaf in group.children"
          :key="leaf.path"
          :to="leaf.path"
          class="block h-10 pl-8 pr-4 flex items-center text-[13px] border-l-2 transition-colors"
          :style="route.path === leaf.path
            ? 'color: #fff; background: var(--dark2); border-color: var(--p)'
            : 'color: #8f967f; border-color: transparent'"
        >{{ leaf.label }}</router-link>
      </template>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MENU, groupLabelOfPath, type MenuGroup } from '@/config/menu'

const route = useRoute()

/** 展开的一级导航（默认展开当前路由所属一级，对齐画板「台位管理默认展开」） */
const open = reactive<Record<string, boolean>>({})
const initGroup = groupLabelOfPath(route.path)
for (const g of MENU) open[g.label] = g.label === (initGroup ?? '台位管理')

/** 手风琴：一次只展开一个一级，避免 19 项同时铺开时侧栏过长 */
function toggle(label: string) {
  const next = !open[label]
  for (const g of MENU) open[g.label] = false
  open[label] = next
}

function isOpen(label: string) {
  return open[label] === true
}

/** 一级是否处于「含当前路由」态 */
function isGroupActive(group: MenuGroup) {
  return group.children.some(l => l.path === route.path)
}

/** 打印队列告警数：批次 G（W-25）接入后端后填充，未接入时角标不显示 */
const printAlarmCount = 0

// 路由变化时保证所属一级展开（含直接输入 URL 进入的深链）
watch(
  () => route.path,
  path => {
    const label = groupLabelOfPath(path)
    if (label && !open[label]) {
      for (const g of MENU) open[g.label] = false
      open[label] = true
    }
  },
  { immediate: true }
)
</script>
