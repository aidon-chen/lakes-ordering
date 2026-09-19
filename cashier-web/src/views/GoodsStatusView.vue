<template>
  <!-- W-26 点单品沽清与上下架（Ardot `2:504`；内容区 `2:507`） -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 说明条 2:509（1170×40 圆角 4，浅底） -->
    <div
      class="shrink-0 flex items-center rounded"
      style="height: 40px; padding-left: 12px; background: var(--pl)"
    >
      <span class="text-[12px]" style="color: var(--tc-title)">
        沽清只停售、不改商品资料：开关关闭后客户端立即不可下单；商品资料与价格仍在管理端 A-05 维护
      </span>
    </div>

    <!-- 工具条 2:511（40 高，间距 12） -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="keyword"
        class="rounded border text-[12px] outline-none"
        style="width: 240px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="搜索品名 / 商品编号"
      />
      <select
        v-model="menuId"
        class="rounded border text-[12px] outline-none"
        style="width: 140px; height: 32px; padding: 0 10px; border-color: var(--tc-line); color: var(--tc-gray)"
      >
        <option :value="null">全部分类</option>
        <option v-for="m in menus" :key="m.id" :value="m.id">{{ m.name }}</option>
      </select>

      <!-- 切换组 2:516（两段各 100×32） -->
      <div class="flex rounded border overflow-hidden" style="border-color: var(--tc-line)">
        <button
          class="text-[12px]"
          style="width: 100px; height: 30px"
          :style="!onlySoldOut ? 'background: var(--p); color: #fff' : 'background: #fff; color: var(--tc-gray)'"
          @click="onlySoldOut = false"
        >全部({{ goods.length }})</button>
        <button
          class="text-[12px]"
          style="width: 100px; height: 30px"
          :style="onlySoldOut ? 'background: var(--p); color: #fff' : 'background: #fff; color: var(--tc-gray)'"
          @click="onlySoldOut = true"
        >仅看已沽清({{ soldOutCount }})</button>
      </div>

      <div class="flex-1" />

      <!-- 批量操作 2:522 / 2:524 -->
      <button
        class="rounded border text-[12px] disabled:opacity-50"
        style="width: 110px; height: 32px; border-color: var(--tc-line); color: var(--tc-gray)"
        :disabled="bulkRunning || saleableCount === 0"
        @click="bulk(4)"
      >批量沽清</button>
      <button
        class="rounded border text-[12px] disabled:opacity-50"
        style="width: 110px; height: 32px; border-color: var(--tc-line); color: var(--tc-gray)"
        :disabled="bulkRunning || soldOutCount === 0"
        @click="bulk(2)"
      >批量恢复</button>
    </div>

    <!-- 卡片区 2:526：3 列 × 370×150（列距 30 / 行距 12） -->
    <div class="flex-1 min-h-0 overflow-y-auto">
      <div v-if="loading" class="text-[12px] py-4" style="color: var(--tc-sub)">加载中…</div>

      <div v-else-if="!visible.length" class="text-[12px] py-4" style="color: var(--tc-sub)">
        没有符合条件的商品
      </div>

      <div
        v-else
        class="grid"
        style="grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 12px 30px"
      >
        <div
          v-for="g in visible"
          :key="g.id"
          class="rounded-lg flex items-center"
          style="height: 150px; padding-left: 14px"
          :style="g.status === 4 ? 'background: var(--bg)' : 'background: #fff'"
        >
          <!-- 商品图 72×72（2:530 等） -->
          <div
            class="w-[72px] h-[72px] rounded-md shrink-0 overflow-hidden flex items-center justify-center"
            style="background: var(--pl)"
          >
            <img
              v-if="g.mainImage && !broken.has(g.id)"
              :src="ossUrl(g.mainImage)"
              class="w-full h-full object-cover"
              :style="g.status === 4 ? 'filter: grayscale(1); opacity: .55' : ''"
              @error="broken.add(g.id)"
            />
            <span v-else class="text-[22px]" style="color: var(--tc-sub)">♪</span>
          </div>

          <!-- 信息列 216×64（gap 6） -->
          <div class="flex-1 min-w-0 ml-3 flex flex-col" style="gap: 6px">
            <div class="text-[13px] leading-tight truncate" style="color: var(--txt)">{{ g.name }}</div>
            <div class="text-[11px] truncate" style="color: var(--tc-sub)">
              {{ g.menuName || '未分类' }} · ¥{{ money(g.salePrice || g.price) }}
            </div>
            <div class="text-[11px] truncate" :style="g.status === 4 ? 'color: var(--warn)' : 'color: var(--tc-sub)'">
              {{ changeText(g) }}
            </div>
          </div>

          <!-- 开关 44×24 圆角 12（开=主色 / 关=灰） -->
          <button
            class="shrink-0 ml-3 mr-5 rounded-full flex items-center disabled:opacity-60"
            style="width: 44px; height: 24px; padding: 2px"
            :style="g.status === 4 ? 'background: var(--tc-hold)' : 'background: var(--p)'"
            :disabled="savingId === g.id"
            :title="g.status === 4 ? '点击恢复出餐' : '点击沽清'"
            @click="toggle(g)"
          >
            <span
              class="rounded-full bg-white"
              style="width: 20px; height: 20px; transition: margin-left .15s"
              :style="g.status === 4 ? 'margin-left: 0' : 'margin-left: 20px'"
            />
          </button>
        </div>
      </div>

      <!-- 加载更多（板为「上拉加载更多」，桌面端改为按钮，文案对齐） -->
      <div v-if="visible.length < filtered.length" class="mt-4 flex justify-center">
        <button
          class="rounded border text-[12px]"
          style="height: 32px; padding: 0 24px; border-color: var(--tc-line); color: var(--p)"
          @click="shown += PAGE_SIZE"
        >加载更多</button>
      </div>
    </div>

    <!-- 更多提示 2:572 -->
    <div class="shrink-0 text-[11px]" style="color: var(--tc-sub)">{{ moreHint }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { fetchGoods, fetchMenus, updateGoodsStatus, updateGoodsStatusBatch, type GoodsRow, type MenuRow } from '@/api/menu'
import { ossUrl } from '@/config/oss'

const PAGE_SIZE = 12

const menus = ref<MenuRow[]>([])
const goods = ref<GoodsRow[]>([])
const loading = ref(true)
const broken = ref<Set<number>>(new Set())
const savingId = ref<number | null>(null)
const bulkRunning = ref(false)

const keyword = ref('')
const menuId = ref<number | null>(null)
const onlySoldOut = ref(false)
const shown = ref(PAGE_SIZE)

const soldOutCount = computed(() => goods.value.filter(g => g.status === 4).length)
const saleableCount = computed(() => goods.value.filter(g => g.status === 2).length)

/** 筛选：分类 → 关键字（品名 / 商品编号）→ 仅看沽清 */
const filtered = computed(() => {
  const kw = keyword.value.toLowerCase()
  return goods.value.filter(g => {
    if (menuId.value != null && g.menuId !== menuId.value) return false
    if (onlySoldOut.value && g.status !== 4) return false
    if (kw && !(g.name.toLowerCase().includes(kw) || String(g.id).includes(kw))) return false
    return true
  })
})

const visible = computed(() => filtered.value.slice(0, shown.value))

/** 顶部筛选变化时重置分页，避免「加载更多」后换分类出现空白 */
watch([keyword, menuId, onlySoldOut], () => {
  shown.value = PAGE_SIZE
})

const moreHint = computed(() => {
  const total = filtered.value.length
  const rest = Math.max(0, total - visible.value.length)
  const restSoldOut = filtered.value.slice(visible.value.length).filter(g => g.status === 4).length
  const tail = rest === 0
    ? ''
    : restSoldOut > 0
      ? `（其余 ${rest} 件中 ${restSoldOut} 件已沽清）`
      : `（其余 ${rest} 件均在售）`
  return `已显示 ${visible.value.length} / ${total} 件 · 上拉加载更多${tail}`
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [menuPage, goodsPage] = await Promise.all([fetchMenus(), fetchGoods()])
    menus.value = menuPage?.records ?? []
    goods.value = goodsPage?.records ?? []
  } catch (e) {
    console.error('[goods-status] 加载失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 变更行文案
 * ⚠️ 两处**不臆造**：
 *   ① 画板样例含操作人（「已沽清 · 王莉 20:36 标记」），但 `tb_goods` **无操作人字段** → 只显示时间
 *   ② 同理无法断言「恢复在售」这个具体动作（`update_time` 对任何编辑都会刷新，不区分动作）
 *      → 只陈述「最近变更 <时间>」，不写动作名
 */
function changeText(g: GoodsRow): string {
  const t = hhmm(g.updateTime)
  if (g.status === 4) return t ? `已沽清 · 最近变更 ${t}` : '已沽清'
  return t ? `在售 · 最近变更 ${t}` : '在售'
}

/** 兼容 "2026/09/19 14:54:13" 与 ISO 两种时间格式，取 HH:mm */
function hhmm(v?: string): string {
  if (!v) return ''
  const m = String(v).match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : ''
}

/** 单件切换：成功改本地态并刷新变更时间（与后端 SET status, update_time 一致） */
async function toggle(g: GoodsRow) {
  const next: 2 | 4 = g.status === 4 ? 2 : 4
  savingId.value = g.id
  try {
    await updateGoodsStatus(g.id, next)
    g.status = next
    g.updateTime = nowText()
  } catch (e) {
    alert((e as Error).message || '操作失败')
  } finally {
    savingId.value = null
  }
}

/** 批量：作用于「当前筛选结果」（画板未画多选，按筛选集语义实现），执行前确认 */
async function bulk(status: 2 | 4) {
  const targets = filtered.value.filter(g => (status === 4 ? g.status === 2 : g.status === 4))
  if (!targets.length) return
  const label = status === 4 ? '沽清' : '恢复出餐'
  const scope = describeScope()
  if (!window.confirm(`将${scope}的 ${targets.length} 件商品${label}？`)) return

  bulkRunning.value = true
  try {
    const { ok, failed } = await updateGoodsStatusBatch(targets.map(g => g.id), status)
    await load()
    alert(failed.length ? `成功 ${ok} 件，失败 ${failed.length} 件（见控制台）` : `已${label} ${ok} 件`)
  } finally {
    bulkRunning.value = false
  }
}

function describeScope(): string {
  const parts: string[] = []
  if (menuId.value != null) parts.push(menus.value.find(m => m.id === menuId.value)?.name ?? '当前分类')
  if (keyword.value) parts.push(`关键字「${keyword.value}」`)
  if (onlySoldOut.value) parts.push('仅看已沽清')
  return parts.length ? parts.join(' + ') : '全部'
}

function nowText(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${p(d.getMonth() + 1)}/${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function money(v: number | string): string {
  return Number(v || 0).toFixed(2)
}
</script>
