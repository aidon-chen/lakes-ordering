<template>
  <!-- W-04 做法与备注区 290（Ardot 83:29：padding 12、圆角 8、gap 8） -->
  <div
    class="shrink-0 w-[230px] min-[1440px]:w-[290px] 2xl:w-[320px] rounded-lg bg-white flex flex-col overflow-y-auto"
    style="padding: 12px; gap: 8px"
  >
    <!-- 空态：未选商品 -->
    <div v-if="!pos.picked" class="flex-1 flex items-center justify-center">
      <p class="text-[12px] text-center leading-relaxed" style="color: var(--tc-sub)">
        点击左侧商品<br />在此选择做法与配搭
      </p>
    </div>

    <template v-else>
      <!-- 已选品名 83:30 / 价格行 83:31 -->
      <div>
        <div class="text-[14px] font-medium leading-snug" style="color: var(--tc-title)">
          已选：{{ pos.picked.name }}
        </div>
        <div class="mt-1 text-[12px]" style="color: var(--p)">
          ¥{{ money(pos.picked.salePrice || pos.picked.price) }}
          <span v-if="pos.pickedAddon > 0" style="color: var(--tc-sub)">
            （含加价 ¥{{ money(pos.pickedAddon) }}）
          </span>
        </div>
      </div>

      <!-- 规格组：组名即标题；加料/配搭类可多选 -->
      <div v-for="(opts, group) in pos.specGroups" :key="group" class="mt-1">
        <div class="text-[12px] mb-2" style="color: var(--tc-gray)">
          {{ group }}{{ isMultiSpecGroup(group) ? '（可多选）' : '' }}
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="o in opts"
            :key="o.id"
            class="h-[30px] px-3 rounded-[15px] border text-[12px] whitespace-nowrap"
            :class="o.stock === 2 ? 'opacity-40' : ''"
            :style="isSelected(group, o.name)
              ? 'background: var(--pl); border-color: var(--p); color: var(--p)'
              : 'background: #fff; border-color: var(--tc-line); color: var(--tc-gray)'"
            :disabled="o.stock === 2"
            @click="pos.toggleSpec(group, o.name)"
          >{{ o.name }}{{ Number(o.price) > 0 ? ` +¥${Number(o.price).toFixed(0)}` : '' }}</button>
        </div>
      </div>

      <!-- 数量 83:56（步进器 110×32 圆角 4） -->
      <div class="mt-1 flex items-center">
        <span class="text-[12px]" style="color: var(--tc-gray)">数量</span>
        <div class="ml-auto flex items-center rounded border" style="border-color: var(--tc-line); height: 32px">
          <button class="w-[34px] h-full text-[15px]" style="color: var(--tc-gray)" @click="pos.bumpQty(-1)">−</button>
          <span class="w-[42px] text-center text-[14px]" style="color: var(--tc-title)">{{ pos.qty }}</span>
          <button class="w-[34px] h-full text-[15px]" style="color: var(--tc-gray)" @click="pos.bumpQty(1)">+</button>
        </div>
      </div>

      <!-- 单品备注 83:80~83:90（**整单备注已移至结算栏**，2026-09-23：
           做法区只留单品级备注，订单级的整单备注与结算同层，避免两处填错） -->
      <div class="mt-1">
        <div class="text-[12px] mb-2" style="color: var(--tc-gray)">单品备注</div>
        <input
          v-model="pos.itemRemark"
          class="w-full h-7 px-2 rounded border text-[12px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="该菜品的备注，如：少冰"
        />
      </div>

      <!-- 加入购物车 83:65（252×36 圆角 4） -->
      <button
        class="mt-auto h-9 rounded text-[13px] font-medium text-white"
        style="background: var(--p)"
        @click="pos.addToCart()"
      >加入购物车 ¥{{ money(pos.pickedPrice * pos.qty) }}</button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { usePosStore } from '@/stores/pos'

const pos = usePosStore()

/** 多选规格组（加料/配搭/配料/小料），与 store 的 toSpecListJson 编码保持一致 */
const MULTI_SPEC_KEYWORDS = ['加料', '配搭', '配料', '小料']
function isMultiSpecGroup(name: string): boolean {
  return MULTI_SPEC_KEYWORDS.some(k => name.includes(k))
}

function isSelected(group: string, option: string): boolean {
  return (pos.specSelected[group] ?? []).includes(option)
}

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
