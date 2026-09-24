/**
 * ZXing 解码薄封装（2026-09-24 新增，配合收银台「扫码收款」的平板摄像头）
 *
 * 为什么单独一个文件：
 *  1. 让 @zxing/library（体积不小）留在**动态 import** 之后，不进主包 —— 只有
 *     浏览器没有原生 BarcodeDetector（iPad/Safari、Firefox）时才付出这份体积
 *  2. @zxing/library 的根 `index.d.ts` **没有**导出 HTMLCanvasElementLuminanceSource，
 *     它只存在于 `esm/browser/` 下（package.json 无 exports 字段，故深路径可引入）。
 *     把这份脆弱点收敛在这一个文件里。
 *
 * ⚠️ 不要改用 RGBLuminanceSource 自己转帧：它的构造函数在拿到 Uint8ClampedArray
 *    （BYTES_PER_ELEMENT === 1）时会**当成单通道灰度**直接用，而不是 RGBA。
 *    传入 ImageData.data 会被按 4 倍步长误读 → 永远解不出来。
 *
 * ★★ 2026-09-25：**只解一维码，不要加 QR_CODE**（三条实测依据，别改回去）：
 *
 *   ① 付款码支付（被扫）要的是**条形码**里的 18 位 `auth_code`；二维码里那段 URL
 *      后端根本不认（`isPaymentCode` 只收 `\d{10,24}`）。加 QR 对业务零收益。
 *   ② `POSSIBLE_FORMATS` 里只要同时含 `QR_CODE`，@zxing/library 就会把
 *      `QRCodeReader` 插到 1D reader **前面**（`setHints()` 里 1D 在 tryHarder 时被 push 到最后），
 *      于是每帧都要先白跑一次二维码检测才轮到条形码 —— 平板上纯属浪费帧预算。
 *   ③ 更要命的是 JS 版 `QRCodeReader` **基本不可用**（2026-09-25 实测）：
 *      · 直接 `new QRCodeReader().decodeWithState(bitmap)` → **抛 TypeError**
 *      · 经 `MultiFormatReader(POSSIBLE_FORMATS=[QR_CODE])` → 普通取景下恒 **NotFoundException**
 *        （只有再加 `PURE_BARCODE`、把整幅当"纯码"才解得出来）
 *      ⇒ 加上它既解不出二维码，又要先花一次检测开销。
 *
 *   实测佐证（python-barcode 独立造码 + 桩 canvas 跑真实源码，10 种劣化用例）：
 *   1D 配置在「虚焦 r=1.8 / 低对比 ×0.35 / 倾斜 6° / 条码只占画面 1/3 宽」下 **10/10 全解出**。
 */
import { BarcodeFormat, BinaryBitmap, DecodeHintType, HybridBinarizer, MultiFormatReader } from '@zxing/library'
// 深路径引入：根 index.d.ts 未导出该类，但 esm/browser 下有对应 .d.ts 且运行时存在
import { HTMLCanvasElementLuminanceSource } from '@zxing/library/esm/browser/HTMLCanvasElementLuminanceSource'

export type ZxDecoder = {
  /** 解码一帧画面；扫不到返回 null（不算异常） */
  decode(canvas: HTMLCanvasElement): string | null
}

export type ZxDecoderOptions = {
  /**
   * 是否允许 ZXing 逐次交替「正常/反色」两套亮度。
   *
   * ⚠️ 实测要点（2026-09-24，用 python-barcode 独立生成的 Code128 实测）：
   *    开启后 ZXing 内部 `FRAME_INDEX` 每次调用翻转，**同一张黑条白底的码只有一半调用能解出**
   *    （表现为 miss/PASS/miss/PASS 交替）。付款码恒为黑条白底，故生产上默认**关掉**，
   *    避免白扔一半机会；仅当怀疑遇到反色码（深色主题把码反显）时才开。
   */
  autoInvert?: boolean
}

/** 建一个可连续复用的解码器（hints 只设一次，逐帧复用） */
export function createZxDecoder(opts: ZxDecoderOptions = {}): ZxDecoder {
  const autoInvert = opts.autoInvert ?? false
  const reader = new MultiFormatReader()
  const hints = new Map<number, unknown>()
  hints.set(DecodeHintType.POSSIBLE_FORMATS, [
    BarcodeFormat.CODE_128, // 微信/支付宝付款码就是它
    BarcodeFormat.CODE_39, // 少数银行/门店码
    BarcodeFormat.ITF // 兼容老式码
    // ⚠️ 不要在这里加 QR_CODE —— 理由见文件头 ★★
  ])
  // TRY_HARDER：手机屏幕反光、轻微倾斜时更耐操（代价是慢一点，故调用方要节流）
  hints.set(DecodeHintType.TRY_HARDER, true)
  reader.setHints(hints as Map<number, unknown> as never)

  return {
    decode(canvas: HTMLCanvasElement): string | null {
      // autoInvert 见 ZxDecoderOptions 注释：默认 false，否则一半的调用会被浪费在反色分支
      const source = new HTMLCanvasElementLuminanceSource(canvas, autoInvert)
      const bitmap = new BinaryBitmap(new HybridBinarizer(source))
      try {
        return reader.decodeWithState(bitmap)?.getText()?.trim() ?? null
      } catch {
        // NotFoundException / ChecksumException / FormatException 都只是「这一帧没扫到」
        return null
      } finally {
        reader.reset()
      }
    },
  }
}
