<template>
  <!-- W-01 商家登录（Ardot `2:218`）：品牌字「湖里Lakes · 收银端」+ 登录卡 380×410
       三种模式复用同一卡片：账号密码 / 手机验证登录 / 忘记密码（两步） -->
  <div class="h-screen flex flex-col items-center justify-center" style="background: var(--dark)">
    <div class="text-[20px] font-medium text-white mb-7 tracking-wide">湖里Lakes · 收银端</div>

    <div class="rounded-xl bg-white p-8 shadow-xl" style="width: min(380px, calc(100vw - 32px))">
      <div class="text-[20px] font-medium mb-6" style="color: var(--txt)">{{ title }}</div>

      <!-- 账号密码登录 -->
      <template v-if="mode === 'password'">
        <input
          v-model.trim="username"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="商家账号"
          autocomplete="username"
        />
        <input
          v-model="password"
          type="password"
          class="mt-3 w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="密码"
          autocomplete="current-password"
        />
      </template>

      <!-- 手机验证码登录 -->
      <template v-else-if="mode === 'mobile'">
        <input
          v-model.trim="mobile"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="手机号"
          autocomplete="tel"
        />
        <div class="mt-3 flex gap-2">
          <input
            v-model.trim="mobileCode"
            class="flex-1 h-10 px-3 rounded border text-[13px] outline-none"
            style="border-color: var(--tc-line)"
            placeholder="验证码"
            autocomplete="one-time-code"
          />
          <button
            class="w-[110px] h-10 rounded border text-[12px] disabled:opacity-60"
            style="border-color: var(--tc-line); color: var(--p)"
            :disabled="countdown > 0 || sending"
            @click="getCode('login')"
          >{{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}</button>
        </div>
      </template>

      <!-- 忘记密码：第一步校验手机号 -->
      <template v-else-if="mode === 'forgot1'">
        <p class="mb-3 text-[12px]" style="color: var(--tc-sub)">验证手机号后设置新密码</p>
        <input
          v-model.trim="mobile"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="手机号"
          autocomplete="tel"
        />
        <div class="mt-3 flex gap-2">
          <input
            v-model.trim="mobileCode"
            class="flex-1 h-10 px-3 rounded border text-[13px] outline-none"
            style="border-color: var(--tc-line)"
            placeholder="验证码"
            autocomplete="one-time-code"
          />
          <button
            class="w-[110px] h-10 rounded border text-[12px] disabled:opacity-60"
            style="border-color: var(--tc-line); color: var(--p)"
            :disabled="countdown > 0 || sending"
            @click="getCode('findpwd')"
          >{{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}</button>
        </div>
      </template>

      <!-- 忘记密码：第二步设置新密码 -->
      <template v-else>
        <p class="mb-3 text-[12px]" style="color: var(--tc-sub)">手机号 {{ mobile }}</p>
        <input
          v-model="newPassword"
          type="password"
          class="w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="新密码（≥6 位）"
          autocomplete="new-password"
        />
        <input
          v-model="newPassword2"
          type="password"
          class="mt-3 w-full h-10 px-3 rounded border text-[13px] outline-none"
          style="border-color: var(--tc-line)"
          placeholder="确认新密码"
          autocomplete="new-password"
        />
      </template>

      <p v-if="error" class="mt-3 text-[12px]" style="color: var(--danger, #f56c6c)">{{ error }}</p>
      <p v-if="tip" class="mt-3 text-[12px]" style="color: var(--p)">{{ tip }}</p>

      <button
        class="mt-5 w-full h-10 rounded text-[13px] font-medium text-white disabled:opacity-60"
        style="background: var(--p)"
        :disabled="loading"
        @click="submit"
      >{{ loading ? '处理中…' : submitLabel }}</button>

      <!-- 分隔线 + 快捷行（对位 2:230 / 2:231） -->
      <template v-if="mode === 'password'">
        <div class="my-5 h-px" style="background: var(--tc-line)" />
        <button class="w-full text-center text-[12px]" style="color: var(--p)" @click="switchTo('mobile')">
          手机验证登录
        </button>
        <div class="mt-3 text-right">
          <button class="text-[12px]" style="color: var(--tc-sub)" @click="switchTo('forgot1')">忘记密码</button>
        </div>
      </template>

      <div v-else class="mt-4 text-center">
        <button class="text-[12px]" style="color: var(--tc-sub)" @click="switchTo('password')">← 返回账号密码登录</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  forgetPasswordStep1,
  forgetPasswordStep2,
  login as loginApi,
  loginByMobile,
  sendMobileCode,
  type SmsType
} from '@/api/auth'
import { DEFAULT_PATH } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'

type Mode = 'password' | 'mobile' | 'forgot1' | 'forgot2'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<Mode>('password')
const username = ref('')
const password = ref('')
const mobile = ref('')
const mobileCode = ref('')
const newPassword = ref('')
const newPassword2 = ref('')

const loading = ref(false)
const sending = ref(false)
const error = ref('')
const tip = ref('')
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const title = computed(() => (mode.value === 'password' ? '收银端登录' : '手机验证登录'))
const submitLabel = computed(() => {
  if (mode.value === 'forgot1') return '下一步'
  if (mode.value === 'forgot2') return '重 置'
  return '登 录'
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function switchTo(m: Mode) {
  mode.value = m
  error.value = ''
  tip.value = ''
}

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) clearInterval(timer)
  }, 1000)
}

/** 发送验证码：演示环境 smsSwitch=false 时后端会提示改用临时验证码 123456 */
async function getCode(type: SmsType) {
  error.value = ''
  tip.value = ''
  if (!/^1\d{10}$/.test(mobile.value)) {
    error.value = '请输入正确的手机号'
    return
  }
  sending.value = true
  try {
    await sendMobileCode(mobile.value, type)
    tip.value = '验证码已发送'
    startCountdown()
  } catch (e) {
    error.value = (e as Error).message || '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function submit() {
  error.value = ''
  tip.value = ''
  if (mode.value === 'password') return doPasswordLogin()
  if (mode.value === 'mobile') return doMobileLogin()
  if (mode.value === 'forgot1') return doForgotStep1()
  return doForgotStep2()
}

async function doPasswordLogin() {
  if (!username.value || !password.value) {
    error.value = '请输入账号与密码'
    return
  }
  loading.value = true
  try {
    const { token } = await loginApi(username.value, password.value)
    await afterLogin(token)
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function doMobileLogin() {
  if (!mobile.value || !mobileCode.value) {
    error.value = '请输入手机号与验证码'
    return
  }
  loading.value = true
  try {
    const { token } = await loginByMobile(mobile.value, mobileCode.value)
    await afterLogin(token)
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function afterLogin(token: string) {
  localStorage.setItem('token', token)
  await auth.load(true)
  void router.replace(DEFAULT_PATH)
}

async function doForgotStep1() {
  if (!mobile.value || !mobileCode.value) {
    error.value = '请输入手机号与验证码'
    return
  }
  loading.value = true
  try {
    await forgetPasswordStep1(mobile.value, mobileCode.value)
    switchTo('forgot2')
    tip.value = '验证通过，请设置新密码'
  } catch (e) {
    error.value = (e as Error).message || '校验失败'
  } finally {
    loading.value = false
  }
}

async function doForgotStep2() {
  if (newPassword.value.length < 6) {
    error.value = '新密码长度最少 6 位'
    return
  }
  if (newPassword.value !== newPassword2.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await forgetPasswordStep2(mobile.value, newPassword.value)
    tip.value = '密码已重置，请使用新密码登录'
    password.value = ''
    username.value = mobile.value
    setTimeout(() => switchTo('password'), 900)
  } catch (e) {
    error.value = (e as Error).message || '重置失败'
  } finally {
    loading.value = false
  }
}
</script>
