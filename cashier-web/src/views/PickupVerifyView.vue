<template>
  <!--
    W-15 自提订单核销（Ardot 2:1028，内容区 2:1031）
    面包屑 40 ｜ 核销-双栏 1170×620（扫码核销区 680 + 待核销列表 474，gap 16）｜ 反馈条 1170×46
    ⚠️ 与画板的两处差异（都因工程无数据源，不放假按钮/不编文案）：
      ① 反馈条不写「已打印自提小票」—— 打印属批次 G，未实现，写了就是假话
      ② 核销方式（扫码/手输）工程无字段，页面不做该标识
    ★ 2026-09-27 用户拍板：自提场景摄像头扫码多余 —— 核销以「自提码 + 订单号 + 下单用户」人工核对为准，
      扫码区整体隐藏（SCAN_ENABLED=false，代码保留，改回 true 即恢复）
  -->
  <div class="flex-1 min-h-0 flex flex-col p-5 gap-3">
    <!-- 核销-双栏 2:1033 -->
    <div class="flex-1 min-h-0 flex" style="gap: 16px">
      <!-- ============ 左：扫码核销区 680 ============ -->
      <div
        class="rounded-lg flex flex-col items-center justify-center shrink-0"
        style="width: 680px; padding: 20px; background: #fff; border: 1px solid var(--tc-line); gap: 16px"
      >
        <!-- 扫码框 420×260（2026-09-27 起 SCAN_ENABLED=false 隐藏；恢复改 true 即可） -->
        <div
          v-if="SCAN_ENABLED"
          class="rounded-xl flex flex-col items-center justify-center"
          style="width: 420px; height: 260px; background: #F5F7FA; gap: 12px"
        >
          <template v-if="scanning">
            <video ref="videoEl" autoplay muted playsinline style="width: 380px; height: 200px; object-fit: cover; border-radius: 8px" />
            <span class="text-[12px]" style="color: var(--tc-sub)">对准顾客自提码…</span>
          </template>
          <template v-else>
            <span class="text-[19px] font-medium" style="color: var(--tc-title)">请扫描顾客自提码</span>
            <span class="text-[12px] text-center" style="color: var(--tc-sub); padding: 0 40px">
              对准顾客小程序「我的存酒/商城订单」自提码
            </span>
            <button
              class="rounded text-[12px] text-white"
              style="height: 30px; padding: 0 16px; background: var(--p)"
              @click="startScan"
            >启用摄像头扫码</button>
            <span v-if="scanMsg" class="text-[11px] text-center" style="color: var(--tc-hold); padding: 0 24px">
              {{ scanMsg }}
            </span>
          </template>
        </div>

        <!-- 手输行 420×40 -->
        <div class="flex items-center" style="width: 420px; gap: 10px">
          <input
            v-model.trim="inputCode"
            class="rounded border text-[13px] outline-none"
            style="width: 300px; height: 40px; padding: 0 12px; border-color: var(--tc-line)"
            placeholder="输入自提码，如 PK202609150002"
            @keyup.enter="verifyByCode"
          />
          <button
            class="rounded text-[14px] font-medium text-white disabled:opacity-50"
            style="width: 110px; height: 40px; background: var(--p)"
            :disabled="!inputCode || busy"
            @click="verifyByCode"
          >核 销</button>
        </div>

        <span class="text-[11px]" style="color: var(--tc-hold)">
          幂等校验：已核销 / 不存在 / 已过期的码将拒绝并留痕（M-03）
        </span>
      </div>

      <!-- ============ 右：待核销列表 474 ============ -->
      <div
        class="rounded-lg flex flex-col min-h-0 shrink-0"
        style="width: 474px; padding: 14px; background: #fff; border: 1px solid var(--tc-line); gap: 10px"
      >
        <span class="text-[13px]" style="color: var(--tc-title)">待核销订单（{{ rows.length }}）</span>

        <div class="flex-1 min-h-0 overflow-y-auto flex flex-col" style="gap: 10px">
          <div v-if="loading" class="text-[12px]" style="color: var(--tc-sub)">加载中…</div>
          <div v-else-if="!rows.length" class="text-[12px]" style="color: var(--tc-sub)">
            暂无待核销的自提单
          </div>

          <div
            v-for="(o, i) in rows"
            v-else
            :key="o.id"
            class="rounded-md flex flex-col"
            :style="{
              padding: '12px',
              gap: '6px',
              background: i === 0 ? '#EDF2DE' : '#fff',
              border: '1px solid var(--tc-line)'
            }"
          >
            <span class="text-[15px] font-bold" style="color: var(--tc-title)">{{ o.pickupCode || '—' }}</span>
            <span class="text-[12px]" style="color: var(--txt)">{{ decodeEntities(o.description) || '—' }}</span>
            <!-- 订单号：2026-09-27 人工核对三要素之一（自提码/订单号/下单用户），补显便于收银员对单 -->
            <span v-if="o.orderNo" class="text-[11px]" style="color: var(--tc-hold)">单号 {{ o.orderNo }}</span>
            <div class="flex items-center" style="height: 28px">
              <span class="text-[12px]" style="color: var(--tc-sub)">
                ¥{{ money(o.actualPrice) }} · {{ hhmm(o.createTime) }} 下单<template v-if="phoneTail(o.contactPhone)">
                  · 尾号 {{ phoneTail(o.contactPhone) }}</template>
              </span>
              <button
                class="ml-auto rounded text-[12px] text-white disabled:opacity-50"
                style="height: 28px; padding: 0 12px; background: var(--p)"
                :disabled="busy"
                @click="doVerify(o)"
              >核销</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 反馈条 1170×46 ============ -->
    <div
      class="rounded flex items-center shrink-0"
      :style="{ height: '46px', paddingLeft: '12px', gap: '10px', background: fb.bg }"
    >
      <span class="shrink-0" :style="{ width: '3px', height: '20px', borderRadius: '2px', background: fb.bar }" />
      <span class="text-[13px]" :style="{ color: fb.fg }">{{ fb.text }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  confirmPickup,
  fetchWaitPickupList,
  hhmm,
  phoneTail,
  type PickupOrderRow
} from '@/api/pickup'
import { decodeEntities } from '@/utils/text'

/** 扫码开关（2026-09-27 用户拍板：自提码扫码多余，先隐藏；改回 true 整体恢复，含摄像头逻辑） */
const SCAN_ENABLED = false

const rows = ref<PickupOrderRow[]>([])
const loading = ref(true)
const busy = ref(false)
const inputCode = ref('')

/** 反馈条：成功=主色浅底+成功色竖条；失败=危险浅底+危险色竖条 */
const feedback = ref<{ kind: 'idle' | 'ok' | 'fail'; text: string }>({
  kind: 'idle',
  text: '等待核销 —— 手输自提码或核对右侧待核销单（自提码/单号/尾号）后点「核销」，结果在此显示（小票打印属批次 G，本期未实现）'
})

const fb = computed(() => {
  if (feedback.value.kind === 'ok') return { bg: '#EDF2DE', bar: '#66C23B', fg: '#404A21', text: feedback.value.text }
  if (feedback.value.kind === 'fail') return { bg: '#FCE3E3', bar: '#F56B6B', fg: '#791F1F', text: feedback.value.text }
  return { bg: '#F5F7FA', bar: 'var(--tc-line)', fg: '#5F5E5A', text: feedback.value.text }
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await fetchWaitPickupList()
    rows.value = res?.records ?? []
  } catch (e) {
    console.error('[pickup] 待核销加载失败', e)
    feedback.value = { kind: 'fail', text: `待核销列表加载失败：${(e as Error).message || ''}` }
  } finally {
    loading.value = false
  }
}

/** 手输码核销：在已加载的待核销列表里精确匹配 → 匹配不到即「码不存在」 */
async function verifyByCode() {
  const code = inputCode.value.trim()
  if (!code) return
  const hit = rows.value.find(o => (o.pickupCode ?? '').trim() === code)
  if (!hit) {
    feedback.value = { kind: 'fail', text: `自提码 ${code} 核销失败：码不存在或该单已不在「待自提」（已核销 / 已过期会被拒绝）` }
    return
  }
  await doVerify(hit)
}

async function doVerify(o: PickupOrderRow) {
  if (busy.value) return
  busy.value = true
  try {
    await confirmPickup(o.id)
    const time = nowHHmm()
    feedback.value = { kind: 'ok', text: `${o.pickupCode || o.orderNo || o.id} 核销成功 ${time}` }
    inputCode.value = ''
    stopScan()
    await load()
  } catch (e) {
    feedback.value = {
      kind: 'fail',
      text: `${o.pickupCode || o.id} 核销失败：${(e as Error).message || '未知错误'}`
    }
  } finally {
    busy.value = false
  }
}

/* ------------------ 摄像头扫码（能力探测 + 诚实降级） ------------------ */
const scanning = ref(false)
const scanMsg = ref('')
const videoEl = ref<HTMLVideoElement | null>(null)
let stream: MediaStream | null = null
let raf = 0

async function startScan() {
  scanMsg.value = ''
  // 浏览器原生条码识别：Chrome/Edge 支持；Safari/Firefox 多数不支持 → 直接诚实告知
  const Detector = (window as unknown as { BarcodeDetector?: new (o?: unknown) => { detect: (s: unknown) => Promise<{ rawValue: string }[]> } }).BarcodeDetector
  if (!Detector) {
    scanMsg.value = '当前浏览器不支持摄像头扫码（BarcodeDetector 不可用），请使用下方手输自提码'
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    scanning.value = true
    await nextTick()
    if (videoEl.value) {
      videoEl.value.srcObject = stream
      await videoEl.value.play()
    }
    const detector = new Detector({ formats: ['qr_code', 'code_128'] })
    const tick = async () => {
      if (!scanning.value || !videoEl.value) return
      try {
        const codes = await detector.detect(videoEl.value)
        if (codes.length) {
          inputCode.value = codes[0].rawValue.trim()
          await verifyByCode()
          if (feedback.value.kind === 'ok') return
        }
      } catch {
        /* 单帧识别失败可忽略，继续下一帧 */
      }
      raf = requestAnimationFrame(() => void tick())
    }
    void tick()
  } catch (e) {
    scanning.value = false
    scanMsg.value = `无法访问摄像头：${(e as Error).message || ''}；请使用下方手输自提码`
  }
}

function stopScan() {
  scanning.value = false
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
}

function nowHHmm(): string {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}
</script>
