<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(false)
const mockPhone = ref('')
const mockEnabled = import.meta.env.VITE_WECHAT_MOCK_ENABLED === 'true'

onShow(() => {
  session.restore()
  if (session.authenticated) uni.switchTab({ url: '/pages/home/index' })
})

async function login() {
  loading.value = true
  try {
    if (await session.login()) uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '登录失败', icon: 'none' })
  } finally { loading.value = false }
}

async function bindPhone(event: { detail: { code?: string; errMsg?: string } }) {
  if (!event.detail.code) {
    uni.showToast({ title: '需要授权手机号才能完成登录', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await session.bindPhone(event.detail.code)
    uni.switchTab({ url: '/pages/home/index' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '手机号绑定失败', icon: 'none' })
  } finally { loading.value = false }
}

async function bindMockPhone() {
  if (!/^1\d{10}$/.test(mockPhone.value)) {
    uni.showToast({ title: '请输入已开通的 11 位手机号', icon: 'none' })
    return
  }
  await bindPhone({ detail: { code: `mock-phone:${mockPhone.value}` } })
}
</script>

<template>
  <view class="login-page">
    <view class="login-symbol">消</view>
    <text class="login-title">企业消防</text>
    <text class="login-subtitle">使用已开通权限的企业手机号登录</text>
    <button v-if="!session.bindingTicket" class="primary-button" :loading="loading" @click="login">微信登录</button>
    <template v-else-if="mockEnabled">
      <input v-model="mockPhone" class="mock-phone-field" type="number" maxlength="11" placeholder="本地联调手机号" />
      <button class="primary-button" :loading="loading" @click="bindMockPhone">绑定并登录</button>
    </template>
    <button v-else class="primary-button" open-type="getPhoneNumber" :loading="loading" @getphonenumber="bindPhone">授权手机号并登录</button>
    <text v-if="session.bindingTicket" class="login-note">手机号仅用于匹配甲方已开通的账号</text>
  </view>
</template>
