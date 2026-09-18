<template>
  <div class="p-7">
    <h1 class="text-lg font-bold" style="color: var(--txt)">未结订单（ticket）</h1>
    <p class="text-[13px] mt-2" style="color: var(--sub)">
      M0 空壳 · 进行中 {{ pos.busyTables.length }} 桌（M1 接入 pending-list）
    </p>
    <p class="text-xs mt-4" style="color: var(--muted)">
      M2 落地：PendingOrders 列表（台位/分区/人数/时长/件数/金额/新加数 + 继续点单 / 去结账）—— Odoo TicketScreen 语义
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
