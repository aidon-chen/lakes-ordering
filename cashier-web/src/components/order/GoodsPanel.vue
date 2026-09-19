<template>
  <!-- W-04 商品区（Ardot 2:315）：顶部分类横条 36 + 商品网格
       适配：宽度由写死 566 改为 flex-1——
       1440 下内容区 1170 减去两栏与间距正好 566（与画板一致），更宽时自动多出一列 -->
  <div class="flex-1 min-w-0 flex flex-col">
    <!-- 分类-顶部Tab 2:315/83:67：chip 高 36、圆角 18、「分类名 + 商品数」 -->
    <div class="flex items-center gap-2 overflow-x-auto shrink-0" style="height: 36px">
      <button
        v-for="c in pos.menuChips"
        :key="String(c.id)"
        class="h-9 px-4 rounded-[18px] text-[13px] whitespace-nowrap shrink-0 border transition-colors"
        :style="pos.activeMenuId === c.id
          ? 'background: var(--p); border-color: var(--p); color: #fff'
          : 'background: #fff; border-color: var(--tc-line); color: var(--tc-gray)'"
        @click="pos.selectMenu(c.id)"
      >{{ c.name }} {{ c.count }}</button>
    </div>

    <!-- 商品网格 2:355：gap 10，卡 278×150 -->
    <div class="flex-1 overflow-y-auto mt-[10px]">
      <div v-if="pos.loadingGoods" class="text-[12px] p-4" style="color: var(--tc-gray)">加载中…</div>

      <div v-else-if="pos.goods.length === 0" class="text-[12px] p-4" style="color: var(--tc-gray)">
        该分类暂无在售商品
      </div>

      <!-- 自适应列数（下限 240 = 卡内「图 100 + 间距 12 + 文本 ≥ 96 + 内边距 32」的硬约束）：
           1280 → 2 列(258)；1440 → 2 列(278，与画板一致)；1920 → 3 列；更宽继续增加 -->
      <div v-else class="grid gap-[10px]" style="grid-template-columns: repeat(auto-fill, minmax(240px, 1fr))">
        <button
          v-for="g in pos.goods"
          :key="g.id"
          class="flex items-center gap-3 rounded-lg border bg-white text-left p-4 disabled:cursor-default"
          style="height: 150px; border-color: var(--tc-line)"
          :style="g.status === 4 ? 'border-color: #ececec' : ''"
          :disabled="g.status === 4"
          @click="pos.pickGoods(g)"
        >
          <!-- 商品图：相对路径需拼 OSS 前缀（config/oss.ts），失败退回 ♪ -->
          <div
            class="w-[100px] h-[100px] rounded-md shrink-0 overflow-hidden flex items-center justify-center"
            :style="g.status === 4 ? 'background: #f5f5f5' : 'background: var(--pl)'"
          >
            <img
              v-if="g.mainImage && !broken.has(g.id)"
              :src="ossUrl(g.mainImage)"
              class="w-full h-full object-cover"
              :style="g.status === 4 ? 'filter: grayscale(1); opacity: .55' : ''"
              @error="broken.add(g.id)"
            />
            <span v-else class="text-[26px]" style="color: var(--tc-sub)">♪</span>
          </div>

          <div class="flex-1 min-w-0 h-full flex flex-col">
            <div
              class="text-[13px] font-medium leading-tight line-clamp-2"
              :style="g.status === 4 ? 'color: var(--tc-sub)' : 'color: var(--tc-text, #26301b)'"
            >{{ g.name }}</div>

            <div class="mt-1 text-[11px] truncate" style="color: var(--tc-sub)">{{ subline(g) }}</div>

            <div class="mt-auto flex items-end gap-2">
              <span
                class="text-[16px] font-medium"
                :style="g.status === 4 ? 'color: var(--tc-sub)' : 'color: var(--p)'"
              >¥{{ money(g.salePrice || g.price) }}</span>

              <!-- 加购钮：沽清时置灰 -->
              <span
                v-if="g.status !== 4"
                class="ml-auto w-7 h-7 rounded-full flex items-center justify-center text-[15px] leading-none text-white shrink-0"
                style="background: var(--p)"
              >+</span>
            </div>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { usePosStore } from '@/stores/pos'
import { ossUrl } from '@/config/oss'
import type { GoodsRow } from '@/api/menu'

const pos = usePosStore()
/** 图片加载失败的商品（OSS 缺图时退回 ♪，不显示裂图） */
const broken = ref<Set<number>>(new Set())

/**
 * 卡内次级说明（W-04：如「冰量可选 · 库存 24」「20:36 沽清 · 王莉」）
 * ⚠️ 沽清时间取 tb_goods.update_time（后端改 status 时会刷新），
 *    但库中**没有沽清操作人字段**，故只显示时间，不臆造操作人。
 */
function subline(g: GoodsRow): string {
  if (g.status === 4) {
    const t = g.updateTime ? String(g.updateTime).slice(11, 16) : ''
    return t ? `${t} 沽清` : '已沽清'
  }
  return `库存 ${g.stock ?? 0}`
}

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
