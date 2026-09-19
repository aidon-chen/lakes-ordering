<template>
  <!-- 收银台登录：商家账号 + 密码（POST /rest/merchant/login，密码 Base64） -->
  <div class="h-screen flex items-center justify-center" style="background: var(--dark)">
    <form class="w-[360px] rounded-xl bg-white p-8 shadow-xl" @submit.prevent="login">
      <div class="flex items-center gap-2 mb-6">
        <div class="w-9 h-9 rounded-lg flex items-center justify-center text-lg text-white" style="background: var(--p)">♪</div>
        <div>
          <div class="text-[18px] font-bold leading-tight">乐章点餐 · 收银台</div>
          <div class="text-[12px]" style="color: var(--tc-gray)">请使用商家账号登录</div>
        </div>
      </div>

      <div class="mb-3">
        <label class="text-[12px] font-medium mb-1 block" style="color: var(--tc-sub)">账号</label>
        <input
          v-model.trim="account"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="商家账号"
          autocomplete="username"
        />
      </div>
      <div class="mb-5">
        <label class="text-[12px] font-medium mb-1 block" style="color: var(--tc-sub)">密码</label>
        <input
          v-model="password"
          type="password"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="密码"
          autocomplete="current-password"
        />
      </div>

      <p v-if="error" class="mb-3 text-[12px]" style="color: var(--danger, #f56c6c)">{{ error }}</p>

      <button
        type="submit"
        class="w-full h-10 rounded text-[13px] font-medium text-white disabled:opacity-60"
        style="background: var(--p)"
        :disabled="loading"
      >
        {{ loading ? '登录中…' : '登 录' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login as loginApi } from '@/api/auth'

const router = useRouter()
const account = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function login() {
  if (!account.value || !password.value) {
    error.value = '请输入账号与密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const { token } = await loginApi(account.value, password.value)
    localStorage.setItem('token', token)
    await router.replace('/floor')
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
