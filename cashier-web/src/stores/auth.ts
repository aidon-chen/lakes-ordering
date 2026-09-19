import { defineStore } from 'pinia'
import {
  getLoginMerchantInfo,
  getLoginShopInfo,
  logout as logoutApi,
  updatePassword as updatePasswordApi,
  type MerchantInfo,
  type ShopInfo
} from '@/api/auth'

/** 外壳与账号页共用的登录身份信息（商家 + 所属门店） */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    merchant: null as MerchantInfo | null,
    shop: null as ShopInfo | null,
    loading: false,
    loaded: false
  }),

  getters: {
    /** 头部展示的商家账号名 */
    accountName(s): string {
      return s.merchant?.nickname || s.merchant?.username || '商家'
    },
    /** 头部展示的门店名（画板：声呐 SONAR · 音乐酒馆） */
    shopName(s): string {
      return s.shop?.name || (s.merchant?.shopId ? `门店 #${s.merchant.shopId}` : '—')
    },
    /** 头像占位首字（无头像图时用） */
    initial(): string {
      return this.accountName.slice(0, 1).toUpperCase()
    },
    /**
     * 角色：tb_merchant.roles 列存在但当前全为 NULL，且无角色枚举与校验逻辑
     * → 如实回显「未配置」，不臆造角色名
     */
    roleLabel(s): string {
      const roles = (s.merchant?.roles ?? '').trim()
      return roles || '未配置'
    },
    /**
     * 权限范围：M-01 的角色/菜单/按钮级权限模型尚未实现（后端无 role/permission 类，
     * 越权仅靠各处手写 shopId 比对）→ 此处只陈述现状，不伪造权限清单
     */
    permissionLabel(): string {
      return '暂未接入角色权限模型（当前按门店 shopId 隔离）'
    }
  },

  actions: {
    /** 拉取登录身份信息（登录成功后与壳层首次挂载时调用） */
    async load(force = false) {
      if (this.loading) return
      if (this.loaded && !force) return
      this.loading = true
      try {
        const [merchant, shop] = await Promise.all([
          getLoginMerchantInfo(),
          getLoginShopInfo().catch(() => null)
        ])
        this.merchant = merchant
        this.shop = shop
        this.loaded = true
      } catch (e) {
        console.error('[auth] 拉取登录信息失败', e)
      } finally {
        this.loading = false
      }
    },

    /** 清空身份信息（退出登录时调用） */
    reset() {
      this.merchant = null
      this.shop = null
      this.loaded = false
    },

    /** 修改密码（成功与否由调用方处理提示；后端校验原密码与长度） */
    async changePassword(oldPassword: string, newPassword: string) {
      await updatePasswordApi(oldPassword, newPassword)
    },

    /** 退出登录：先请求后端失效会话，失败也照常清本地，避免卡住用户 */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        console.warn('[auth] 退出接口失败，仍清空本地登录态', e)
      } finally {
        localStorage.removeItem('token')
        this.reset()
      }
    }
  }
})
