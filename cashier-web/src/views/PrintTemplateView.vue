<template>
  <!--
    W-21 打印模板与补打记录（Ardot 2:1358，内容区 2:1361）
    模板-双栏 1170×460（配置卡 460 + 预览卡 694, gap 16）｜ 补打记录卡 1170×280
    配置卡 5 行：模板类型 / 小票规格(58·80) / 页脚文字 / 打印份数 / 二维码 + 按钮行（保存 · 设为默认）
    ⚠️ 预览为**样例渲染**：本页没有订单上下文，故用样例单号与品项（与画板一致），不是真实数据
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3 overflow-auto">
    <!-- 模板-双栏 2:1363 -->
    <div class="flex shrink-0" style="gap: 16px">
      <!-- ============ 配置卡 460 ============ -->
      <div class="rounded-lg flex flex-col shrink-0" style="width: 460px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
        <span class="text-[15px] font-medium" style="color: var(--tc-title)">模板配置（P-05 · 58 / 80mm 双规格）</span>

        <!-- 行1 模板类型 -->
        <div class="flex items-center" style="gap: 10px; height: 36px">
          <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">模板类型</span>
          <select
            v-model.number="form.scene"
            class="rounded border text-[12px] outline-none"
            style="width: 200px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
            @change="onSceneChange"
          >
            <option v-for="t in TICKET_TYPES" :key="t.code" :value="t.code">{{ t.text }}</option>
          </select>
          <span v-if="currentIsDefault" class="text-[11px]" style="color: #008000">（默认）</span>
        </div>

        <!-- 行2 小票规格 -->
        <div class="flex items-center" style="gap: 10px; height: 32px">
          <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">小票规格</span>
          <button
            v-for="w in [58, 80]"
            :key="w"
            class="rounded text-[12px]"
            :style="{
              width: '80px', height: '32px',
              background: form.paperWidth === w ? 'var(--p)' : '#fff',
              color: form.paperWidth === w ? '#fff' : 'var(--tc-gray)',
              border: '1px solid var(--tc-line)'
            }"
            @click="form.paperWidth = w"
          >{{ w }}mm</button>
        </div>

        <!-- 行3 页脚文字 -->
        <div class="flex items-center" style="gap: 10px; height: 36px">
          <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">页脚文字</span>
          <input
            v-model.trim="form.footerText"
            class="rounded border text-[12px] outline-none"
            style="width: 300px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
            placeholder="如：感谢光临，请勿酒后驾车"
          />
        </div>

        <!-- 行4 打印份数 -->
        <div class="flex items-center" style="gap: 10px; height: 36px">
          <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">打印份数</span>
          <input
            v-model.number="form.copies"
            type="number"
            :min="1"
            class="rounded border text-[12px] outline-none"
            style="width: 100px; height: 36px; padding: 0 10px; border-color: var(--tc-line)"
          />
        </div>

        <!-- 行5 二维码 -->
        <div class="flex items-center" style="gap: 10px; height: 24px">
          <span class="text-[12px] shrink-0" style="width: 90px; color: var(--txt)">二维码</span>
          <button
            class="rounded-full relative"
            :style="{ width: '44px', height: '24px', background: form.showQr ? 'var(--p)' : 'var(--tc-line)' }"
            @click="form.showQr = !form.showQr"
          >
            <span class="absolute rounded-full bg-white" :style="{ width: '20px', height: '20px', top: '2px', left: form.showQr ? '22px' : '2px', transition: 'left .15s' }" />
          </button>
        </div>

        <!-- 按钮行 -->
        <div class="flex items-center" style="gap: 12px; height: 38px">
          <button
            class="rounded text-[13px] font-medium text-white disabled:opacity-50"
            style="width: 110px; height: 38px; background: var(--p)"
            :disabled="saving"
            @click="doSave"
          >{{ saving ? '保存中…' : '保存' }}</button>
          <button
            class="rounded border text-[13px] disabled:opacity-40"
            style="width: 110px; height: 38px; border-color: var(--tc-line); color: var(--tc-gray)"
            :disabled="!form.id || saving || currentIsDefault"
            @click="doSetDefault"
          >设为默认</button>
          <button
            v-if="form.id"
            class="text-[12px] disabled:opacity-40"
            style="color: var(--warn)"
            :disabled="saving || currentIsDefault"
            @click="doDelete"
          >删除</button>
        </div>

        <p v-if="err" class="text-[12px]" style="color: var(--warn)">{{ err }}</p>
      </div>

      <!-- ============ 预览卡 694 ============ -->
      <div class="rounded-lg flex flex-col shrink-0 items-center" style="width: 694px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 10px">
        <span class="text-[13px]" style="color: var(--txt)">小票实时预览（{{ form.paperWidth }}mm）</span>

        <!-- 小票纸样 300×360 -->
        <div
          class="flex flex-col"
          :style="{
            width: form.paperWidth === 80 ? '360px' : '300px',
            minHeight: '360px',
            padding: '14px',
            background: '#fff',
            border: '1px solid var(--tc-line)',
            gap: '8px'
          }"
        >
          <span class="text-[15px] font-bold" style="color: var(--tc-title)">{{ shopName }}</span>
          <span class="text-[12px]" style="color: var(--txt)">{{ sceneText }} SO202609150038</span>
          <span style="height: 1px; background: var(--tc-line)" />
          <div class="flex flex-col text-[12px]" style="color: var(--txt); gap: 4px">
            <div class="flex"><span>威士忌(单杯·加冰)×2</span><span class="ml-auto">¥136.00</span></div>
            <div class="flex"><span>时令果盘(大)×1</span><span class="ml-auto">¥68.00</span></div>
          </div>
          <span style="height: 1px; background: var(--tc-line)" />
          <div class="flex text-[13px] font-bold" style="color: var(--tc-title)">
            <span>实收</span><span class="ml-auto">¥204.00</span>
          </div>
          <span class="text-[12px]" style="color: var(--tc-sub)">台位 A03 · 2026-09-15 21:05</span>
          <div v-if="form.showQr" class="rounded" style="width: 64px; height: 64px; background: #F5F7FA; display: flex; align-items: center; justify-content: center">
            <span class="text-[10px]" style="color: var(--tc-hold)">二维码</span>
          </div>
          <span class="text-[12px]" style="color: var(--tc-sub)">{{ form.footerText || '（未设置页脚）' }}</span>
          <span class="text-[11px]" style="color: var(--tc-hold)">份数 ×{{ form.copies }}</span>
        </div>

        <p class="text-[11px] text-center" style="color: var(--tc-hold)">
          预览为**样例渲染**（本页无订单上下文）；真实小票内容由打印任务渲染后随任务下发。
        </p>
      </div>
    </div>

    <!-- ============ 补打记录卡 2:1405 ============ -->
    <div class="rounded-lg flex flex-col shrink-0" style="background: #fff; border: 1px solid var(--tc-line)">
      <div class="flex items-center shrink-0" style="height: 40px; padding: 0 16px; background: var(--bg)">
        <span class="text-[13px]" style="color: var(--tc-title)">补打 / 重打记录（P-06 · 补打记录可查询）</span>
        <span class="ml-auto text-[11px]" style="color: var(--tc-hold)">取打印任务中 isReprint=true 的流水</span>
      </div>
      <div style="max-height: 220px" class="overflow-y-auto">
        <div v-if="reprints.length === 0" class="text-[12px]" style="padding: 12px 16px; color: var(--tc-sub)">
          暂无补打记录
        </div>
        <div
          v-for="r in reprints"
          v-else
          :key="r.id"
          class="text-[12px]"
          style="padding: 8px 16px; border-top: 1px solid var(--tc-line)"
          :style="{ color: r.status === 4 ? 'var(--warn)' : 'var(--txt)' }"
        >
          {{ r.orderNo || '—' }} · {{ ticketTypeText(r.ticketType) }} · {{ r.printerName || '（未指定打印机）' }}
          · {{ r.copies ?? 1 }} 份 · {{ taskStatusText(r.status) }}<template v-if="r.status === 4 && r.failReason">
            （{{ r.failReason }}）</template><template v-if="(r.retryCount ?? 0) > 0">
            · 已重试 {{ r.retryCount }} 次</template>
          · {{ r.operatorName || '—' }} {{ hms(r.dispatchTime) }}
        </div>
      </div>
    </div>

    <p class="shrink-0 text-[11px]" style="color: var(--tc-hold)">{{ status }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  TICKET_TYPES,
  deletePrintTemplate,
  fetchPrintTasks,
  fetchPrintTemplates,
  savePrintTemplate,
  setDefaultPrintTemplate,
  taskStatusText,
  ticketTypeText,
  type PrintTaskRow,
  type PrintTemplateRow
} from '@/api/print'

const reprints = ref<PrintTaskRow[]>([])
const templates = ref<PrintTemplateRow[]>([])
const saving = ref(false)
const err = ref('')
const status = ref('')
const shopName = ref('本店')

const form = ref({
  id: null as number | null,
  scene: 3,
  paperWidth: 58,
  footerText: '感谢光临，请勿酒后驾车',
  copies: 1,
  showQr: true
})

const sceneText = computed(() => ticketTypeText(form.value.scene))
const currentIsDefault = computed(() => {
  const hit = templates.value.find(t => t.id === form.value.id)
  return !!hit?.isDefault
})

onMounted(load)

async function load() {
  try {
    const [tpl, taskPage] = await Promise.all([
      fetchPrintTemplates(),
      fetchPrintTasks({ isReprint: true, pageSize: 50 })
    ])
    templates.value = tpl ?? []
    reprints.value = taskPage?.records ?? []
    onSceneChange()
    status.value = templates.value.length
      ? `已载入 ${templates.value.length} 个模板`
      : '暂无模板 —— 配置好后点「保存」即新增（首次保存后可用「设为默认」）'
  } catch (e) {
    status.value = `加载失败：${(e as Error).message || ''}（若为「表不存在」，请先执行 sql/mysql/cloudrun/23-打印模块表.sql）`
  }
}

/** 切换小票类型 → 载入该类型的默认模板（没有则给一份带默认值的草稿） */
function onSceneChange() {
  err.value = ''
  const scene = form.value.scene
  const hit = templates.value.find(t => t.scene === scene && t.isDefault) ?? templates.value.find(t => t.scene === scene)
  if (hit) {
    form.value = {
      id: hit.id,
      scene: hit.scene ?? scene,
      paperWidth: hit.paperWidth ?? 58,
      footerText: hit.footerText ?? '',
      copies: hit.copies ?? 1,
      showQr: hit.showQr !== false
    }
  } else {
    form.value = { id: null, scene, paperWidth: 58, footerText: '感谢光临，请勿酒后驾车', copies: 1, showQr: true }
  }
}

async function doSave() {
  saving.value = true
  err.value = ''
  try {
    await savePrintTemplate({
      ...(form.value.id ? { id: form.value.id } : {}),
      scene: form.value.scene,
      paperWidth: form.value.paperWidth,
      footerText: form.value.footerText,
      copies: form.value.copies,
      showQr: form.value.showQr,
      name: `${sceneText.value} ${form.value.paperWidth}mm`
    })
    await load()
    status.value = '模板已保存'
  } catch (e) {
    err.value = (e as Error).message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function doSetDefault() {
  if (!form.value.id) return
  saving.value = true
  try {
    await setDefaultPrintTemplate(form.value.id)
    await load()
    status.value = '已设为该类型的默认模板'
  } catch (e) {
    err.value = (e as Error).message || '设置失败'
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  if (!form.value.id) return
  if (!confirm('确定删除该模板？默认模板需先把其它模板设为默认后才能删除。')) return
  saving.value = true
  try {
    await deletePrintTemplate(form.value.id)
    form.value.id = null
    await load()
  } catch (e) {
    err.value = (e as Error).message || '删除失败'
  } finally {
    saving.value = false
  }
}

function hms(t: string | null | undefined): string {
  const m = String(t ?? '').match(/(\d{1,2}):(\d{2})(?::(\d{2}))?/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : '—'
}
</script>
