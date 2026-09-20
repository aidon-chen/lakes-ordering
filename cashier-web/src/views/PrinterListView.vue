<template>
  <!--
    W-20 小票打印机绑定（Ardot 2:1265，内容区 2:1268）
    工具条 40（名称180 / 编号180 / 查询88×36 + 空隙 + 新增打印机96×32）
    表格 1170×260：表头 40 + 行 52
    ⚠️ 与画板的有意差异（3 列删掉、其余列按 1170 重新分配）——原因见页面底注与代码注释
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 工具条 2:1270 -->
    <div class="shrink-0 flex items-center gap-3">
      <input
        v-model.trim="qName"
        class="rounded border text-[12px] outline-none"
        style="width: 180px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="打印机名称"
        @keyup.enter="load"
      />
      <input
        v-model.trim="qNumber"
        class="rounded border text-[12px] outline-none"
        style="width: 180px; height: 32px; padding: 0 10px; border-color: var(--tc-line)"
        placeholder="打印机编号"
        @keyup.enter="load"
      />
      <button
        class="rounded text-[13px] font-medium text-white"
        style="height: 36px; padding: 0 22px; background: var(--p)"
        @click="load"
      >查询</button>
      <span class="flex-1" />
      <button
        class="rounded text-[12px] text-white"
        style="height: 32px; padding: 0 16px; background: var(--p)"
        @click="openCreate"
      >新增打印机</button>
    </div>

    <!-- 表格 2:1279 -->
    <div class="flex-1 min-h-0 flex flex-col rounded overflow-hidden" style="border: 1px solid var(--tc-line)">
      <div class="shrink-0 flex items-center text-[12px]" style="height: 40px; background: var(--bg); color: var(--tc-title)">
        <span v-for="c in COLS" :key="c.key" class="px-2 truncate" :style="{ width: c.w + 'px' }">{{ c.label }}</span>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto">
        <div v-if="loading" class="p-4 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!rows.length" class="p-4 text-[12px]" style="color: var(--tc-sub)">
          暂无打印机 —— 点右上「新增打印机」录入云打印平台的设备编号与密钥
        </div>

        <div
          v-for="p in rows"
          v-else
          :key="p.id"
          class="flex items-center text-[12px]"
          style="height: 52px; border-top: 1px solid var(--tc-line)"
        >
          <span class="px-2 truncate" style="width: 260px; color: var(--txt)">{{ p.name }}</span>
          <!-- 编号 · 密钥（密钥脱敏）；密钥为空时只显示编号 -->
          <span class="px-2 truncate" style="width: 280px; color: var(--txt)">
            {{ p.number || '—' }}<template v-if="p.identifyingCode"> · {{ maskCode(p.identifyingCode) }}</template>
          </span>
          <span class="px-2 truncate" style="width: 180px; color: var(--txt)">{{ printerBrandText(p.brand) }}</span>
          <span class="px-2 truncate" style="width: 180px" :style="{ color: p.isAutoPrint ? '#008000' : 'var(--tc-sub)' }">
            {{ p.isAutoPrint ? '是' : '否' }}
          </span>
          <span class="px-2 flex items-center gap-3" style="width: 270px">
            <button class="text-[12px]" style="color: var(--p)" @click="openEdit(p)">编辑</button>
            <button class="text-[12px]" :style="{ color: 'var(--warn)' }" @click="askDelete(p)">删除</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 底注 2:1311（如实说明省略的列，而不是留空列） -->
    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">
      ⚠️ 画板的「设备状态/出品区归属/待重试任务」三列**本期未做**：设备运行态（在线·缺纸）需云打印平台回传
      （`tb_printer.cloud_registration_status` 是**预留的注册状态**、不是运行态）；出品区归属属 W-22 的映射设计；
      待重试任务属 W-25 的队列表（「缺表缺服务」）。画板样例品牌写「芯烨」，工程枚举实为 1=飞鹅 / 2=易联云，**以工程为准**。
      新增需录入云打印平台**设备编号与密钥**（5.4）；绑真机验证受云打印账号阻塞。
    </p>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <Teleport to="body">
      <div
        v-if="editing"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="editing = null"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(520px, calc(100vw - 32px))">
          <span class="text-[16px] font-bold" style="color: var(--tc-title)">
            {{ form.id ? '编辑打印机' : '新增打印机' }}
          </span>

          <div class="mt-4 flex flex-col" style="gap: 12px">
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">打印机名称 <b style="color: var(--warn)">*</b></span>
              <input v-model.trim="form.name" class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)" placeholder="如：大厅小票打印机" />
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">设备编号</span>
              <input v-model.trim="form.number" class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)" placeholder="云打印平台设备编号" />
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">密钥 / 识别码</span>
              <input v-model.trim="form.identifyingCode" class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)" placeholder="云打印平台密钥（保存后列表脱敏展示）" />
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">类型</span>
              <select v-model.number="form.type" class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)">
                <option :value="1">小票打印机</option>
                <option :value="2">标签打印机</option>
              </select>
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">品牌</span>
              <select v-model.number="form.brand" class="rounded border outline-none flex-1"
                style="height: 34px; padding: 0 10px; border-color: var(--tc-line)">
                <option :value="1">飞鹅打印机</option>
                <option :value="2">易联云打印机</option>
              </select>
            </label>
            <label class="flex items-center text-[12px]" style="gap: 10px">
              <span class="shrink-0" style="width: 96px; color: var(--txt)">接单自动打印</span>
              <input v-model="form.isAutoPrint" type="checkbox" />
              <span style="color: var(--tc-sub)">勾选后接单自动出小票（P-02）</span>
            </label>
          </div>

          <p v-if="formErr" class="mt-3 text-[12px]" style="color: var(--warn)">{{ formErr }}</p>

          <div class="mt-5 flex justify-end gap-2">
            <button class="rounded border text-[13px]"
              style="height: 34px; padding: 0 16px; border-color: var(--tc-line); color: var(--tc-gray)"
              @click="editing = null">取消</button>
            <button class="rounded text-[13px] text-white disabled:opacity-50"
              style="height: 34px; padding: 0 18px; background: var(--p)"
              :disabled="!form.name || busy" @click="doSave">
              {{ busy ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  deletePrinters,
  fetchPrinterList,
  insertPrinter,
  printerBrandText,
  updatePrinter,
  type PrinterRow
} from '@/api/printer'

/**
 * 列宽：保留画板有真实数据源的 4 列 + 操作列，按 1170 重新分配
 * （画板原为 160/170/110/110/130/130/120/240，其中「设备状态/出品区归属/待重试任务」无数据源）
 */
const COLS = [
  { key: 'name', label: '名称', w: 260 },
  { key: 'number', label: '编号 / 密钥', w: 280 },
  { key: 'brand', label: '品牌', w: 180 },
  { key: 'auto', label: '自动打印', w: 180 },
  { key: 'op', label: '操作', w: 270 }
] as const

const rows = ref<PrinterRow[]>([])
const loading = ref(true)
const busy = ref(false)
const qName = ref('')
const qNumber = ref('')

interface Form {
  id?: number
  name: string
  number: string
  identifyingCode: string
  type: number
  brand: number
  isAutoPrint: boolean
}
const editing = ref<PrinterRow | null>(null)
const form = ref<Form>(blankForm())
const formErr = ref('')

onMounted(load)

function blankForm(): Form {
  return { name: '', number: '', identifyingCode: '', type: 1, brand: 1, isAutoPrint: true }
}

async function load() {
  loading.value = true
  try {
    const res = await fetchPrinterList({ name: qName.value || undefined, number: qNumber.value || undefined })
    rows.value = res?.records ?? []
  } catch (e) {
    console.error('[printer] 加载失败', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = {} as PrinterRow
  form.value = blankForm()
  formErr.value = ''
}

function openEdit(p: PrinterRow) {
  editing.value = p
  form.value = {
    id: p.id,
    name: p.name ?? '',
    number: p.number ?? '',
    identifyingCode: p.identifyingCode ?? '',
    type: p.type ?? 1,
    brand: p.brand ?? 1,
    isAutoPrint: !!p.isAutoPrint
  }
  formErr.value = ''
}

async function doSave() {
  if (!form.value.name) return
  busy.value = true
  formErr.value = ''
  const f = form.value
  const payload = {
    ...(f.id ? { id: f.id } : {}),
    name: f.name,
    number: f.number || undefined,
    identifyingCode: f.identifyingCode || undefined,
    type: f.type,
    brand: f.brand,
    isAutoPrint: f.isAutoPrint
  }
  try {
    if (f.id) await updatePrinter(payload)
    else await insertPrinter(payload)
    editing.value = null
    await load()
  } catch (e) {
    formErr.value = (e as Error).message || '保存失败'
  } finally {
    busy.value = false
  }
}

async function askDelete(p: PrinterRow) {
  if (!confirm(`确定删除打印机「${p.name}」？\n删除后归属该设备的打印任务将无法投递。`)) return
  try {
    await deletePrinters([p.id])
    await load()
  } catch (e) {
    alert((e as Error).message || '删除失败')
  }
}

/** 密钥脱敏：保留首尾各 4 位，中间打码（不完整回显云平台密钥） */
function maskCode(code: string): string {
  if (code.length <= 8) return code.replace(/.(?=.{2})/g, '*')
  return `${code.slice(0, 4)}****${code.slice(-4)}`
}
</script>
