<template>
  <!-- 未落地页面的统一占位（批次 A 地基交付物）：随 B~H 批次逐页替换为真实页面 -->
  <div class="flex-1 overflow-auto p-5">
    <div
      class="max-w-[720px] rounded-lg border bg-white p-8"
      style="border-color: var(--tc-line)"
    >
      <div class="flex items-baseline gap-2">
        <span class="text-[16px] font-medium" style="color: var(--tc-title)">{{ leaf?.label ?? '未命名页面' }}</span>
        <span class="text-[12px]" style="color: var(--tc-sub)">{{ leaf?.board }}</span>
      </div>

      <p class="mt-3 text-[13px]" style="color: var(--tc-sub)">
        本页尚未落地，将在 <span style="color: var(--p)">{{ batch }}</span> 批次实现。
      </p>

      <div class="mt-5 pt-4 border-t text-[12px] leading-relaxed" style="border-color: var(--tc-line); color: var(--tc-gray)">
        <div>路径：{{ route.path }}</div>
        <div>所属一级：{{ groupLabel }}</div>
        <div>画板节点：Ardot file/726208852398101 · {{ leaf?.board }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { BATCH_OF_BOARD, findMenuByPath } from '@/config/menu'

const route = useRoute()
const found = computed(() => findMenuByPath(route.path))
const leaf = computed(() => found.value?.leaf ?? null)
const groupLabel = computed(() => found.value?.group?.label ?? '（直属一级功能）')
const batch = computed(() => (leaf.value ? (BATCH_OF_BOARD[leaf.value.board] ?? '待排期') : '待排期'))
</script>
