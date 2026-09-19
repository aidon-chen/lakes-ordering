<template>
  <!-- W-03 点单管理（2:307）：三栏 = 台位/会话条 + 分类导航 + 商品网格；右侧账单栏 -->
  <div class="flex-1 flex min-w-0 p-4 gap-4">
    <!-- 左：台位 + 分类 -->
    <div class="w-[200px] shrink-0 flex flex-col gap-3">
      <!-- 当前台位 -->
      <div class="rounded-lg border p-3" style="border-color: var(--tc-line)">
        <div class="text-[14px] font-bold" style="color: var(--tc-title">{{ currentTable?.tableNo ?? '未选台位' }}</div>
        <div class="text-[11px] mt-0.5" style="color: var(--tc-sub)">
          {{ currentTable ? `${currentTable.typeName} · ${currentTable.zone} · ${currentTable.session?.memberNum ?? '-'}人` : '请先在桌台图开台' }}
        </div>
        <div class="text-[11px] mt-1" style="color: var(--p)">
          {{ currentTable?.session ? `会话 #${currentTable.session.sessionId} · 已点 ¥${currentTable.session.goodsTotalPrice ?? 0}` : '' }}
        </div>
      </div>

      <!-- 分类导航 -->
      <div class="flex-1 overflow-auto rounded-lg border" style="border-color: var(--tc-line)">
        <button
          v-for="m in pos.menus"
          :key="m.id"
          class="w-full h-11 px-4 text-left text-[13px] border-b last:border-b-0"
          :class="m.id === pos.currentMenuId ? 'font-medium text-white' : ''"
          :style="m.id === pos.currentMenuId
            ? 'background: var(--p)'
            : 'background: #fff; color: var(--tc-gray)'"
          @click="pos.loadGoods(m.id)"
        >{{ m.name }}</button>
      </div>
    </div>

    <!-- 中：商品网格 -->
    <div class="flex-1 overflow-auto">
      <div v-if="pos.loadingGoods" class="text-[12px] p-4" style="color: var(--tc-gray)">加载中…</div>
      <div v-else class="grid gap-3" style="grid-template-columns: repeat(auto-fill, minmax(140px, 1fr))">
        <button
          v-for="g in pos.goods"
          :key="g.id"
          class="rounded-lg border p-3 text-left flex flex-col gap-1.5"
          :class="g.status === 4 ? 'opacity-50' : ''"
          style="border-color: var(--tc-line); background: #fff"
          :disabled="g.status === 4"
          @click="pos.pickGoods(g.id, g.name, Number(g.salePrice || g.price || 0))"
        >
          <div class="w-full h-[72px] rounded flex items-center justify-center overflow-hidden" style="background: var(--pl)">
            <img v-if="g.mainImage" :src="g.mainImage" class="w-full h-full object-cover" />
            <span v-else class="text-[24px]" style="color: var(--tc-sub)">♪</span>
          </div>
          <div class="text-[12px] font-medium leading-tight" style="color: var(--tc-title)">{{ g.name }}</div>
          <div class="text-[12px] font-bold" style="color: var(--p)">¥{{ g.salePrice || g.price }}</div>
          <div v-if="g.status === 4" class="text-[10px]" style="color: var(--danger, #f56c6c)">已售罄</div>
        </button>
      </div>
    </div>

    <!-- 右：账单栏 -->
    <div class="w-[280px] shrink-0 flex flex-col rounded-lg border" style="border-color: var(--tc-line)">
      <div class="px-3 py-2.5 border-b text-[13px] font-medium" style="color: var(--tc-title); border-color: var(--tc-line)">
        当前账单（{{ pos.cart.length }} 项）
      </div>

      <div class="flex-1 overflow-auto px-3 py-2">
        <div v-if="!pos.cart.length" class="text-[12px] text-center py-6" style="color: var(--tc-gray)">
          点击左侧商品加购
        </div>
        <div v-for="(l, i) in pos.cart" :key="i" class="flex items-center gap-2 py-1.5 border-b last:border-b-0" style="border-color: #f2f3f5">
          <div class="flex-1 min-w-0">
            <div class="text-[12px] truncate" style="color: var(--tc-title)">{{ l.goodsName }}</div>
            <div class="text-[10px]" style="color: var(--tc-sub)">{{ l.specText || '默认' }} · ¥{{ l.price }}</div>
          </div>
          <div class="flex items-center gap-1.5">
            <button class="w-6 h-6 rounded border text-[13px]" style="border-color: var(--tc-line); color: var(--tc-gray)" @click="pos.changeQty(i, -1)">−</button>
            <span class="w-6 text-center text-[13px] font-medium">{{ l.qty }}</span>
            <button class="w-6 h-6 rounded border text-[13px]" style="border-color: var(--tc-line); color: var(--tc-gray)" @click="pos.changeQty(i, 1)">+</button>
          </div>
          <div class="w-14 text-right text-[12px] font-medium" style="color: var(--tc-title)">¥{{ (l.price * l.qty).toFixed(2) }}</div>
        </div>
      </div>

      <div class="px-3 py-2.5 border-t" style="border-color: var(--tc-line)">
        <div class="flex justify-between text-[13px] mb-2">
          <span style="color: var(--tc-sub)">合计</span>
          <span class="font-bold text-[16px]" style="color: var(--p)">¥{{ pos.cartTotal.toFixed(2) }}</span>
        </div>
        <div class="flex gap-2">
          <button class="flex-1 h-9 rounded text-[12px] border" style="border-color: var(--tc-line); color: var(--tc-gray)" @click="pos.holdOrder()">
            挂单
          </button>
          <button
            class="flex-1 h-9 rounded text-[12px] font-medium text-white disabled:opacity-50"
            style="background: var(--p)"
            :disabled="!pos.cart.length || submitting"
            @click="submit"
          >{{ submitting ? '提交中…' : '下单送厨' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { usePosStore } from '@/stores/pos'

const pos = usePosStore()
const submitting = ref(false)

const currentTable = computed(() => pos.currentTable)

async function submit() {
  submitting.value = true
  try {
    const ok = await pos.sendItems()
    if (ok) {
      // 下单成功：刷新桌台（会话金额/added 变化）并留在点单页
      await pos.loadTables()
    }
  } finally {
    submitting.value = false
  }
}
</script>
