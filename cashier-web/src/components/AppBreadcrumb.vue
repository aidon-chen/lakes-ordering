<template>
  <!-- 面包屑 40px（对位 Ardot C-面包屑 2:65；实板样例见 W-04 截图「点单管理 / 代客点单」） -->
  <div class="h-10 shrink-0 flex items-center px-5">
    <span class="text-[15px] font-medium" style="color: var(--tc-title)">{{ leaf?.label ?? '—' }}</span>
    <span class="ml-auto text-[12px]" style="color: var(--tc-sub)">{{ trail }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findMenuByPath } from '@/config/menu'

const route = useRoute()

const found = computed(() => findMenuByPath(route.path))
const leaf = computed(() => found.value?.leaf ?? null)

/**
 * 路径串：分组页为「一级 / 二级」；**直属一级叶子**（台位图/代客点单/…）无上级，只显自身。
 * 画板实板无「首页」前缀。
 */
const trail = computed(() => {
  const f = found.value
  if (!f) return ''
  return f.group ? `${f.group.label} / ${f.leaf.label}` : f.leaf.label
})
</script>
