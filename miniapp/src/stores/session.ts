import { defineStore } from 'pinia'
import { bindWeChatPhone, logout as logoutApi, weChatLogin,
  type AuthenticationResult, type SessionUser } from '../api/auth'

function loginCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (result) => result.code ? resolve(result.code) : reject(new Error('未获取到微信登录凭证')),
      fail: () => reject(new Error('微信登录失败')),
    })
  })
}

function restoreUser(): SessionUser | null {
  const value = uni.getStorageSync('miniapp_user') as string
  if (!value) return null
  try { return JSON.parse(value) as SessionUser }
  catch { uni.removeStorageSync('miniapp_user'); return null }
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: '' as string,
    refreshToken: '' as string,
    user: null as SessionUser | null,
    bindingTicket: '' as string,
  }),
  getters: {
    authenticated: (state) => Boolean(state.token && state.user),
    isEnterpriseAdmin: (state) => state.user?.role === 'ENTERPRISE_ADMIN',
  },
  actions: {
    restore() {
      this.token = (uni.getStorageSync('miniapp_token') as string) || ''
      this.refreshToken = (uni.getStorageSync('miniapp_refresh_token') as string) || ''
      this.user = restoreUser()
    },
    async login() {
      const result = await weChatLogin(await loginCode())
      if (result.bindingRequired) {
        this.bindingTicket = result.bindingTicket || ''
        return false
      }
      if (!result.user || !result.tokens) throw new Error('登录响应不完整')
      this.applyAuthentication({ user: result.user, tokens: result.tokens })
      return true
    },
    async bindPhone(phoneCode: string) {
      if (!this.bindingTicket) throw new Error('手机号绑定凭证已失效')
      this.applyAuthentication(await bindWeChatPhone(this.bindingTicket, phoneCode))
      this.bindingTicket = ''
    },
    applyAuthentication(result: AuthenticationResult) {
      this.token = result.tokens.accessToken
      this.refreshToken = result.tokens.refreshToken
      this.user = result.user
      uni.setStorageSync('miniapp_token', this.token)
      uni.setStorageSync('miniapp_refresh_token', this.refreshToken)
      uni.setStorageSync('miniapp_user', JSON.stringify(this.user))
    },
    async logout() {
      try { if (this.token) await logoutApi() } catch { /* local session still needs clearing */ }
      this.clear()
    },
    clear() {
      this.token = ''
      this.refreshToken = ''
      this.user = null
      this.bindingTicket = ''
      uni.removeStorageSync('miniapp_token')
      uni.removeStorageSync('miniapp_refresh_token')
      uni.removeStorageSync('miniapp_user')
    },
  },
})
