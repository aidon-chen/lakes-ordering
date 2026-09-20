<template>
  <!--
    W-12 订单详情（Ardot 2:846，内容区 2:849）
    两列：左 700（订单头 60 / 明细卡 / 备注卡）｜右 474（台位卡 130 / 金额支付卡 150 / 时间线卡 / 操作条 46）
    ⚠️ 两处与画板的有意差异（均因工程无数据源，绝不用假数据凑）：
      ① 明细表去掉「出品区 / 出品状态」两列 —— 工程出品状态是**订单级**
         （tb_order.production_status，批次D 新增），无行级字段；W-14 看板同为订单级，两边一致
      ② 台位卡去掉「低消」行 —— 需求书 V7 变更一已取消台位低消设置与达标监控（见批次 C 裁决）
    ⚠️ 状态时间线：工程无 tb_order_status_log，本页只用 tb_order 里**真实存在的时间戳**
       （create_time / payment_success_time / order_completion_time）与当前状态拼装；
       带操作人（「接单 · 王莉」）与「开台」节点需日志表，待补
  -->
  <div class="flex-1 overflow-auto p-5 flex gap-4 min-w-0">
    <div v-if="loading" class="text-[12px]" style="color: var(--tc-sub)">加载中…</div>
    <div v-else-if="!order" class="text-[12px]" style="color: var(--tc-sub)">订单不存在或无权查看</div>

    <template v-else>
      <!-- ===================== 左列 ===================== -->
      <div class="flex-1 min-w-0 flex flex-col" style="gap: 12px; max-width: 700px">
        <!-- 订单头 -->
        <div class="rounded-lg flex items-center" style="height: 60px; padding: 0 16px; gap: 12px; background: var(--pl)">
          <span class="text-[26px] font-bold leading-none" style="color: var(--tc-title)">{{ firstNo }}</span>
          <span
            class="rounded text-[12px] leading-[24px] text-center"
            style="min-width: 60px; height: 24px; padding: 0 8px; background: var(--warn-bg, #FCF5ED); color: var(--warn)"
          >{{ orderStatusText(order.status) }}</span>
          <span class="text-[12px]" style="color: var(--tc-sub)">
            下单 {{ fmtTime(order.createTime) }} · {{ order.orderScene === 2 ? '商城' : '堂食' }}
          </span>
        </div>

        <!-- 明细卡 -->
        <div class="rounded" style="border: 1px solid var(--tc-line)">
          <div class="flex items-center text-[13px]" style="height: 40px; padding-left: 16px; background: var(--panel, #F5F7FA); color: var(--tc-title)">
            商品明细
          </div>
          <div class="flex items-center text-[12px]" style="height: 34px; background: var(--bg); color: var(--tc-title)">
            <span class="px-2 truncate" style="width: 240px">品名 / 规格</span>
            <span class="px-2 truncate" style="width: 80px">数量</span>
            <span class="px-2 truncate" style="width: 110px">单价</span>
            <span class="px-2 truncate" style="width: 110px">小计</span>
            <span class="px-2 truncate" style="flex: 1">备注</span>
          </div>

          <div v-if="!details.length" class="text-[12px]" style="padding: 12px 16px; color: var(--tc-sub)">无明细</div>
          <div
            v-for="d in details"
            v-else
            :key="d.id"
            class="flex items-center text-[12px]"
            style="height: 40px; border-top: 1px solid var(--tc-line); color: var(--txt)"
          >
            <span class="px-2 truncate" style="width: 240px">{{ d.goodsName || '—' }}{{ specText(d) ? ' · ' + specText(d) : '' }}</span>
            <span class="px-2 truncate" style="width: 80px">{{ d.number ?? 1 }}</span>
            <span class="px-2 truncate" style="width: 110px">¥{{ money(d.price) }}</span>
            <span class="px-2 truncate" style="width: 110px">¥{{ money(d.subtotal) }}</span>
            <span class="px-2 truncate" style="flex: 1; color: var(--tc-sub)">{{ d.remark || '—' }}</span>
          </div>

          <div class="flex items-center text-[13px] font-medium" style="height: 40px; background: var(--bg); color: var(--tc-title)">
            <span class="px-2" style="flex: 1">商品合计 ¥{{ money(order.goodsTotalPrice ?? order.actualPrice) }}</span>
            <span class="px-2" style="width: 120px">{{ order.goodsTotalQuantity ?? details.length }} 件</span>
          </div>
        </div>

        <!-- 备注卡 -->
        <div class="rounded" style="border: 1px solid var(--tc-line)">
          <div class="flex items-center text-[13px]" style="height: 36px; padding-left: 16px; background: var(--panel, #F5F7FA); color: var(--tc-title)">
            备注
          </div>
          <div class="flex flex-col text-[12px]" style="padding: 12px; gap: 6px">
            <span style="color: var(--txt)">整单：{{ order.remark || '—' }}</span>
            <span style="color: var(--txt)">单品：{{ itemRemarks || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- ===================== 右列 ===================== -->
      <div class="flex flex-col shrink-0" style="width: 474px; gap: 12px">
        <!-- 台位卡 -->
        <div class="rounded-lg" style="padding: 14px; background: var(--bg); display: flex; flex-direction: column; gap: 8px">
          <span class="text-[13px]" style="color: var(--tc-title)">台位信息</span>
          <div class="flex text-[12px]" style="color: var(--txt)">
            <span>{{ tableLine }}</span>
            <span class="ml-auto" style="color: var(--tc-sub)">{{ openTimeLine }}</span>
          </div>
          <div class="flex text-[12px]" style="color: var(--tc-sub)">
            <span>会话</span>
            <span class="ml-auto">{{ order.diningSessionId ? '#' + order.diningSessionId : '—' }}</span>
          </div>
          <div
            class="flex items-center rounded text-[12px]"
            style="height: 30px; padding-left: 10px; background: var(--bg); color: var(--tc-sub)"
          >
            台位码：只读展示（生成/作废在管理端 A-09；列表接口未下发 qrCodeUrl，待补）
          </div>
        </div>

        <!-- 金额与支付卡 -->
        <div class="rounded-lg" style="padding: 14px; background: var(--bg); display: flex; flex-direction: column; gap: 8px">
          <span class="text-[13px]" style="color: var(--tc-title)">金额与支付</span>
          <div class="flex text-[12px]"><span style="color: var(--tc-sub)">商品合计</span><span class="ml-auto" style="color: var(--txt)">¥{{ money(order.goodsTotalPrice) }}</span></div>
          <div class="flex text-[12px]"><span style="color: var(--tc-sub)">实付款</span><span class="ml-auto" style="color: var(--txt)">¥{{ money(order.actualPrice) }}</span></div>
          <div class="flex text-[12px]"><span style="color: var(--tc-sub)">支付方式</span><span class="ml-auto" style="color: var(--txt)">{{ payingText }}</span></div>
          <div class="flex text-[13px] font-medium"><span style="color: var(--tc-sub)">出品状态</span><span class="ml-auto" style="color: var(--p)">{{ productionStatusText(order.productionStatus) }}</span></div>
        </div>

        <!-- 状态变更记录 -->
        <div class="rounded-lg flex-1 min-h-0 overflow-y-auto" style="padding: 14px; background: var(--bg); display: flex; flex-direction: column; gap: 10px">
          <span class="text-[13px]" style="color: var(--tc-title)">状态变更记录</span>
          <span
            v-for="(n, i) in timeline"
            :key="i"
            class="text-[12px] leading-snug"
            :style="{ color: n.color }"
          >{{ n.text }}</span>
          <span class="text-[11px]" style="color: var(--tc-hold)">
            仅列工程真实存在的时间戳；带操作人（如「接单 · 王莉」）与「开台」节点需新增 tb_order_status_log，待补
          </span>
        </div>

        <!-- 操作条 -->
        <div class="rounded-lg flex items-center" style="height: 46px; padding: 0 12px; gap: 10px; background: var(--bg)">
          <button
            v-if="(order.productionStatus ?? 1) < 3"
            class="rounded text-[12px] text-white disabled:opacity-50"
            style="height: 30px; padding: 0 14px; background: var(--p)"
            :disabled="busy"
            @click="advanceProduction"
          >{{ (order.productionStatus ?? 1) === 1 ? '标记出品中' : '标记已出品' }}</button>
          <span class="text-[11px]" style="color: var(--tc-hold)">
            改单 / 退单 / 补打待实现：改单与退单**工程无接口**（无 OrderRefundController、无改单逻辑），
            补打属批次 G；不放点了没反应的按钮
          </span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  fetchOrderById,
  fetchOrderDetails,
  orderStatusText,
  paymentModeText,
  productionStatusText,
  updateProductionStatus,
  type MerchantOrderRow,
  type OrderDetailRow
} from '@/api/order'
import { fetchTableList } from '@/api/table'

const route = useRoute()
const order = ref<MerchantOrderRow | null>(null)
const details = ref<OrderDetailRow[]>([])
/** 台位号 → 类型名（画板「散台 A03」口径） */
const typeByTableNo = ref<Record<string, string>>({})
/** 当前该台位的开台时间（由 diningTable/list 的 openMinutes 反推） */
const openAtByTableNo = ref<Record<string, string>>({})
const loading = ref(true)
const busy = ref(false)

const orderId = Number(route.query.id)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [o, d, t] = await Promise.all([
      fetchOrderById(orderId),
      fetchOrderDetails(orderId).catch(e => {
        console.error('[order-detail] 明细拉取失败', e)
        return { records: [] as OrderDetailRow[] }
      }),
      fetchTableList().catch(() => ({ tables: [] }))
    ])
    order.value = o ?? null
    details.value = d?.records ?? []

    const map: Record<string, string> = {}
    const openMap: Record<string, string> = {}
    for (const tb of t?.tables ?? []) {
      map[tb.tableNo] = tb.typeName
      if (tb.openMinutes != null && tb.status === 2) {
        openMap[tb.tableNo] = hhmm(new Date(Date.now() - tb.openMinutes * 60000).toISOString())
      }
    }
    typeByTableNo.value = map
    openAtByTableNo.value = openMap
  } catch (e) {
    console.error('[order-detail] 加载失败', e)
  } finally {
    loading.value = false
  }
}

const firstNo = computed(() => order.value?.queueNo ?? order.value?.tableNo ?? '—')

const tableLine = computed(() => {
  const o = order.value
  if (!o) return '—'
  const type = o.tableNo ? typeByTableNo.value[o.tableNo] : ''
  const base = type && o.tableNo ? `${type} ${o.tableNo}` : o.tableName || o.tableNo || '—'
  return o.diningNum ? `${base} · ${o.diningNum} 人` : base
})

const openTimeLine = computed(() => {
  const no = order.value?.tableNo
  const t = no ? openAtByTableNo.value[no] : ''
  return t ? `开台 ${t}` : ''
})

const payingText = computed(() => {
  const o = order.value
  if (!o) return '—'
  if (!o.isPayment) return '未支付'
  return paymentModeText(o.paymentMode)
})

const itemRemarks = computed(() =>
  details.value
    .filter(d => (d.remark ?? '').trim())
    .map(d => `${d.goodsName} —— ${d.remark}`)
    .join('；')
)

/**
 * 时间线：只用工程真实存在的时间戳，不臆造节点
 * - create_time 下单
 * - payment_success_time 支付
 * - order_completion_time 完成
 * - 其余以「最近变更」中性表述（update_time 对任何编辑都会刷新，不代表状态变更）
 */
interface Node { t: number; text: string; color: string }
const timeline = computed<Node[]>(() => {
  const o = order.value
  if (!o) return []
  const nodes: Node[] = []
  const push = (v: string | null | undefined, make: (hhmm: string) => string, color = 'var(--tc-sub)') => {
    const t = parseTime(v)
    if (t) nodes.push({ t, text: make(hhmm(v)), color })
  }
  push(o.createTime, h => `下单 ${h}${o.tableNo ? ' · ' + (typeByTableNo.value[o.tableNo] ?? '') + o.tableNo : ''}`, 'var(--txt)')
  push(o.paymentSuccessTime, h => `支付 ${h} ¥${money(o.actualPrice)}（${paymentModeText(o.paymentMode)}）`, 'var(--txt)')
  push(o.createTime !== o.updateTime ? o.updateTime : null, h => `最近变更 ${h}`, 'var(--tc-sub)')
  push(o.orderCompletionTime, h => `已完成 ${h}`, 'var(--p)')
  nodes.push({
    t: Number.MAX_SAFE_INTEGER,
    text: `当前：${orderStatusText(o.status)} · 出品 ${productionStatusText(o.productionStatus)}`,
    color: 'var(--p)'
  })
  return nodes
})

async function advanceProduction() {
  const o = order.value
  if (!o) return
  const cur = o.productionStatus ?? 1
  if (cur >= 3) return
  busy.value = true
  try {
    await updateProductionStatus(o.id, (cur + 1) as 2 | 3)
    await load()
  } catch (e) {
    console.error('[order-detail] 出品推进失败', e)
    alert((e as Error).message || '操作失败')
  } finally {
    busy.value = false
  }
}

/** specList 是 JSON 串；跳过多选编码键（「加料2」这类后缀键） */
function specText(d: OrderDetailRow): string {
  if (!d.specList) return ''
  try {
    const o = JSON.parse(d.specList)
    return Object.keys(o)
      .filter(k => !/\d+$/.test(k))
      .map(k => String(o[k]))
      .filter(Boolean)
      .join('·')
  } catch {
    return ''
  }
}

function parseTime(v: string | null | undefined): number | null {
  if (!v) return null
  const m = String(v).match(/(\d{4})\/(\d{2})\/(\d{2})\s+(\d{2}):(\d{2}):(\d{2})/)
  if (m) return new Date(`${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6]}`).getTime()
  const t = new Date(String(v)).getTime()
  return Number.isNaN(t) ? null : t
}

function fmtTime(v: string | null): string {
  if (!v) return '—'
  const m = String(v).match(/(\d{4})\/(\d{2})\/(\d{2})\s+(\d{2}):(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}` : String(v)
}

function hhmm(v: string | null | undefined): string {
  const m = String(v ?? '').match(/(\d{1,2}):(\d{2})/)
  return m ? `${m[1].padStart(2, '0')}:${m[2]}` : '—'
}

function money(v: number | null | undefined): string {
  return Number(v || 0).toFixed(2)
}
</script>
