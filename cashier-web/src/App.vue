<template>
  <!-- W-07 整体骨架（2:581）：头部 48px + 左侧深色侧栏 230px + 白色内容区 -->
  <div v-if="isLogin" class="h-screen flex flex-col overflow-hidden" style="background: #fff">
    <!-- 头部 2:582 -->
    <header class="flex items-center gap-3.5 h-12 shrink-0 px-4 text-white border-b" style="background: var(--dark); border-color: #2a3120">
      <div class="w-7 h-7 rounded-lg flex items-center justify-center text-base" style="background: var(--p)">♪</div>
      <div>
        <div class="text-[15px] font-bold tracking-wide leading-tight">乐章点餐 · 收银台</div>
      </div>
      <div class="ml-auto flex items-center gap-3 text-xs" style="color: #8f94a0">
        <span>商家 {{ merchantName }}</span>
        <button class="px-2 py-1 rounded border border-white/20 hover:bg-white/10" @click="logout">退出</button>
      </div>
    </header>

    <div class="flex flex-1 min-h-0">
      <!-- 侧栏 2:583：竖排导航（三视图 = Odoo Screen 状态机） -->
      <aside class="w-[230px] shrink-0 flex flex-col pt-3" style="background: var(--dark)">
        <router-link
          v-for="n in navs"
          :key="n.to"
          :to="n.to"
          class="mx-3 mb-1 px-4 py-3 rounded-lg text-[13px] transition-colors"
          :class="n.active ? 'text-white font-medium' : ''"
          :style="n.active ? 'background: var(--dark2); color: #fff' : 'color: #aab2a0'"
        >
          {{ n.label }}
        </router-link>
        <div class="mt-auto p-4 text-[11px]" style="color: #5a614e">
          Vue3 + Pinia · floor / order / ticket
        </div>
      </aside>

      <!-- 内容区 2:584 -->
      <main class="flex-1 flex flex-col min-w-0 bg-white">
        <router-view />
      </main>
    </div>
  </div>

  <!-- 登录页：独立无骨架布局 -->
  <router-view v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isLogin = computed(() => route.path !== '/login')

/** 登录商家名：从 JWT payload 或兜底显示（本期仅展示用） */
const merchantName = computed(() => {
  const token = localStorage.getItem('token')
  if (!token) return ''
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.username || '商家'
  } catch {
    return '商家'
  }
})

function logout() {
  localStorage.removeItem('token')
  void router.replace('/login')
}

const navs = computed(() => [
  { to: '/floor', label: '桌台图', active: route.path.startsWith('/floor') },
  { to: '/order', label: '点单管理', active: route.path.startsWith('/order') },
  { to: '/ticket', label: '未结订单', active: route.path.startsWith('/ticket') }
])
</script>
