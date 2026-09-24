import { createRouter, createWebHistory } from 'vue-router'
import { defineAsyncComponent, type Component } from 'vue'
import { DEFAULT_PATH, MENU_LEAVES } from '@/config/menu'
import { http } from '@/api/http'
import { usePosStore } from '@/stores/pos'
import PageStub from '@/components/PageStub.vue'

/**
 * 已落地页面对照表（批次 A 现状）
 * 未列出的二级项一律走 PageStub 占位，随 B~H 批次逐页替换。
 */
const IMPLEMENTED: Record<string, Component> = {
  '/table/floor': defineAsyncComponent(() => import('@/views/FloorView.vue')),
  '/order/cashier': defineAsyncComponent(() => import('@/views/OrderView.vue')),
  '/order/goods-status': defineAsyncComponent(() => import('@/views/GoodsStatusView.vue')),
  '/orders/today': defineAsyncComponent(() => import('@/views/OrderTodayView.vue')),
  '/orders/handle': defineAsyncComponent(() => import('@/views/OrderHandleView.vue')),
  '/orders/production': defineAsyncComponent(() => import('@/views/ProductionBoardView.vue')),
  '/wine/list': defineAsyncComponent(() => import('@/views/WineStorageView.vue')),
  '/wine/create': defineAsyncComponent(() => import('@/views/WineCreateView.vue')),
  '/print/printers': defineAsyncComponent(() => import('@/views/PrinterListView.vue')),
  '/pickup/verify': defineAsyncComponent(() => import('@/views/PickupVerifyView.vue')),
  '/pickup/records': defineAsyncComponent(() => import('@/views/PickupRecordView.vue')),
  '/print/routing': defineAsyncComponent(() => import('@/views/PrintRoutingView.vue')),
  '/shop/settings': defineAsyncComponent(() => import('@/views/ShopSettingView.vue')),
  '/print/queue': defineAsyncComponent(() => import('@/views/PrintQueueView.vue')),
  '/print/templates': defineAsyncComponent(() => import('@/views/PrintTemplateView.vue')),
  '/wine/expiring': defineAsyncComponent(() => import('@/views/WineExpiringView.vue')),
  '/orders/aftersales': defineAsyncComponent(() => import('@/views/ChangeRecordView.vue')),
  '/shop/account': defineAsyncComponent(() => import('@/views/AccountView.vue')),
  // W-27 会员现金充值（2026-09-24 新增）
  '/member/recharge': defineAsyncComponent(() => import('@/views/MemberRechargeView.vue'))
  // 全部 19 项菜单叶子均已落地（W-13 改单/退单：弹窗挂订单详情页，本表落记录页）→ 不再有 PageStub 入口
  // 注：W-08 台位类型已按 V7 裁决从菜单整项移除，不再出现在本表
}

const router = createRouter({
  /*
   * base 必须显式传 import.meta.env.BASE_URL（= vite 的 base）：
   * 不传时 vue-router 只在页面有 <base> 标签时才认子路径，否则**兜底成 '/'**，
   * 于是它会把 `/cashier/order/goods-status` 整体当成路由路径去匹配 → 匹配不到
   * → 落到 catch-all 重定向回默认页（2026-09-19 线上实测：深链被弹回台位图）。
   * 开发环境 BASE_URL='/',生产 '/cashier/'，两边都正确。
   */
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: DEFAULT_PATH },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },

    // 菜单单一事实源生成（5 直属功能 + 4 分组 = 18 页），保证侧栏、面包屑、路由三者不漂移
    // 直属一级叶子的 groupLabel = 自身 label（见 config/menu.ts）
    ...MENU_LEAVES.map(leaf => ({
      path: leaf.path,
      name: leaf.name,
      component: IMPLEMENTED[leaf.path] ?? PageStub,
      meta: { board: leaf.board, group: leaf.groupLabel }
    })),

    // 非菜单页：订单详情（W-12），由 W-10/W-11 列表的「详情」进入，不出现在侧栏
    {
      path: '/orders/detail',
      name: 'order-detail',
      component: () => import('@/views/OrderDetailView.vue'),
      meta: { board: 'W-12', group: '订单管理' }
    },

    { path: '/:pathMatch(.*)*', redirect: DEFAULT_PATH }
  ]
})

// 登录守卫：无 token 一律进登录页
/**
 * token 有效性校验结果（进程内缓存）：null = 未验证。
 * 登录成功后由 LoginView 调 resetTokenCache() 重置（新 token 不必再走一次校验请求）。
 */
let tokenVerified: boolean | null = null

/** 登录成功后重置校验缓存 */
export function resetTokenCache() {
  tokenVerified = null
}

/**
 * 轻量校验 token：调统计接口，success 即视为有效。
 * 实测（2026-09-24）：假 token 时后端把失效吞成 {"success":false,"code":0,"message":"系统异常"}
 * —— 无法与真业务错区分，故这里**任何失败都视为 token 无效**（清 token 跳登录）。
 * 已知取舍：后端宕机时也会被弹回登录页（此时页面反正无数据可用）。
 */
async function verifyToken(): Promise<boolean> {
  try {
    await http.post('/rest/merchant/order/statistic', { pageNo: 1, pageSize: 1 })
    return true
  } catch {
    return false
  }
}

router.beforeEach(async to => {
  const token = localStorage.getItem('token')

  // 登录页：有 token 但**已失效** ⇒ 清掉并停留，让用户能重新登录
  //（原实现会把用户从登录页弹回看板空页 —— 业务方 09-24 反馈的 bug）
  if (to.path === '/login') {
    if (token && tokenVerified === null) {
      tokenVerified = await verifyToken()
      if (!tokenVerified) localStorage.removeItem('token')
    }
    if (token && tokenVerified) {
      return { path: DEFAULT_PATH }
    }
    return true
  }

  // 其它页面：无 token ⇒ 登录页
  if (!token) {
    return { path: '/login' }
  }
  // 2026-09-24 修复：原实现只查 token **存在与否**，过期/无效 token 会放行进页面
  // ⇒ 接口全部失败 ⇒ 空数据卡死、不跳登录（业务方 09-24 反馈的 bug）。现补一次有效性校验。
  if (tokenVerified === null) {
    tokenVerified = await verifyToken()
    if (!tokenVerified) {
      localStorage.removeItem('token')
      return { path: '/login' }
    }
  }
  return true
})

// 反向同步：桌台图 / 代客点单 / 今日订单三页共用 pos store 的视图态，单一事实源 = 路由
router.afterEach(to => {
  usePosStore().syncViewFromPath(to.path)
})

export default router
