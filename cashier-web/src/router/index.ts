import { createRouter, createWebHistory } from 'vue-router'
import { defineAsyncComponent, type Component } from 'vue'
import { DEFAULT_PATH, MENU_LEAVES } from '@/config/menu'
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
  '/shop/account': defineAsyncComponent(() => import('@/views/AccountView.vue'))
  // 其余 3 项尚未落地 → 统一走 PageStub（批次 F/G 起逐页替换）
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

    // 7 一级 / 19 二级：全部由菜单单一事实源生成，保证侧栏、面包屑、路由三者不漂移
    ...MENU_LEAVES.map(leaf => ({
      path: leaf.path,
      name: leaf.name,
      component: IMPLEMENTED[leaf.path] ?? PageStub,
      meta: { board: leaf.board, group: leaf.label }
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
router.beforeEach(to => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: DEFAULT_PATH }
  }
  return true
})

// 反向同步：桌台图 / 代客点单 / 今日订单三页共用 pos store 的视图态，单一事实源 = 路由
router.afterEach(to => {
  usePosStore().syncViewFromPath(to.path)
})

export default router
