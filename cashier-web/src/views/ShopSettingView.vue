<template>
  <!--
    W-23 门店营业设置（Ardot 2:1434，内容区 2:1437）
    双列 1170（左 577 ｜ 右 577，gap 16）
      左：营业状态卡 150 ｜ 订单提醒卡 130
      右：订单设置卡 190 ｜ 打印设置卡 250
    ⚠️ 与画板的三处差异（已核实，非偷懒）：
      ① 「低消校验」开关**不做** —— 需求书 V7 变更一明令取消最低消费校验（同批次 C 的 W-08 裁决）
      ② 「云打印（芯烨云 · 服务费 ¥0）」文案去掉「芯烨云 · 服务费 ¥0」——
         云打印平台账号未就绪，品牌与服务费无从确认；开关只代表**意愿**，不代表可下发
      ③ 「营业状态」的「最后切换 17:02」需要落库时间戳 —— 本页存 businessOpen +
         businessOpenChangedAt（前端在切换时写入），未切换过则显示「—」
  -->
  <div class="flex-1 min-h-0 overflow-auto p-5">
    <div class="flex" style="gap: 16px">
      <!-- ================= 左列 577 ================= -->
      <div class="flex flex-col shrink-0" style="width: 577px; gap: 12px">
        <!-- 营业状态卡 2:1442 -->
        <div class="rounded-lg flex flex-col" style="padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
          <span class="text-[15px] font-medium" style="color: var(--tc-title)">营业状态（C-03 数据源）</span>
          <div class="flex items-center" style="height: 29px">
            <span class="text-[15px] font-medium" :style="{ color: s.businessOpen ? '#008000' : 'var(--warn)' }">
              {{ s.businessOpen ? '营业中' : '已打烊' }}
              <template v-if="s.businessOpenChangedAt"> · 最后切换 {{ hhmm(s.businessOpenChangedAt) }}</template>
            </span>
            <button
              class="ml-auto rounded-full relative shrink-0 disabled:opacity-50"
              :style="{ width: '44px', height: '24px', background: s.businessOpen ? 'var(--p)' : 'var(--tc-line)' }"
              :disabled="saving"
              @click="toggleBusiness"
            >
              <span
                class="absolute rounded-full bg-white"
                :style="{ width: '20px', height: '20px', top: '2px', left: s.businessOpen ? '22px' : '2px', transition: 'left .15s' }"
              />
            </button>
          </div>
          <span class="text-[12px]" style="color: var(--tc-sub)">
            打烊后客户端（C-03）立即显示「已打烊」，不可下单
          </span>
        </div>

        <!-- 订单提醒卡 2:1448 -->
        <div class="rounded-lg flex flex-col" style="padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
          <span class="text-[15px] font-medium" style="color: var(--tc-title)">订单提醒</span>
          <div class="flex items-center" style="height: 24px">
            <span class="text-[12px]" style="color: var(--txt)">
              新订单语音提醒
              <button class="ml-2 text-[12px] underline" style="color: var(--p)" @click="previewRing">铃声试听</button>
              <span class="ml-2">· 音量 {{ s.voiceVolume }}%</span>
            </span>
            <button
              class="ml-auto rounded-full relative shrink-0 disabled:opacity-50"
              :style="{ width: '44px', height: '24px', background: s.orderVoiceAlert ? 'var(--p)' : 'var(--tc-line)' }"
              :disabled="saving"
              @click="toggle('orderVoiceAlert')"
            >
              <span
                class="absolute rounded-full bg-white"
                :style="{ width: '20px', height: '20px', top: '2px', left: s.orderVoiceAlert ? '22px' : '2px', transition: 'left .15s' }"
              />
            </button>
          </div>
          <div class="flex items-center" style="gap: 10px">
            <span class="text-[12px] shrink-0" style="color: var(--tc-sub)">音量</span>
            <input v-model.number="s.voiceVolume" type="range" min="0" max="100" step="10" style="flex: 1" />
          </div>
        </div>
      </div>

      <!-- ================= 右列 577 ================= -->
      <div class="flex flex-col shrink-0" style="width: 577px; gap: 12px">
        <!-- 订单设置卡 2:1453 -->
        <div class="rounded-lg flex flex-col" style="padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
          <span class="text-[15px] font-medium" style="color: var(--tc-title)">订单设置</span>

          <div v-for="row in ORDER_ROWS" :key="row.key" class="flex items-center" style="height: 24px">
            <span class="text-[12px]" style="color: var(--txt)">{{ row.label }}</span>
            <button
              class="ml-auto rounded-full relative shrink-0 disabled:opacity-50"
              :style="{ width: '44px', height: '24px', background: s[row.key] ? 'var(--p)' : 'var(--tc-line)' }"
              :disabled="saving"
              @click="toggle(row.key)"
            >
              <span
                class="absolute rounded-full bg-white"
                :style="{ width: '20px', height: '20px', top: '2px', left: s[row.key] ? '22px' : '2px', transition: 'left .15s' }"
              />
            </button>
          </div>

          <div class="flex items-center" style="height: 24px; gap: 10px">
            <span class="text-[12px]" style="color: var(--txt)">出品超时提醒</span>
            <input
              v-model.number="s.productionTimeoutMin"
              type="number"
              :min="1"
              class="rounded border text-[12px] outline-none"
              style="width: 80px; height: 26px; padding: 0 8px; border-color: var(--tc-line)"
            />
            <span class="text-[12px]" style="color: var(--tc-sub)">分钟</span>
          </div>

          <p class="text-[11px]" style="color: var(--tc-hold)">
            画板的「低消校验」开关**未实现**：需求书 V7 变更一已取消最低消费校验（同批次 C 的 W-08 裁决）
          </p>
        </div>

        <!-- 打印设置卡 2:1464 -->
        <div class="rounded-lg flex flex-col" style="padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 12px">
          <span class="text-[15px] font-medium" style="color: var(--tc-title)">打印设置（P-01 / P-02 / 4.5）</span>

          <div class="flex items-center" style="height: 24px">
            <span class="text-[12px]" style="color: var(--txt)">云打印</span>
            <button
              class="ml-auto rounded-full relative shrink-0 disabled:opacity-50"
              :style="{ width: '44px', height: '24px', background: s.cloudPrint ? 'var(--p)' : 'var(--tc-line)' }"
              :disabled="saving"
              @click="toggle('cloudPrint')"
            >
              <span class="absolute rounded-full bg-white" :style="{ width: '20px', height: '20px', top: '2px', left: s.cloudPrint ? '22px' : '2px', transition: 'left .15s' }" />
            </button>
          </div>
          <div class="flex items-center" style="height: 24px">
            <span class="text-[12px]" style="color: var(--txt)">本地打印</span>
            <button
              class="ml-auto rounded-full relative shrink-0 disabled:opacity-50"
              :style="{ width: '44px', height: '24px', background: s.localPrint ? 'var(--p)' : 'var(--tc-line)' }"
              :disabled="saving"
              @click="toggle('localPrint')"
            >
              <span class="absolute rounded-full bg-white" :style="{ width: '20px', height: '20px', top: '2px', left: s.localPrint ? '22px' : '2px', transition: 'left .15s' }" />
            </button>
          </div>

          <div class="flex items-center" style="height: 34px">
            <span class="text-[12px]" style="color: var(--txt)">接单打印模式（P-02）</span>
            <select
              v-model="s.printMode"
              class="ml-auto rounded border text-[12px] outline-none"
              style="width: 200px; height: 34px; padding: 0 10px; border-color: var(--tc-line)"
              @change="saveNow"
            >
              <option value="afterConfirm">前台确认后打印</option>
              <option value="onAccept">接单立即打印</option>
              <option value="manual">仅手动补打</option>
            </select>
          </div>

          <div class="flex items-center" style="height: 20px">
            <span class="text-[12px]" style="color: var(--txt)">默认打印机</span>
            <select
              v-model="s.defaultPrinterId"
              class="ml-auto rounded border text-[12px] outline-none"
              style="width: 200px; height: 30px; padding: 0 10px; border-color: var(--tc-line)"
              @change="saveNow"
            >
              <option value="">（未设置）</option>
              <option v-for="p in printers" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
            </select>
          </div>

          <p class="text-[11px]" style="color: var(--tc-hold)">
            云打印开关只代表**意愿**：真正的下发需云打印平台账号（P-01），目前未就绪。
          </p>
        </div>
      </div>
    </div>

    <p class="mt-3 text-[11px]" style="color: var(--tc-hold)">
      {{ status }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchShopSettings, saveShopSettings } from '@/api/shopSetting'
import { fetchPrinterList, type PrinterRow } from '@/api/printer'

/** 订单设置卡的可开关行（「低消校验」已按 V7 移除，不在此列） */
const ORDER_ROWS = [
  { key: 'autoAccept', label: '自动接单' }
] as const

const printers = ref<PrinterRow[]>([])
const saving = ref(false)
const status = ref('')

/** 前端持有带类型的状态；后端统一存字符串，这里做双向转换 */
const s = reactive({
  businessOpen: true,
  businessOpenChangedAt: '' as string,
  orderVoiceAlert: true,
  voiceVolume: 70,
  autoAccept: false,
  productionTimeoutMin: 10,
  cloudPrint: true,
  localPrint: false,
  printMode: 'afterConfirm',
  defaultPrinterId: '' as string
})

onMounted(async () => {
  try {
    const [raw, printerPage] = await Promise.all([fetchShopSettings(), fetchPrinterList()])
    printers.value = printerPage?.records ?? []
    apply(raw ?? {})
    status.value = Object.keys(raw ?? {}).length
      ? `已从服务端载入 ${Object.keys(raw ?? {}).length} 项设置`
      : '服务端暂无设置，当前显示的是默认值（改动任意项即会落库）'
  } catch (e) {
    status.value = `设置加载失败：${(e as Error).message || ''}（若为「表不存在」，请先执行 sql/mysql/cloudrun/22-门店设置表.sql）`
  }
})

function apply(raw: Record<string, string>) {
  const bool = (v: string | undefined, dft: boolean) => (v == null || v === '' ? dft : v === 'true')
  const num = (v: string | undefined, dft: number) => (v == null || v === '' ? dft : Number(v) || dft)
  s.businessOpen = bool(raw.businessOpen, true)
  s.businessOpenChangedAt = raw.businessOpenChangedAt ?? ''
  s.orderVoiceAlert = bool(raw.orderVoiceAlert, true)
  s.voiceVolume = num(raw.voiceVolume, 70)
  s.autoAccept = bool(raw.autoAccept, false)
  s.productionTimeoutMin = num(raw.productionTimeoutMin, 10)
  s.cloudPrint = bool(raw.cloudPrint, true)
  s.localPrint = bool(raw.localPrint, false)
  s.printMode = raw.printMode || 'afterConfirm'
  s.defaultPrinterId = raw.defaultPrinterId ?? ''
}

/** 组装要提交的键值对（一律字符串） */
function payload(): Record<string, string> {
  return {
    businessOpen: String(s.businessOpen),
    businessOpenChangedAt: s.businessOpenChangedAt || '',
    orderVoiceAlert: String(s.orderVoiceAlert),
    voiceVolume: String(s.voiceVolume),
    autoAccept: String(s.autoAccept),
    productionTimeoutMin: String(s.productionTimeoutMin),
    cloudPrint: String(s.cloudPrint),
    localPrint: String(s.localPrint),
    printMode: s.printMode,
    defaultPrinterId: s.defaultPrinterId
  }
}

async function saveNow() {
  if (saving.value) return
  saving.value = true
  try {
    await saveShopSettings(payload())
    status.value = `已保存 · ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`
  } catch (e) {
    status.value = `保存失败：${(e as Error).message || ''}`
  } finally {
    saving.value = false
  }
}

async function toggle(key: 'orderVoiceAlert' | 'cloudPrint' | 'localPrint' | 'autoAccept') {
  s[key] = !s[key]
  await saveNow()
}

async function toggleBusiness() {
  s.businessOpen = !s.businessOpen
  s.businessOpenChangedAt = new Date().toISOString()
  await saveNow()
}

/** 铃声试听：用 WebAudio 合成两声短提示音（不引入音频文件，避免额外资源与版权问题） */
function previewRing() {
  try {
    const Ctx = (window as unknown as { AudioContext?: typeof AudioContext; webkitAudioContext?: typeof AudioContext })
    const AC = Ctx.AudioContext || Ctx.webkitAudioContext
    if (!AC) {
      status.value = '当前浏览器不支持音频合成，无法试听'
      return
    }
    const ctx = new AC()
    const gain = ctx.createGain()
    gain.gain.value = Math.max(0.02, s.voiceVolume / 100)
    gain.connect(ctx.destination)
    ;[0, 0.22].forEach(offset => {
      const osc = ctx.createOscillator()
      osc.frequency.value = 880
      osc.connect(gain)
      osc.start(ctx.currentTime + offset)
      osc.stop(ctx.currentTime + offset + 0.15)
    })
    setTimeout(() => void ctx.close(), 800)
  } catch (e) {
    status.value = `试听失败：${(e as Error).message || ''}`
  }
}

function hhmm(t: string): string {
  const m = String(t).match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : '—'
}
</script>
