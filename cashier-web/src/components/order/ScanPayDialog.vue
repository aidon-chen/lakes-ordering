<template>
  <!--
    扫码收款（付款码支付 · 被扫）2026-09-24 新增
    收银员扫顾客手机上的微信付款码 → 后端调微信 /pay/micropay 真扣款。

    四条录入方式（对应收银台真实硬件）：
      ① 扫码枪 —— 键盘模拟，聚焦输入框后直接扫，末尾回车触发（本组件默认焦点常驻）
      ② 摄像头 —— 双引擎：优先浏览器原生 BarcodeDetector；没有就上 ZXing（iPad/Safari/Firefox 可用）
      ③ 手输 —— 付款码看不清时兜底
      ④ 摄像头切换 / 补光灯 —— 多摄平板选后置、暗环境补光

    ⚠️ 摄像头硬性前提（缺一不可）：
      a. 必须 https:// 或 localhost（安全上下文）—— http://<局域网IP> 下浏览器把
         navigator.mediaDevices 直接设为 undefined，任何代码都调不起来
      b. 若收银台嵌在商家端页面里，父页 iframe 必须 allow="camera"，否则跨域策略掐掉摄像头
    两条都不满足时本组件**明说原因**，不摆假按钮。
  -->
  <div
    v-if="visible"
    class="fixed inset-0 z-50 flex items-center justify-center"
    style="background: rgba(0, 0, 0, 0.45)"
    @click.self="emit('close')"
  >
    <div
      class="rounded-lg bg-white flex flex-col"
      style="width: min(600px, 94vw); max-height: 94vh; overflow-y: auto; padding: 22px; gap: 14px"
    >
      <!-- 头：标题 + 金额 -->
      <div class="flex items-center">
        <span class="text-[16px] font-medium" style="color: var(--tc-title)">扫码收款</span>
        <span class="ml-auto text-[20px] font-medium" style="color: var(--p)">
          ¥{{ amount.toFixed(2) }}
        </span>
      </div>

      <!-- 扫码枪输入（默认焦点常驻：扫码枪是键盘设备，扫完自带回车） -->
      <div class="flex flex-col" style="gap: 6px">
        <div class="flex items-center" style="gap: 10px">
          <input
            ref="inputEl"
            v-model.trim="code"
            class="rounded border text-[14px] outline-none flex-1"
            style="height: 44px; padding: 0 12px; border-color: var(--tc-line)"
            placeholder="对准顾客付款码扫描（扫码枪直接扫）"
            autocomplete="off"
            @keyup.enter="submit"
          />
          <button
            class="rounded text-[13px] text-white disabled:opacity-50"
            style="height: 44px; padding: 0 18px; background: var(--p)"
            :disabled="busy || !code"
            @click="submit"
          >
            {{ busy ? '处理中…' : '确认收款' }}
          </button>
        </div>
        <span class="text-[11px]" style="color: var(--tc-sub)">
          扫码枪：直接扫，尾部回车自动提交 ｜ 手输：输入付款码后按回车
        </span>
      </div>

      <!-- 摄像头区 -->
      <div
        class="rounded-xl flex flex-col items-center justify-center"
        style="background: #f5f7fa; gap: 10px; padding: 12px"
      >
        <template v-if="scanning">
          <div style="position: relative; width: 100%">
            <video
              ref="videoEl"
              autoplay
              muted
              playsinline
              style="width: 100%; height: min(38vh, 300px); object-fit: cover; border-radius: 8px; background: #000"
            />
            <!-- 取景框：付款码是 1D 条码，务必让条码「横着」占满框 -->
            <div
              style="
                position: absolute;
                left: 8%;
                right: 8%;
                top: 32%;
                height: 36%;
                border: 2px solid rgba(95, 125, 34, 0.9);
                border-radius: 6px;
                pointer-events: none;
              "
            />
            <span
              style="
                position: absolute;
                left: 0;
                right: 0;
                bottom: 6px;
                text-align: center;
                font-size: 11px;
                color: #fff;
                text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
              "
            >
              让付款码横着占满绿框
            </span>
          </div>

          <!-- 引擎 + 设备控制 -->
          <div class="flex items-center w-full" style="gap: 8px">
            <span class="text-[11px]" style="color: var(--tc-sub)">
              识别引擎：{{ engineLabel }}
            </span>
            <select
              v-if="cameras.length > 1"
              v-model="cameraId"
              class="text-[11px] rounded border"
              style="height: 26px; padding: 0 4px; border-color: var(--tc-line); max-width: 150px"
              @change="switchCamera"
            >
              <option v-for="c in cameras" :key="c.id" :value="c.id">{{ c.label }}</option>
            </select>
            <button
              v-if="torchSupported"
              class="rounded text-[11px]"
              style="height: 26px; padding: 0 10px; border: 1px solid var(--tc-line); color: var(--tc-gray)"
              @click="toggleTorch"
            >
              {{ torchOn ? '关补光' : '开补光' }}
            </button>
            <button
              class="ml-auto rounded text-[11px]"
              style="height: 26px; padding: 0 12px; border: 1px solid var(--tc-line); color: var(--tc-gray)"
              @click="stopScan"
            >
              停止扫码
            </button>
          </div>

          <!-- 实时诊断行（★ 2026-09-25 加）：把「扫了没反应」变成看得见的三种原因，
               否则收银员/远程协作都只能靠猜。刻意放在画面正下方、不遮视频 -->
          <span
            class="text-[11px] text-center"
            style="line-height: 1.6; padding: 0 10px; color: var(--tc-sub)"
          >
            {{ diagText }}
          </span>
        </template>

        <template v-else>
          <span class="text-[13px]" style="color: var(--tc-sub)">或用平板/摄像头扫</span>
          <button
            class="rounded text-[12px] text-white"
            style="height: 32px; padding: 0 18px; background: var(--p)"
            @click="startScan"
          >
            启用摄像头扫码
          </button>
          <span v-if="scanMsg" class="text-[11px] text-center" style="color: var(--tc-hold); padding: 0 24px; line-height: 1.6">
            {{ scanMsg }}
          </span>
        </template>
      </div>

      <!-- 反馈条 -->
      <div
        v-if="message"
        class="rounded text-[12px] leading-relaxed"
        style="padding: 10px 12px"
        :style="feedbackStyle"
      >
        {{ message }}
      </div>

      <div class="flex items-center">
        <span class="text-[11px] leading-relaxed" style="color: var(--tc-sub)">
          收款成功后：订单→待处理（下厨）· 台位保持占用 · 用餐结束点「清台」
        </span>
        <button
          class="ml-auto rounded text-[13px]"
          style="height: 34px; padding: 0 18px; border: 1px solid var(--tc-line); color: var(--tc-gray)"
          @click="emit('close')"
        >
          取消
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
// 只引类型（编译期擦除，不会把 ZXing 拉进主包）；实现在 prepareDecoder 里动态 import
import type { ZxDecoder } from '@/utils/zxing-1d'

const props = defineProps<{
  visible: boolean
  amount: number
  busy?: boolean
  message?: string
  /** 反馈语气：info 一般提示 / warn 需重试（如 USERPAYING）/ error 失败 / ok 成功 */
  messageKind?: 'info' | 'warn' | 'error' | 'ok'
}>()

const emit = defineEmits<{ close: []; scan: [code: string] }>()

const code = ref('')
const inputEl = ref<HTMLInputElement | null>(null)

const feedbackStyle = computed(() => {
  switch (props.messageKind) {
    case 'ok':
      return 'background: var(--pl); color: var(--tc-title)'
    case 'warn':
      return 'background: var(--rsv-bg); color: #8a5a13'
    case 'error':
      return 'background: var(--clean-bg); color: #a32c2c'
    default:
      return 'background: #f5f7fa; color: var(--tc-gray)'
  }
})

function submit() {
  if (props.busy) return
  const v = code.value.trim()
  if (!v) return
  emit('scan', v)
}

/**
 * 扫码枪是「键盘设备」，靠输入框常驻焦点接收按键 → 只在该环境自动聚焦。
 * 平板上自动聚焦会弹出软键盘、挡住大半个界面（收银员还得先收键盘），反而碍事。
 * 用 `(hover: hover) and (pointer: fine)` 区分鼠标桌面 / 触屏平板。
 */
function focusInput() {
  if (window.matchMedia?.('(hover: hover) and (pointer: fine)').matches) inputEl.value?.focus()
}

/* ==================================================================
   摄像头扫码：双引擎
     · native：浏览器原生 BarcodeDetector（Chrome / Edge / 安卓 Chrome，最快）
     · zxing ：@zxing/library（iPad / Safari / Firefox 没有 BarcodeDetector 时兜底，纯 JS）
   ——付款码是 Code128 一维码，ZXing 1D 解码较吃 CPU，故按 ~8fps 节流而非逐帧狂解。
   ================================================================== */

/** 单帧解码间隔（ms）。1D 解码 CPU 开销大，节流比全速帧循环更稳、更省电（平板尤其明显） */
const DECODE_INTERVAL = 120

const scanning = ref(false)
const scanMsg = ref('')
const engine = ref<'' | 'native' | 'zxing'>('')
const videoEl = ref<HTMLVideoElement | null>(null)
const cameras = ref<{ id: string; label: string }[]>([])
const cameraId = ref('')
const torchOn = ref(false)
const torchSupported = ref(false)

let stream: MediaStream | null = null
let loopTimer: number | undefined
let decoding = false // 防重入：解码未回来之前不发起下一帧
let nativeDetector: { detect: (s: unknown) => Promise<{ rawValue: string }[]> } | null = null
let decoder: ZxDecoder | null = null
let canvas: HTMLCanvasElement | null = null
let cvsCtx: CanvasRenderingContext2D | null = null
let lastAccepted = '' // 同一张码不重复提交
/** 本次提交前摄像头是否在跑：失败/USERPAYING 时自动恢复，成功则不恢复 */
let resumeAfterBusy = false

/**
 * 诊断计数（★ 2026-09-25 加：平板实测「摄像头能开、但扫了没反应」）
 *
 * 原实现有个致命盲区：**「一帧都没解出」和「解出了但不是付款码」都返回 null**，
 * 界面上毫无区别 ⇒ 收银员只能看到「没反应」，排查时无从下手。
 * 这两个计数把两类原因分开：
 *   · `decodedFrames > 0` ⇒ 解码器在正常工作，问题在**内容/格式**（如扫到了二维码）
 *   · `decodedFrames === 0` ⇒ 解码器一帧都没解出 ⇒ **物理原因**：太远/虚焦/反光/码太小
 */
const decodedFrames = ref(0)
/** 最近一帧「解出了但不是付款码」的内容长度（⚠️ 只存长度不存原文：付款码是支付凭据） */
const lastOtherLen = ref(0)
/** 上一帧解出的是哪类码：'qr' 时提示「对准条形码」，'1d'/'native' 时提示「内容不是付款码」 */
const lastOtherSrc = ref<'' | '1d' | 'qr' | 'native'>('')
/**
 * 画面亮度范围（如 `3~241`）。★ 这是**区分「取不到画面」和「画面正常但没码」的唯一手段**：
 *   · `0~0` / 恒定值 ⇒ canvas 是纯色（多半是 drawImage 从 video 取帧失败）→ 不是编码问题
 *   · 范围很宽     ⇒ 画面真的进来了，那问题就在「码没在解码带里 / 太小 / 内容不对」
 * 只在解码失败时按 ~1s 节流采样，避免每帧 getImageData 拖慢平板。
 */
const frameLuma = ref('')
let lumaAt = 0

/** 按 1s 节流采样当前解码画布的亮度范围 */
function sampleLuma() {
  if (!cvsCtx || !canvas || !canvas.width || !canvas.height) return
  try {
    const d = cvsCtx.getImageData(0, 0, canvas.width, canvas.height).data
    let lo = 255
    let hi = 0
    // 每 16 像素取一个：1280x317 约 2.5 万样本，足够判断且几乎不耗时
    for (let i = 0; i < d.length; i += 64) {
      const v = d[i]
      if (v < lo) lo = v
      if (v > hi) hi = v
    }
    frameLuma.value = `${lo}~${hi}`
  } catch {
    frameLuma.value = ''
  }
}

const engineLabel = computed(() =>
  engine.value === 'native' ? '浏览器原生（BarcodeDetector）' : engine.value === 'zxing' ? 'ZXing（JS 解码）' : ''
)

/**
 * 实时诊断文案（★ 2026-09-25 加）。
 *
 * 目的只有一个：**让「扫了没反应」当场可判读**，不用连电脑看 console。
 * 三档语义（顺序即排查优先级）：
 *   ① 解出过内容但不是付款码 → 解码器没毛病，扫错东西了（多半是二维码）
 *   ② 解出过帧但没命中      → 解码器在工作，继续对准条形码
 *   ③ 一帧都没解出          → 物理条件问题，界面上直接给可操作建议
 */
const diagText = computed(() => {
  if (!scanning.value) return ''
  if (lastOtherLen.value > 0) {
    const tail =
      lastOtherSrc.value === 'qr'
        ? '扫到的是二维码 → 请对准付款码上方的「条形码」'
        : `扫到的内容不是付款码（长度 ${lastOtherLen.value}，非纯数字）→ 请对准付款码上方的「条形码」`
    return `解码器工作正常（已解出 ${decodedFrames.value} 帧内容）· ${tail}`
  }
  if (decodedFrames.value > 0) {
    return `已解出 ${decodedFrames.value} 帧内容 · 尚未命中付款码`
  }
  // 一帧没解出：把「画面是否真的进来了」一并显示 —— 黑白纯色说明取帧失败，不是编码问题
  let luma = ''
  if (frameLuma.value) {
    const [lo, hi] = frameLuma.value.split('~').map(Number)
    luma = hi - lo < 8 ? `｜⚠ 画面亮度 ${frameLuma.value}（纯色，取帧可能失败）` : `｜画面亮度 ${frameLuma.value}`
  }
  return `一帧都没解出${luma} → ① 对准付款码的「条形码」而非二维码 ② 凑近到 15~20cm ③ 顾客手机屏幕调到最亮`
})

/**
 * 送进解码器的画面宽度上限。
 *
 * ★ 取 1280 是有实测依据的（2026-09-24，用 python-barcode 独立生成的 Code128 实测）：
 *   · 解码下限 ≈ 1.5 px/模块；付款码铺满取景框时约 4~6 px/模块，余量充足
 *   · 耗时实测（本机 Node）：640宽 10.9ms / 960宽 17.1ms / 1280宽 23.5ms
 *     —— 全分辨率也只要 23ms，8fps 的 120ms 预算绰绰有余
 *   · **最关键**：实测发现重采样本身会毁掉临界条码（同一张码在某些缩放比例下
 *     时好时坏，与插值质量无关）。既然全分辨率跑得动，就别降采样，直接消掉这一整类失败。
 *   → 只有当摄像头 > 1280 宽（高分辨率平板）时才降采样，用于兜住 CPU。
 */
const SCAN_MAX_WIDTH = 1280
/** 只把画面中间这条横带送进解码：1D 条码只需一条横带，裁掉上下纯赚（像素少 ~56%） */
const SCAN_BAND = 0.44

/** 付款码只认纯数字（微信 18 位 / 支付宝 16~24 位）——避免扫到商品条码就误提交 */
function isPaymentCode(raw: string): boolean {
  return /^\d{10,24}$/.test((raw || '').trim())
}

/** 摄像头异常说人话（浏览器给的英文名对收银员没意义） */
function cameraErrorText(e: unknown): string {
  const err = e as { name?: string; message?: string }
  switch (err?.name) {
    case 'NotAllowedError':
    case 'SecurityError':
      return '摄像头权限被拒绝。① 若是 http:// 打开请改用 https://；② 若收银台嵌在商家端页面里，需父页 iframe 放行摄像头（allow="camera"）；③ 也可能是浏览器地址栏权限被手动关了'
    case 'NotFoundError':
    case 'DevicesNotFoundError':
      return '没有检测到摄像头设备'
    case 'NotReadableError':
    case 'TrackStartError':
      return '摄像头被其他程序占用（微信、视频会议等），请关掉后再试'
    case 'OverconstrainedError':
      return '该摄像头不支持所需参数，请换一个摄像头'
    case 'NotSupportedError':
      return '当前环境不支持摄像头采集'
    default:
      return err?.message || '未知错误'
  }
}

/** 枚举摄像头；首次授权前 label 为空，故授权成功后再刷一次 */
async function refreshCameras() {
  const list = await navigator.mediaDevices.enumerateDevices()
  const cams = list.filter(d => d.kind === 'videoinput')
  cameras.value = cams.map((d, i) => ({ id: d.deviceId, label: d.label || `摄像头 ${i + 1}` }))
  if (!cams.length) return
  // 已在推流时，下拉框要显示「当前真正在用的那个」——否则画面和设备名对不上
  const active = stream?.getVideoTracks()[0]?.getSettings?.().deviceId
  if (active && cams.some(d => d.deviceId === active)) {
    cameraId.value = active
    return
  }
  if (!cameras.value.some(c => c.id === cameraId.value)) {
    // 优先「后置/环境摄像头」：平板对着顾客手机扫，用后摄
    const rear = cams.find(d => /back|rear|environment|后置|后摄|环境/i.test(d.label))
    cameraId.value = (rear || cams[cams.length - 1]).deviceId
  }
}

/** 开流（切换摄像头也走这里） */
async function openStream() {
  closeStream()
  const video: MediaTrackConstraints = { width: { ideal: 1280 }, height: { ideal: 720 } }
  if (cameraId.value) video.deviceId = { exact: cameraId.value }
  else video.facingMode = { ideal: 'environment' }
  stream = await navigator.mediaDevices.getUserMedia({ video, audio: false })
  const track = stream.getVideoTracks()[0]
  const caps = (track.getCapabilities?.() ?? {}) as unknown as { torch?: boolean }
  torchSupported.value = !!caps.torch
}

function closeStream() {
  stream?.getTracks().forEach(t => t.stop())
  stream = null
  torchOn.value = false
  torchSupported.value = false
}

/** 选引擎 + 建解码器（ZXing 走动态 import，只有真需要它的浏览器才付这份体积） */
async function prepareDecoder() {
  const Detector = (
    window as unknown as { BarcodeDetector?: new (o?: unknown) => { detect: (s: unknown) => Promise<{ rawValue: string }[]> } }
  ).BarcodeDetector
  if (Detector) {
    nativeDetector = new Detector({ formats: ['code_128', 'code_39', 'qr_code'] })
    engine.value = 'native'
    return
  }
  // iPad/Safari/Firefox 没有 BarcodeDetector → 上 ZXing 纯 JS 解码（动态 import，不进主包）
  const { createZxDecoder } = await import('@/utils/zxing-1d')
  decoder = createZxDecoder()
  canvas = document.createElement('canvas')
  cvsCtx = canvas.getContext('2d', { willReadFrequently: true })
  if (cvsCtx) {
    // 只有摄像头 > SCAN_MAX_WIDTH 真要降采样时才起作用。本机实测 BILINEAR 与 LANCZOS
    // 结果一致（都不是关键项），设 high 只是让降采样场景更稳妥。
    cvsCtx.imageSmoothingEnabled = true
    cvsCtx.imageSmoothingQuality = 'high'
  }
  engine.value = 'zxing'
}

/**
 * 单帧识别结果。
 *
 * ⚠️ 必须把「没解出」和「解出了但不是付款码」**分开返回** —— 二者的处置完全不同：
 * 前者要调物理条件（距离/对焦/反光），后者要换扫的位置（多半扫到了二维码）。
 * （旧版一律返回 `null`，是「扫了没反应却查不出原因」的根因。）
 */
type FrameHit =
  | { kind: 'none' } // 这一帧什么都没解出来
  | { kind: 'pay'; code: string } // 命中付款码
  | { kind: 'other'; len: number; src: '1d' | 'qr' | 'native' } // 解出内容了，但不是「10~24 位纯数字」

/** 单帧识别：返回上面三种之一（任何情况都不抛异常，不打断循环） */
async function detectOnce(): Promise<FrameHit> {
  const video = videoEl.value
  if (!video || video.readyState < 2) return { kind: 'none' }

  if (nativeDetector) {
    const list = await nativeDetector.detect(video)
    if (!list.length) return { kind: 'none' }
    // 原生引擎会一次性返回画面里**所有**码 → 遍历找出付款码即可，没有「谁先谁后」问题
    let other: { len: number; src: '1d' | 'qr' | 'native' } | null = null
    for (const b of list) {
      const v = String(b.rawValue || '').trim()
      if (isPaymentCode(v)) return { kind: 'pay', code: v }
      const fmt = String((b as { format?: string }).format || '').toLowerCase()
      other = { len: v.length, src: fmt.includes('qr') ? 'qr' : 'native' }
    }
    return other ? { kind: 'other', len: other.len, src: other.src } : { kind: 'none' }
  }

  if (!decoder || !canvas || !cvsCtx) return { kind: 'none' }
  const vw = video.videoWidth
  const vh = video.videoHeight
  if (!vw || !vh) return { kind: 'none' }
  // 降分辨率 + 只取中间横带：这两件事直接决定每帧 CPU 开销（平板尤其吃紧）
  const scale = Math.min(1, SCAN_MAX_WIDTH / vw)
  const dw = Math.max(1, Math.round(vw * scale))
  const bandH = vh * SCAN_BAND
  const dh = Math.max(1, Math.round(bandH * scale))
  if (canvas.width !== dw || canvas.height !== dh) {
    canvas.width = dw
    canvas.height = dh
  }
  cvsCtx.drawImage(video, 0, (vh - bandH) / 2, vw, bandH, 0, 0, dw, dh)
  // 解码器只认一维码（不加 QR_CODE 的三条理由见 utils/zxing-1d.ts 头部 ★★）
  const text = (decoder.decode(canvas) || '').trim()
  if (!text) {
    // 没解出时顺手采一次画面亮度（1s 节流）—— 用来区分「黑屏取帧失败」与「画面正常只是没码」
    const now = Date.now()
    if (now - lumaAt > 1000) {
      lumaAt = now
      sampleLuma()
    }
    return { kind: 'none' }
  }
  return isPaymentCode(text) ? { kind: 'pay', code: text } : { kind: 'other', len: text.length, src: '1d' }
}

function tick() {
  if (!scanning.value) return
  if (!props.busy && !decoding) {
    decoding = true
    void (async () => {
      try {
        const hit = await detectOnce()
        if (hit.kind === 'pay') {
          accept(hit.code)
          return
        }
        // 「解出了但不是付款码」也要计数 —— 这是区分物理问题 / 内容问题的唯一线索
        if (hit.kind === 'other') {
          decodedFrames.value++
          lastOtherLen.value = hit.len
          lastOtherSrc.value = hit.src
        }
      } catch {
        /* 单帧异常忽略，继续 */
      } finally {
        decoding = false
      }
      if (scanning.value) loopTimer = window.setTimeout(tick, DECODE_INTERVAL)
    })()
    return
  }
  loopTimer = window.setTimeout(tick, DECODE_INTERVAL)
}

function accept(text: string) {
  if (text === lastAccepted) return
  lastAccepted = text
  resumeAfterBusy = true // 失败/USERPAYING 时自动恢复摄像头
  code.value = text
  stopScan()
  submit()
}

async function startScan() {
  scanMsg.value = ''
  decodedFrames.value = 0
  lastOtherLen.value = 0
  lastOtherSrc.value = ''
  frameLuma.value = ''
  lumaAt = 0
  // 前置检查 1：安全上下文。http://<局域网IP> 下 navigator.mediaDevices 直接是 undefined
  if (!window.isSecureContext) {
    scanMsg.value =
      '当前地址是 http 明文访问，浏览器不允许调用摄像头。请改用 https:// 地址打开收银台（或联系管理员）'
    return
  }
  // 前置检查 2：API 存在性
  if (!navigator.mediaDevices?.getUserMedia) {
    scanMsg.value = '当前浏览器不提供摄像头接口（navigator.mediaDevices 不可用），请用扫码枪或手输'
    return
  }
  try {
    // ★ iOS/Safari 要求 getUserMedia 紧跟用户手势：openStream() 之前**不能有任何 await**，
    //   否则「瞬时激活」可能已过期 → 直接报 NotAllowedError（iPad 上必踩）。
    //   所以先用 facingMode:'environment' 开流（平板默认就是后摄），
    //   等画面出来后再枚举设备（此时 label 才有内容）供收银员切换镜头。
    await openStream()
    scanning.value = true
    await nextTick()
    if (!videoEl.value) return
    videoEl.value.srcObject = stream
    await videoEl.value.play()
    await refreshCameras() // 授权后 label 才有内容，刷新给用户看真实设备名
    await prepareDecoder()
    tick()
  } catch (e) {
    stopScan()
    scanMsg.value = `无法访问摄像头：${cameraErrorText(e)}`
  }
}

/** 切换摄像头（平板多摄：前置/后置） */
async function switchCamera() {
  if (!scanning.value) return
  try {
    await openStream()
    if (videoEl.value) {
      videoEl.value.srcObject = stream
      await videoEl.value.play()
    }
  } catch (e) {
    scanMsg.value = `切换摄像头失败：${cameraErrorText(e)}`
  }
}

/** 补光灯（暗环境下扫手机屏幕很有用；设备不支持则不显示按钮） */
async function toggleTorch() {
  const track = stream?.getVideoTracks()[0]
  if (!track) return
  const next = !torchOn.value
  try {
    await track.applyConstraints({ advanced: [{ torch: next }] } as unknown as MediaTrackConstraints)
    torchOn.value = next
  } catch {
    torchSupported.value = false
    scanMsg.value = '该摄像头不支持补光灯'
  }
}

function stopScan() {
  scanning.value = false
  if (loopTimer) {
    clearTimeout(loopTimer)
    loopTimer = undefined
  }
  decoding = false
  closeStream()
  nativeDetector = null
  decoder = null
  engine.value = ''
}

/** 打开时：清空上一个码 + 焦点给输入框（扫码枪需要焦点）；关闭时停摄像头并释放 */
watch(
  () => props.visible,
  async v => {
    if (v) {
      code.value = ''
      scanMsg.value = ''
      lastAccepted = ''
      resumeAfterBusy = false
      await nextTick()
      focusInput()
    } else {
      stopScan()
    }
  }
)

/**
 * 一轮提交结束（busy true→false）：
 *  · 成功（ok）→ 父组件 1s 后自动关层，不复扫
 *  · USERPAYING（warn）/ 失败（error）→ 说明还要再扫一次，自动把摄像头恢复起来，省得收银员再点一下
 *  · 同时把焦点还给输入框，保证扫码枪连续扫不用手点
 */
watch(
  () => props.busy,
  async (now, before) => {
    if (before && !now && props.visible) {
      code.value = ''
      await nextTick()
      focusInput()
      if (resumeAfterBusy && props.messageKind !== 'ok') {
        resumeAfterBusy = false
        lastAccepted = '' // 允许重扫同一张码（微信对同单号幂等，重扫安全）
        await startScan()
      }
    }
  }
)

onBeforeUnmount(stopScan)
</script>
