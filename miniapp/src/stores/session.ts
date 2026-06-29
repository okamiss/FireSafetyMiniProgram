import { defineStore } from 'pinia'

interface SessionState {
  token: string
  userName: string
  enterpriseName: string
}

export const useSessionStore = defineStore('session', {
  state: (): SessionState => ({
    token: '',
    userName: '',
    enterpriseName: '',
  }),
  actions: {
    restore() {
      this.token = uni.getStorageSync('miniapp_token') || ''
      this.userName = uni.getStorageSync('miniapp_user_name') || ''
      this.enterpriseName = uni.getStorageSync('miniapp_enterprise_name') || ''
    },
  },
})
