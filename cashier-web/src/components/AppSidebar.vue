<template>
  <!-- 侧栏 230px（2026-09-22 重排：5 直属功能 + 4 分组；见 config/menu.ts 说明） -->
  <aside class="w-[230px] shrink-0 flex flex-col overflow-y-auto pt-3 pb-4" style="background: var(--dark)">
    <div v-for="entry in MENU" :key="entry.label" class="mb-0.5">
      <!-- 直属功能（一级叶子）：直接可点，一点即达 -->
      <router-link
        v-if="isMenuLeaf(entry)"
        :to="entry.path"
        class="w-full h-11 px-4 flex items-center text-[13px] border-l-2 transition-colors"
        :style="route.path === entry.path
          ? 'color: #fff; background: var(--dark2); border-color: var(--p)'
          : 'color: #aab2a0; border-color: transparent'"
      >{{ entry.label }}</router-link>

      <!-- 分组（一级）：可展开 -->
      <template v-else>
        <button
          class="w-full h-11 px-4 flex items-center gap-1 text-left text-[13px] transition-colors"
          :style="isGroupActive(entry) ? 'background: var(--dark2); color: #fff' : 'color: #aab2a0'"
          @click="toggle(entry.label)"
        >
          <span class="truncate">{{ entry.label }}</span>
          <span class="text-[10px] leading-none ml-0.5" style="opacity: .7">{{ isOpen(entry.label) ? '▾' : '▸' }}</span>

          <!-- 打印管理告警角标位（对位 2:99 侧-打印角标；数据源在批次 G 接入） -->
          <span
            v-if="entry.badged && printAlarmCount > 0"
            class="ml-auto min-w-[18px] h-[18px] px-1 rounded-full text-[10px] leading-[18px] text-center text-white"
            style="background: var(--warn)"
          >{{ printAlarmCount > 99 ? '99+' : printAlarmCount }}</span>
        </button>

        <!-- 组内二级 -->
        <template v-if="isOpen(entry.label)">
          <router-link
            v-for="leaf in entry.children"
            :key="leaf.path"
            :to="leaf.path"
            class="block h-10 pl-8 pr-4 flex items-center text-[13px] border-l-2 transition-colors"
            :style="route.path === leaf.path
              ? 'color: #fff; background: var(--dark2); border-color: var(--p)'
              : 'color: #8f967f; border-color: transparent'"
          >{{ leaf.label }}</router-link>
        </template>
      </template>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MENU, groupLabelOfPath, isMenuGroup, isMenuLeaf, type MenuGroup } from '@/config/menu'

const route = useRoute()

/**
 * 展开的分组（只有「分组」才可展开；直属叶子恒可见）。
 * 默认：只展开当前路由所在分组——5 个高频功能已在最上面常驻，
 * 分组默认收起即可保持侧栏紧凑（避免 13 项同时铺开）。
 */
const open = reactive<Record<string, boolean>>({})
const initGroup = groupLabelOfPath(route.path)
for (const e of MENU) {
  if (isMenuGroup(e)) open[e.label] = e.label === initGroup
}

/** 手风琴：一次只展开一个分组 */
function toggle(label: string) {
  const next = !open[label]
  for (const e of MENU) {
    if (isMenuGroup(e)) open[e.label] = false
  }
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
      for (const e of MENU) {
        if (isMenuGroup(e)) open[e.label] = false
      }
      open[label] = true
    }
  },
  { immediate: true }
)
</script>
