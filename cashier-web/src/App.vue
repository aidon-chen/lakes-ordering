<template>
  <!-- 登录页：独立无骨架布局 -->
  <router-view v-if="isLogin" />

  <!-- 应用外壳（对位 Ardot W-02 `2:241`）：头部 48px + 侧栏 230px + 内容区（面包屑 40px + 页面） -->
  <div v-else class="h-screen flex flex-col overflow-hidden">
    <AppHeader :collapsed="collapsed" @toggle="collapsed = !collapsed" />

    <div class="flex flex-1 min-h-0">
      <AppSidebar v-show="!collapsed" />

      <main class="flex-1 flex flex-col min-w-0" style="background: var(--bg)">
        <AppBreadcrumb />
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import AppBreadcrumb from '@/components/AppBreadcrumb.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const isLogin = computed(() => route.path === '/login')
const collapsed = ref(false)

// Pad 端适配（Pad端适配-实施计划-v1.1 §2.3）：小横屏（≤1200px）默认收起侧栏，
// 点头部「收起/展开侧栏」按钮可手动展开；1280/1366 大横屏不折叠。
// 仅在进入页面时判定一次（旋转后可手动切换，暂不做 resize 联动）。
onMounted(() => {
  if (window.innerWidth <= 1200) collapsed.value = true
})

// 登录身份信息供头部（店名/头像）与 W-24 账号页共用；登录后与刷新时各拉一次
onMounted(() => {
  if (!isLogin.value) void auth.load()
})
watch(isLogin, login => {
  if (!login) void auth.load()
})
</script>
