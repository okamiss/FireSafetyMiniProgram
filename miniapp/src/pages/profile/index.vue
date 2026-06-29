<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
onShow(() => session.restore())

async function logout() {
  const result = await uni.showModal({ title: '退出登录', content: '确认退出当前账号吗？' })
  if (!result.confirm) return
  await session.logout()
  uni.navigateTo({ url: '/pages/login/index' })
}
function roleName() { return session.user?.role === 'ENTERPRISE_ADMIN' ? '企业管理员' : '企业员工' }
</script>

<template>
  <view class="page">
    <view class="profile-header"><view class="avatar">{{ session.user?.displayName?.slice(0, 1) || '用' }}</view><view><text class="profile-name">{{ session.user?.displayName || '未登录' }}</text><text class="muted">{{ roleName() }}</text></view></view>
    <view class="panel detail-list"><view><text>所属企业</text><text>企业 #{{ session.user?.enterpriseId || '-' }}</text></view><view><text>培训记录</text><text>查看</text></view><view><text>我的证书</text><text>查看</text></view></view>
    <button v-if="session.authenticated" class="secondary-button" @click="logout">退出登录</button>
  </view>
</template>
