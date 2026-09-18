<template>
  <div class="p-7">
    <h1 class="text-lg font-bold" style="color: var(--txt)">点单管理（order）</h1>
    <p class="text-[13px] mt-2" style="color: var(--sub)">
      M0 空壳 · 当前桌台：{{ pos.currentTableId ?? '未选' }} · 会话：{{ pos.currentSessionId ?? '无' }} · 账单 {{ pos.cart.length }} 行 · 新加未下单 {{ pos.addedCount }} 件
    </p>
    <p class="text-xs mt-4" style="color: var(--muted)">
      M2 落地：SessionChips（chips 行）/ CategoryNav + GoodsGrid（分类+商品网格）/ BillPanel（added 金标+步进器）/ BillActions（暂存挂单 · 加菜下单 · 去结账）
    </p>
    <button class="mt-6 px-5 py-3 rounded-xl text-sm border" style="border-color: var(--line); color: var(--sub)" @click="pos.setView('floor')">
      ← 桌台图
    </button>
  </div>
</template>

<script setup lang="ts">
import { usePosStore } from '@/stores/pos'

const pos = usePosStore()
</script>
