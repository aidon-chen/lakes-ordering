<template>
  <!-- W-24 账号与修改密码（Ardot `2:1480`；信息卡 `2:1484` 560×300，改密弹窗卡 `2:1498` 460×300） -->
  <div class="flex-1 overflow-auto p-5">
    <!-- 账号信息卡 -->
    <div class="w-full max-w-[560px] rounded-lg border bg-white" style="border-color: var(--tc-line)">
      <div class="h-11 px-5 flex items-center border-b" style="border-color: var(--tc-line)">
        <span class="text-[14px] font-medium" style="color: var(--tc-title)">账号信息（M-01）</span>
        <button
          class="ml-auto h-7 px-3 rounded border text-[12px] disabled:opacity-60"
          style="border-color: var(--tc-line); color: var(--tc-gray)"
          :disabled="loggingOut"
          @click="doLogout"
        >{{ loggingOut ? '退出中…' : '退出登录' }}</button>
        <button
          class="ml-2 h-7 px-3 rounded text-[12px] text-white disabled:opacity-60"
          style="background: var(--p)"
          :disabled="auth.loading"
          @click="openDialog"
        >修改密码</button>
      </div>

      <div class="px-5 py-4 text-[13px]">
        <div v-for="row in rows" :key="row.label" class="flex items-start gap-4 py-1.5">
          <span class="w-[120px] shrink-0" style="color: var(--tc-sub)">{{ row.label }}</span>
          <span class="flex-1 break-all" :style="row.muted ? 'color: var(--tc-sub)' : 'color: var(--tc-title)'">
            {{ row.value }}
          </span>
        </div>

        <div v-if="auth.loading" class="mt-2 text-[12px]" style="color: var(--tc-sub)">加载中…</div>
        <div v-else-if="!auth.merchant" class="mt-2 text-[12px]" style="color: var(--danger, #f56c6c)">
          未获取到登录信息，请重新登录后再试。
        </div>
      </div>
    </div>

    <!-- 改密弹窗（对位 2:1498） -->
    <Teleport to="body">
      <div
        v-if="dialog"
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(23,28,15,.45)"
        @click.self="closeDialog"
      >
        <div class="rounded-xl bg-white p-6 shadow-xl" style="width: min(460px, calc(100vw - 32px))">
          <div class="text-[16px] font-medium" style="color: var(--tc-title)">修改密码</div>

          <input
            v-model="oldPassword"
            type="password"
            class="mt-5 w-full h-10 px-3 rounded border text-[13px] outline-none"
            style="border-color: var(--tc-line)"
            placeholder="旧密码（仅数字、字母）"
            autocomplete="current-password"
          />
          <input
            v-model="newPassword"
            type="password"
            class="mt-3 w-full h-10 px-3 rounded border text-[13px] outline-none"
            style="border-color: var(--tc-line)"
            placeholder="新密码（≥6 位，提交后强制重新登录）"
            autocomplete="new-password"
          />

          <p v-if="error" class="mt-3 text-[12px]" style="color: var(--danger, #f56c6c)">{{ error }}</p>
          <p v-if="ok" class="mt-3 text-[12px]" style="color: var(--p)">{{ ok }}</p>

          <div class="mt-5 flex gap-3 justify-end">
            <button
              class="h-9 px-5 rounded border text-[13px]"
              style="border-color: var(--tc-line); color: var(--tc-gray)"
              @click="closeDialog"
            >取消</button>
            <button
              class="h-9 px-6 rounded text-[13px] font-medium text-white disabled:opacity-60"
              style="background: var(--p)"
              :disabled="submitting"
              @click="submit"
            >{{ submitting ? '提交中…' : '提 交' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => void auth.load())

const dialog = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const error = ref('')
const ok = ref('')
const submitting = ref(false)
const loggingOut = ref(false)

/** 退出登录（头部下架后收在此页，与 W-24 同屏） */
async function doLogout() {
  loggingOut.value = true
  try {
    await auth.logout()
    void router.replace('/login')
  } finally {
    loggingOut.value = false
  }
}

/** 信息卡 5 行（对位 2:1486 / 2:1489 / 2:1492 / 104:1 / 2:1495） */
const rows = computed(() => [
  { label: '登录账号', value: auth.merchant?.username || auth.merchant?.mobile || '—' },
  { label: '角色', value: auth.roleLabel },
  { label: '所属门店', value: auth.shopName },
  { label: '权限范围（M-01 角色）', value: auth.permissionLabel, muted: true },
  // 后端 tb_merchant 无 last_login_time / last_login_ip 字段，暂无数据源
  { label: '最近登录', value: '—', muted: true }
])

function openDialog() {
  dialog.value = true
  oldPassword.value = ''
  newPassword.value = ''
  error.value = ''
  ok.value = ''
}

function closeDialog() {
  if (submitting.value) return
  dialog.value = false
}

async function submit() {
  error.value = ''
  ok.value = ''
  if (!oldPassword.value) {
    error.value = '请输入旧密码'
    return
  }
  if (newPassword.value.length < 6) {
    error.value = '新密码长度最少 6 位'
    return
  }
  submitting.value = true
  try {
    await auth.changePassword(oldPassword.value, newPassword.value)
    ok.value = '密码修改成功，即将重新登录…'
    // 画板占位提示：提交后强制重新登录
    setTimeout(async () => {
      await auth.logout()
      void router.replace('/login')
    }, 900)
  } catch (e) {
    error.value = (e as Error).message || '修改失败'
  } finally {
    submitting.value = false
  }
}
</script>
