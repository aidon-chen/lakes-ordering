<template>
  <!-- W-04 代客点单（Ardot 2:308）：内容区 1170 = 商品区 566 ｜ 做法与备注 290 ｜ 结算栏 290（gap 12）
       无左栏——台位由 W-07 桌台图点桌带入 -->
  <div class="flex-1 min-h-0 flex flex-col">
    <!-- 未带台位时的守卫（画板台位由桌台图带入，此处只读提示） -->
    <div
      v-if="!pos.currentTable"
      class="mx-5 mt-4 rounded-lg border px-4 py-3 text-[13px] flex items-center"
      style="border-color: var(--tc-line); background: #fffdf5; color: var(--tc-gray)"
    >
      <span>尚未选择台位 —— 请先在「台位图」点桌（占用台进入加酒、空闲台先开台）</span>
      <button
        class="ml-auto h-8 px-4 rounded text-[12px] text-white"
        style="background: var(--p)"
        @click="goFloor"
      >去台位图</button>
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

onMounted(() => {
  // 深链直达时（未经桌台图点桌）也要能加载商品与分类
  if (!pos.allGoods.length) void pos.loadOrderData()
})

function goFloor() {
  void router.push('/table/floor')
}
</script>
