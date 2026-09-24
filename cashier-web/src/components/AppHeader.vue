<template>
  <!-- 头部 48px（对位 Ardot C-头部 2:54） -->
  <header class="h-12 shrink-0 flex items-stretch text-white" style="background: var(--dark)">
    <!-- 品牌块 230×48（2:55），宽度与侧栏一致 -->
    <div
      class="w-[230px] shrink-0 flex items-center gap-2 px-4 border-r"
      style="background: var(--dark2); border-color: #2a3120"
    >
      <div class="w-6 h-6 rounded flex items-center justify-center text-[13px] text-white" style="background: var(--p)">♪</div>
      <span class="text-[15px] font-medium tracking-wide whitespace-nowrap">湖里Lakes · 收银端</span>
    </div>

    <!-- 折叠钮 2:57 -->
    <button
      class="w-11 flex flex-col items-center justify-center gap-[4px] hover:bg-white/5"
      :title="collapsed ? '展开侧栏' : '收起侧栏'"
      @click="emit('toggle')"
    >
      <i v-for="i in 3" :key="i" class="block w-4 h-[1.5px]" style="background: #aab2a0" />
    </button>

    <div class="flex-1 min-w-0" />

    <!-- 全屏（09-24 拍板：自 M-04 看板头部移入顶栏；19:58 二次拍板改为常驻，所有页面显示；
         对整个页面走浏览器 Fullscreen API） -->
    <button
      class="self-center mr-5 text-[12px] shrink-0"
      style="color: #aab2a0"
      title="全屏 / 退出全屏"
      @click="toggleFullscreen"
    >⛶ 全屏</button>

    <!-- 代客点单快捷钮 88×32（2:59） -->
    <button
      class="self-center h-8 w-[88px] rounded text-[12px] font-medium text-white shrink-0"
      style="background: var(--p)"
      @click="goCashier"
    >代客点单</button>

    <span class="self-center ml-4 text-[12px] shrink-0" style="color: #aab2a0">开关设置 ▾</span>
    <span class="self-center ml-4 text-[12px] shrink-0 truncate max-w-[200px]" style="color: #aab2a0">{{ auth.shopName }}</span>

    <!-- 头像 32×32（2:63）：点击直达账号页（画板头部无下拉；退出登录在 W-24 账号页） -->
    <router-link to="/shop/account" class="self-center mx-4 shrink-0" :title="`${auth.accountName} · 账号与密码`">
      <img v-if="auth.merchant?.headImg" :src="ossUrl(auth.merchant.headImg)" class="w-8 h-8 rounded-full object-cover" alt="" />
      <span
        v-else
        class="w-8 h-8 rounded-full flex items-center justify-center text-[12px]"
        style="background: var(--dark2); color: #aab2a0"
      >{{ auth.initial }}</span>
    </router-link>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ossUrl } from '@/config/oss'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ (e: 'toggle'): void }>()

const auth = useAuthStore()
const router = useRouter()

/** 画板头部快捷入口：直达代客点单 */
function goCashier() {
  void router.push('/order/cashier')
}

/** 全屏切换（自 M-04 看板头部移入顶栏并常驻，逻辑不变） */
function toggleFullscreen() {
  const el = document.documentElement
  if (document.fullscreenElement) document.exitFullscreen()
  else el.requestFullscreen?.()
}
</script>
