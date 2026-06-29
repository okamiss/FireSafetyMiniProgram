<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
onShow(() => {
  session.restore()
  if (!session.authenticated) uni.navigateTo({ url: '/pages/login/index' })
})
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="hero-kicker">企业消防服务</text>
      <text class="hero-title">{{ session.user?.displayName || '您好' }}</text>
      <text class="hero-subtitle">报修、培训与业务通知统一处理</text>
    </view>
    <view class="quick-grid">
      <navigator url="/pages/repair/index" class="quick-card"><text class="quick-title">提交报修</text><text class="quick-desc">记录现场问题和处理进度</text></navigator>
      <navigator url="/pages/training/index" class="quick-card"><text class="quick-title">消防培训</text><text class="quick-desc">完成任务、考试与证书</text></navigator>
      <navigator url="/pages/messages/index" class="quick-card"><text class="quick-title">我的消息</text><text class="quick-desc">查看工单与培训提醒</text></navigator>
      <navigator v-if="session.isEnterpriseAdmin" url="/pages/permission/index" class="quick-card"><text class="quick-title">员工权限</text><text class="quick-desc">提交并跟踪开通申请</text></navigator>
    </view>
  </view>
</template>
