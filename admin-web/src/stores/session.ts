import { defineStore } from 'pinia'
import { adminLogin, currentUser, logout as logoutApi } from '../api/auth'
import type { SessionUser } from '../api/types'

function storedUser(): SessionUser | null {
  const value = localStorage.getItem('admin_user')
  if (!value) return null
  try {
    return JSON.parse(value) as SessionUser
  } catch {
    localStorage.removeItem('admin_user')
    return null
  }
}

export const useSessionStore = defineStore('admin-session', {
  state: () => ({ user: storedUser() as SessionUser | null }),
  getters: {
    authenticated: () => Boolean(localStorage.getItem('admin_token')),
  },
  actions: {
    async login(username: string, password: string) {
      const result = await adminLogin(username, password)
      localStorage.setItem('admin_token', result.tokens.accessToken)
      localStorage.setItem('admin_refresh_token', result.tokens.refreshToken)
      localStorage.setItem('admin_user', JSON.stringify(result.user))
      this.user = result.user
    },
    async restore() {
      if (!localStorage.getItem('admin_token')) return false
      try {
        this.user = await currentUser()
        localStorage.setItem('admin_user', JSON.stringify(this.user))
        return true
      } catch {
        this.clear()
        return false
      }
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.clear()
      }
    },
    clear() {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_refresh_token')
      localStorage.removeItem('admin_user')
      this.user = null
    },
  },
})
