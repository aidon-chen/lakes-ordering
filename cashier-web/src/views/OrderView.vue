<template>
  <!-- W-04 代客点单（Ardot 2:308）：内容区 1170 = 商品区 566 ｜ 做法与备注 290 ｜ 结算栏 290（gap 12）
       无左栏——台位由 W-07 桌台图点桌带入 -->
  <div class="flex-1 min-h-0 flex flex-col">
    <!-- 点单模式条（2026-09-23 补散客入口）：
         堂食＝必须先在台位图点桌；外带自提＝**免台位**，散客带走场景。
         原来这里在未选台位时只给一句死提示 + 「去台位图」，散客因此无路可走。 -->
    <div
      class="mx-5 mt-4 rounded-lg border px-4 py-3 flex items-center"
      style="border-color: var(--tc-line); background: #fff; gap: 12px"
    >
      <div class="flex rounded overflow-hidden shrink-0" style="border: 1px solid var(--tc-line)">
        <button
          v-for="m in MODES"
          :key="m.v"
          class="h-8 px-3 text-[12px] transition-colors"
          :style="pos.orderMode === m.v
            ? 'background: var(--p); color: #fff'
            : 'background: #fff; color: var(--tc-gray)'"
          @click="pos.setOrderMode(m.v)"
        >{{ m.t }}</button>
      </div>

      <!-- 堂食：显示台位状态（未选台位才给「去台位图」） -->
      <template v-if="!pos.isTakeaway">
        <span
          class="text-[13px] truncate"
          :style="{ color: pos.currentTable ? 'var(--txt)' : 'var(--warn)' }"
        >{{ pos.currentTable
          ? `当前台位：${pos.currentTable.tableNo}${pos.currentTable.tableName ? ' · ' + pos.currentTable.tableName : ''}`
          : '尚未选择台位 —— 请先在「台位图」点桌（占用台进加酒、空闲台先开台）' }}</span>
        <button
          v-if="!pos.currentTable"
          class="ml-auto h-8 px-4 rounded text-[12px] text-white shrink-0"
          style="background: var(--p)"
          @click="goFloor"
        >去台位图</button>
      </template>

      <!-- 外带自提：说明免台位与取餐号口径 -->
      <span v-else class="text-[13px] truncate" style="color: var(--txt)">
        外带自提 · 免台位 —— 下单后计入当日取餐号序列，顾客端显示「取餐码 A-0NN」，凭号取餐
      </span>
    </div>

    <div class="flex-1 min-h-0 flex gap-3 p-5">
      <GoodsPanel />
      <SpecPanel />
      <BillPanel />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePosStore } from '@/stores/pos'
import GoodsPanel from '@/components/order/GoodsPanel.vue'
import SpecPanel from '@/components/order/SpecPanel.vue'
import BillPanel from '@/components/order/BillPanel.vue'

const pos = usePosStore()
const router = useRouter()

/** 点单模式选项（堂食需台位 / 外带自提免台位） */
const MODES = [
  { v: 'dinein' as const, t: '堂食' },
  { v: 'takeaway' as const, t: '外带自提' }
]

onMounted(() => {
  // 深链直达时（未经桌台图点桌）也要能加载商品与分类
  if (!pos.allGoods.length) void pos.loadOrderData()
})

function goFloor() {
  void router.push('/table/floor')
}
</script>
