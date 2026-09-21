<template>
  <!--
    W-22 出品区分单规则（Ardot 2:1411，内容区 2:1414）
    面包屑 40 ｜ 说明条 40 ｜ 映射卡 250 ｜ 归属卡 360
    ⚠️ 与画板的**关键差异**（工程无「出品区」概念，不硬造也不放假数据）：
      工程里 tb_goods 只有 `printer_id` + `print_num`（商品直接挂打印机），
      **没有「出品区」实体、分类也没有打印绑定**（全仓搜索确认）。
      → 本页把画板的「出品区 → 打印机」改为 **「打印机 ← 覆盖分类」**（由商品实测聚合同一张表得出），
        路由单元即打印机本身；画板的「新增出品区」钮不放（没有实体可加）。
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3 overflow-auto">
    <!-- 说明条 2:1416 -->
    <div
      class="rounded flex items-center shrink-0"
      style="height: 40px; padding-left: 12px; background: #EDF2DE"
    >
      <span class="text-[12px]" style="color: #404A21">
        分单会把同一订单按**商品归属的打印机**拆成多张票分别下发（P-04）；未映射打印机的商品默认归吧台。
        工程无「出品区」实体，故路由单元＝打印机本身。
      </span>
    </div>

    <!-- 映射卡 2:1418 -->
    <div class="rounded shrink-0" style="background: #fff; border: 1px solid var(--tc-line)">
      <div class="flex items-center" style="height: 40px; padding: 0 16px; background: var(--bg)">
        <span class="text-[13px]" style="color: var(--tc-title)">打印机 ← 覆盖分类</span>
        <span class="ml-auto text-[11px]" style="color: var(--tc-hold)">由商品归类实测聚合（非独立配置表）</span>
      </div>

      <div v-if="loading" class="text-[12px]" style="padding: 12px 16px; color: var(--tc-sub)">加载中…</div>
      <div v-else-if="!printers.length" class="text-[12px]" style="padding: 12px 16px; color: var(--tc-sub)">
        还没有配置打印机 —— 先到「小票打印机」页新增设备
      </div>

      <div
        v-for="p in printerRoutes"
        v-else
        :key="p.id"
        class="text-[12px]"
        style="padding: 8px 16px; border-top: 1px solid var(--tc-line); color: var(--txt)"
      >
        {{ p.name }}
        <template v-if="p.categories.length">
          （{{ p.categories.join(' / ') }}）→ {{ p.goodsCount }} 个商品
        </template>
        <template v-else>
          —— 暂无商品归属
        </template>
        <span class="ml-2" :style="{ color: p.isAutoPrint ? '#008000' : 'var(--tc-sub)' }">
          · {{ p.isAutoPrint ? '接单自动打印' : '不自动打印' }}
        </span>
      </div>
    </div>

    <!-- 归属卡 2:1426 -->
    <div class="rounded flex-1 min-h-0 flex flex-col" style="background: #fff; border: 1px solid var(--tc-line)">
      <div class="flex items-center shrink-0" style="height: 40px; padding: 0 16px; background: var(--bg); gap: 12px">
        <span class="text-[13px]" style="color: var(--tc-title)">商品归属（商品 / 分类 / 打印机 / 打印份数）</span>
        <span class="ml-auto text-[11px]" style="color: var(--tc-hold)">仅显示前 {{ Math.min(filtered.length, 60) }} 条，可用分类筛选</span>
        <select
          v-model="menuFilter"
          class="rounded border text-[12px] outline-none shrink-0"
          style="height: 28px; padding: 0 8px; border-color: var(--tc-line); color: var(--tc-gray)"
        >
          <option :value="null">全部分类</option>
          <option v-for="m in menus" :key="m.id" :value="m.id">{{ m.name }}</option>
        </select>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto">
        <div
          v-for="g in filtered.slice(0, 60)"
          :key="g.id"
          class="flex items-center text-[12px]"
          style="height: 44px; border-top: 1px solid var(--tc-line)"
        >
          <span class="px-4 truncate" style="width: 380px; color: var(--txt)">{{ g.name }}</span>
          <span class="px-2 truncate" style="width: 180px" :style="{ color: g.menuName ? 'var(--txt)' : 'var(--tc-hold)' }">
            {{ g.menuName || '未分类' }}
          </span>
          <span class="px-2 truncate" style="width: 260px" :style="{ color: printerName(g) ? 'var(--txt)' : 'var(--tc-hold)' }">
            {{ printerName(g) || '（未映射 → 默认吧台）' }}
          </span>
          <span class="px-2 truncate" style="width: 120px; color: var(--txt)">{{ g.printNum ?? 1 }} 份</span>
          <span class="px-2 flex items-center" style="flex: 1">
            <button class="text-[12px]" style="color: var(--p)" @click="openEdit(g)">编辑</button>
          </span>
        </div>
      </div>
    </div>

    <!-- ============ 编辑归属弹窗 ============ -->
    <Teleport to="body">
      <div
        v-if="editRow"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="editRow = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(460px, calc(100vw - 32px))">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">编辑打印归属</span>
          <p class="mt-1 text-[12px]" style="color: var(--tc-sub)">{{ editRow.name }}</p>

          <div class="mt-4 flex flex-col" style="gap: 12px">
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 88px; color: var(--txt)">打印机</span>
              <select
                v-model="editPrinterId"
                class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)"
              >
                <option value="">（不映射 → 默认吧台）</option>
                <option v-for="p in printers" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
              </select>
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 88px; color: var(--txt)">打印份数</span>
              <input
                v-model.number="editPrintNum"
                type="number"
                :min="1"
                class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)"
              />
            </label>
          </div>

          <p v-if="editErr" class="mt-3 text-[12px]" style="color: var(--warn)">{{ editErr }}</p>

          <div class="mt-5 flex justify-end gap-2">
            <button
              class="rounded border text-[13px]"
              style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
              @click="editRow = null"
            >取消</button>
            <button
              class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="saving"
              @click="doSave"
            >{{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchGoods, fetchMenus, updateGoodsRouting, type GoodsRow, type MenuRow } from '@/api/menu'
import { fetchPrinterList, type PrinterRow } from '@/api/printer'

const goods = ref<GoodsRow[]>([])
const menus = ref<MenuRow[]>([])
const printers = ref<PrinterRow[]>([])
const loading = ref(true)
const menuFilter = ref<number | null>(null)

const editRow = ref<GoodsRow | null>(null)
const editPrinterId = ref('')
const editPrintNum = ref(1)
const editErr = ref('')
const saving = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [goodsPage, menusPage, printerPage] = await Promise.all([
      fetchGoods(),
      fetchMenus(),
      fetchPrinterList()
    ])
    goods.value = goodsPage?.records ?? []
    menus.value = menusPage?.records ?? []
    printers.value = printerPage?.records ?? []
  } catch (e) {
    console.error('[routing] 加载失败', e)
  } finally {
    loading.value = false
  }
}

const filtered = computed(() =>
  menuFilter.value == null ? goods.value : goods.value.filter(g => g.menuId === menuFilter.value)
)

/** 打印机 ← 覆盖分类（由商品聚合，替代画板的「出品区 → 打印机」） */
const printerRoutes = computed(() =>
  printers.value.map(p => {
    const hit = goods.value.filter(g => String(g.printerId ?? '') === String(p.id))
    const cats = [...new Set(hit.map(g => g.menuName).filter(Boolean))] as string[]
    return { id: p.id, name: p.name, isAutoPrint: !!p.isAutoPrint, categories: cats, goodsCount: hit.length }
  })
)

/** 商品当前归属的打印机名（未映射则为空 → 页面显示「默认吧台」） */
function printerName(g: GoodsRow): string {
  const id = String(g.printerId ?? '')
  if (!id) return ''
  return printers.value.find(p => String(p.id) === id)?.name ?? `#${id}`
}

function openEdit(g: GoodsRow) {
  editRow.value = g
  editPrinterId.value = String(g.printerId ?? '')
  editPrintNum.value = g.printNum ?? 1
  editErr.value = ''
}

async function doSave() {
  const g = editRow.value
  if (!g || saving.value) return
  saving.value = true
  editErr.value = ''
  try {
    await updateGoodsRouting(
      g.id,
      editPrinterId.value ? editPrinterId.value : null,
      Number(editPrintNum.value) || 1
    )
    editRow.value = null
    await load()
  } catch (e) {
    editErr.value = (e as Error).message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>
