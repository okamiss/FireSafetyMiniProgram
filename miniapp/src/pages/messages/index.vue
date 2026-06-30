<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getSubscriptionConfig, listMessages, markMessageRead,
  type StationMessage, type SubscriptionConfig } from '../../api/messages'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(false)
const activeTab = ref<'all' | 'unread'>('all')
const messages = ref<StationMessage[]>([])
const subscription = ref<SubscriptionConfig>({ enabled: false, templateIds: [] })
const visibleMessages = computed(() => activeTab.value === 'unread'
  ? messages.value.filter(item => !item.read) : messages.value)
const unreadCount = computed(() => messages.value.filter(item => !item.read).length)
const typeLabels: Record<string, string> = {
  PERMISSION_REQUEST: '权限申请', REPAIR_STATUS: '报修进度', TRAINING_TASK: '培训任务',
}

async function load() {
  loading.value = true
  try {
    [messages.value, subscription.value] = await Promise.all([listMessages(), getSubscriptionConfig()])
  } catch (error) { showError(error, '消息加载失败') }
  finally { loading.value = false }
}

async function openMessage(item: StationMessage) {
  if (!item.read) {
    try {
      const updated = await markMessageRead(item.id)
      messages.value = messages.value.map(message => message.id === item.id ? updated : message)
    } catch (error) { showError(error, '消息状态更新失败') }
  }
  const target = item.businessType === 'REPAIR' ? '/pages/repair/index'
    : item.businessType === 'TRAINING' ? '/pages/training/index' : ''
  if (target) uni.navigateTo({ url: target })
}

async function enableSubscription() {
  if (!subscription.value.enabled || subscription.value.templateIds.length === 0) {
    uni.showToast({ title: '微信订阅消息尚未配置', icon: 'none' })
    return
  }
  try {
    const result = await uni.requestSubscribeMessage({ tmplIds: subscription.value.templateIds })
    const accepted = subscription.value.templateIds.filter(id => result[id] === 'accept').length
    uni.showToast({ title: accepted ? `已授权 ${accepted} 类提醒` : '未授权提醒', icon: 'none' })
  } catch (error) { showError(error, '订阅授权未完成') }
}

function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN') }
function showError(error: unknown, fallback: string) {
  uni.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
}

onShow(() => {
  session.restore()
  if (!session.authenticated) { uni.navigateTo({ url: '/pages/login/index' }); return }
  load()
})
</script>

<template>
  <view class="page message-page">
    <view class="message-heading"><view><text class="section-title">我的消息</text><text class="muted">{{ unreadCount }} 条未读</text></view><button v-if="subscription.enabled" class="subscribe-button" @click="enableSubscription">开启微信提醒</button></view>
    <view class="message-tabs"><view :class="['message-tab', { active: activeTab === 'all' }]" @click="activeTab = 'all'">全部</view><view :class="['message-tab', { active: activeTab === 'unread' }]" @click="activeTab = 'unread'">未读 {{ unreadCount }}</view></view>
    <view v-if="!loading && visibleMessages.length === 0" class="empty-state">暂无消息</view>
    <view v-for="item in visibleMessages" :key="item.id" :class="['message-row', { unread: !item.read }]" @click="openMessage(item)">
      <view class="message-row-head"><text class="message-type">{{ typeLabels[item.messageType] || '系统消息' }}</text><text class="message-time">{{ formatTime(item.createdAt) }}</text></view>
      <text class="message-title">{{ item.title }}</text><text class="message-content">{{ item.content }}</text>
    </view>
  </view>
</template>

<style scoped>
.message-heading { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; margin-bottom: 24rpx; }
.message-heading .section-title, .message-heading .muted { display: block; }.message-heading .muted { margin-top: 8rpx; }
.subscribe-button { width: auto; height: 64rpx; margin: 0; padding: 0 20rpx; color: #b42318; background: #fff; border: 1rpx solid #d92d20; border-radius: 6rpx; font-size: 24rpx; line-height: 62rpx; }
.message-tabs { display: flex; gap: 36rpx; margin-bottom: 20rpx; border-bottom: 1rpx solid #d8e0ea; }
.message-tab { padding: 18rpx 4rpx; color: #667085; }.message-tab.active { color: #b42318; border-bottom: 4rpx solid #d92d20; font-weight: 600; }
.message-row { position: relative; margin-bottom: 16rpx; padding: 26rpx; background: #fff; border: 1rpx solid #d8e0ea; border-radius: 8rpx; }
.message-row.unread { border-left: 6rpx solid #d92d20; }
.message-row-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }.message-type { color: #b42318; font-size: 24rpx; }.message-time { color: #98a2b3; font-size: 22rpx; }
.message-title, .message-content { display: block; }.message-title { margin-top: 14rpx; color: #1f2933; font-weight: 600; }.message-content { margin-top: 10rpx; color: #475467; line-height: 1.6; }
</style>
