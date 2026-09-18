import { createRouter, createWebHistory } from 'vue-router'
import { usePosStore } from '@/stores/pos'

// 三视图路由：floor（桌台总览）/ order（点单管理）/ ticket（未结订单）
// 对应 Odoo floor_screen / product_screen / ticket_screen 的 Screen 状态机
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/floor' },
    { path: '/floor', name: 'floor', component: () => import('@/views/FloorView.vue') },
    { path: '/order', name: 'order', component: () => import('@/views/OrderView.vue') },
    { path: '/ticket', name: 'ticket', component: () => import('@/views/TicketView.vue') }
  ]
})

// 反向同步：侧栏/浏览器手动导航时保持 posStore.view 与路由一致（单一事实源 = 路由）
router.afterEach(to => {
  const view = to.path.replace(/^\//, '') as 'floor' | 'order' | 'ticket'
  const pos = usePosStore()
  if (pos.view !== view) pos.view = view
})

export default router
