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
  '/shop/account': defineAsyncComponent(() => import('@/views/AccountView.vue'))
  // 其余 15 项尚未落地 → 统一走 PageStub（批次 C 起逐页替换）
}

const router = createRouter({
  history: createWebHistory(),
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
