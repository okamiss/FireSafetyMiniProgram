<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  createRepair,
  downloadAttachment,
  getRepairAttachments,
  getRepairHistory,
  listRepairs,
  uploadRepairAttachment,
  type RepairAttachment,
  type RepairHistory,
  type RepairStatus,
  type RepairTicket,
} from '../../api/repairs'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const activeTab = ref<'list' | 'create'>('list')
const loading = ref(false)
const submitting = ref(false)
const repairs = ref<RepairTicket[]>([])
const selected = ref<RepairTicket | null>(null)
const history = ref<RepairHistory[]>([])
const attachments = ref<RepairAttachment[]>([])
const photoPaths = ref<string[]>([])
const urgencyIndex = ref(0)
const form = reactive({ faultType: '', location: '', description: '', contactName: '', contactPhone: '' })

const statusLabels: Record<RepairStatus, string> = {
  PENDING_ACCEPTANCE: '待受理', PROCESSING: '处理中', COMPLETED: '已完成', CLOSED: '已关闭',
}

async function load() {
  loading.value = true
  try { repairs.value = await listRepairs() }
  catch (error) { showError(error, '报修记录加载失败') }
  finally { loading.value = false }
}

async function choosePhotos() {
  const remaining = 6 - photoPaths.value.length
  if (remaining <= 0) { uni.showToast({ title: '最多选择 6 张照片', icon: 'none' }); return }
  try {
    const result = await uni.chooseMedia({ count: remaining, mediaType: ['image'], sourceType: ['album', 'camera'] })
    photoPaths.value.push(...result.tempFiles.map(file => file.tempFilePath))
  } catch { /* User cancelled the picker. */ }
}

function removePhoto(index: number) { photoPaths.value.splice(index, 1) }

async function submit() {
  if (!form.faultType.trim() || !form.location.trim() || !form.description.trim() || !form.contactName.trim()) {
    uni.showToast({ title: '请完整填写报修信息', icon: 'none' }); return
  }
  if (!/^1\d{10}$/.test(form.contactPhone)) {
    uni.showToast({ title: '请填写正确的联系人手机号', icon: 'none' }); return
  }
  submitting.value = true
  let ticket: RepairTicket | null = null
  try {
    ticket = await createRepair({
      urgency: urgencyIndex.value === 1 ? 'URGENT' : 'NORMAL',
      faultType: form.faultType.trim(), location: form.location.trim(), description: form.description.trim(),
      contactName: form.contactName.trim(), contactPhone: form.contactPhone,
    })
    for (const path of photoPaths.value) await uploadRepairAttachment(ticket.id, path)
    resetForm()
    activeTab.value = 'list'
    uni.showToast({ title: '报修已提交', icon: 'success' })
    await load()
  } catch (error) {
    showError(error, ticket ? '工单已创建，部分照片上传失败' : '报修提交失败')
    if (ticket) { activeTab.value = 'list'; await load() }
  } finally { submitting.value = false }
}

async function openDetail(item: RepairTicket) {
  selected.value = item
  loading.value = true
  try {
    [history.value, attachments.value] = await Promise.all([
      getRepairHistory(item.id), getRepairAttachments(item.id),
    ])
  } catch (error) { showError(error, '工单详情加载失败') }
  finally { loading.value = false }
}

async function previewAttachment(item: RepairAttachment) {
  uni.showLoading({ title: '加载照片' })
  try {
    const path = await downloadAttachment(item.id)
    await uni.previewImage({ urls: [path], current: path })
  } catch (error) { showError(error, '照片加载失败') }
  finally { uni.hideLoading() }
}

function resetForm() {
  Object.assign(form, { faultType: '', location: '', description: '', contactName: '', contactPhone: '' })
  urgencyIndex.value = 0
  photoPaths.value = []
}
function showError(error: unknown, fallback: string) {
  uni.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
}
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN') }
function statusText(value: RepairStatus) { return statusLabels[value] }

onShow(() => {
  session.restore()
  if (!session.authenticated) { uni.navigateTo({ url: '/pages/login/index' }); return }
  if (!form.contactName) form.contactName = session.user?.displayName || ''
  load()
})
</script>

<template>
  <view class="page repair-page">
    <view class="repair-tabs">
      <view :class="['repair-tab', { active: activeTab === 'list' }]" @click="activeTab = 'list'">我的报修</view>
      <view :class="['repair-tab', { active: activeTab === 'create' }]" @click="activeTab = 'create'; selected = null">提交报修</view>
    </view>

    <template v-if="activeTab === 'list'">
      <view v-if="selected" class="panel repair-detail-panel">
        <view class="detail-heading"><text class="section-title">工单 #{{ selected.id }}</text><text class="detail-close" @click="selected = null">返回列表</text></view>
        <text :class="`repair-status repair-status-${selected.status.toLowerCase()}`">{{ statusText(selected.status) }}</text>
        <view class="repair-detail-row"><text>故障类型</text><text>{{ selected.faultType }}</text></view>
        <view class="repair-detail-row"><text>现场位置</text><text>{{ selected.location }}</text></view>
        <view class="repair-description">{{ selected.description }}</view>
        <view v-if="selected.result" class="repair-result"><text>处理结果</text><text>{{ selected.result }}</text></view>
        <text class="detail-subtitle">现场照片</text>
        <view v-if="attachments.length" class="attachment-list"><view v-for="item in attachments" :key="item.id" class="attachment-item" @click="previewAttachment(item)">{{ item.originalName }}<text>查看</text></view></view>
        <text v-else class="muted">未上传现场照片</text>
        <text class="detail-subtitle">流转记录</text>
        <view v-for="item in history" :key="item.id" class="history-item"><view class="history-dot" /><view><text>{{ statusText(item.toStatus) }}</text><text class="muted">{{ item.remark }}</text><text class="history-time">{{ formatTime(item.createdAt) }}</text></view></view>
      </view>
      <template v-else>
        <view class="list-header"><text class="section-title">报修记录</text><text class="muted">{{ loading ? '加载中' : `${repairs.length} 条` }}</text></view>
        <view v-if="!loading && repairs.length === 0" class="empty-state">暂无报修记录</view>
        <view v-for="item in repairs" :key="item.id" class="repair-card" @click="openDetail(item)">
          <view class="repair-card-head"><text class="repair-card-title">{{ item.faultType }}</text><text :class="`repair-status repair-status-${item.status.toLowerCase()}`">{{ statusText(item.status) }}</text></view>
          <text class="repair-location">{{ item.location }}</text>
          <view class="repair-card-foot"><text>{{ item.urgency === 'URGENT' ? '紧急' : '普通' }}</text><text>{{ formatTime(item.createdAt) }}</text></view>
        </view>
      </template>
    </template>

    <view v-else class="panel repair-form">
      <text class="section-title">提交消防报修</text>
      <text class="field-label">紧急程度</text>
      <picker :range="['普通', '紧急']" :value="urgencyIndex" @change="urgencyIndex = Number($event.detail.value)"><view class="field picker-field">{{ urgencyIndex === 1 ? '紧急' : '普通' }}</view></picker>
      <text class="field-label">故障类型</text><input v-model="form.faultType" class="field" maxlength="64" placeholder="如：消防设施、通道占用" />
      <text class="field-label">现场位置</text><input v-model="form.location" class="field" maxlength="200" placeholder="楼栋、楼层和具体位置" />
      <text class="field-label">问题描述</text><textarea v-model="form.description" class="field textarea-field" maxlength="2000" placeholder="描述现场情况和影响" />
      <text class="field-label">联系人</text><input v-model="form.contactName" class="field" maxlength="100" placeholder="联系人姓名" />
      <text class="field-label">联系电话</text><input v-model="form.contactPhone" class="field" type="number" maxlength="11" placeholder="11 位手机号" />
      <view class="photo-heading"><text class="field-label">现场照片（最多 6 张）</text><text class="photo-add" @click="choosePhotos">选择照片</text></view>
      <view v-if="photoPaths.length" class="photo-grid"><view v-for="(path, index) in photoPaths" :key="path" class="photo-item"><image :src="path" mode="aspectFill" @click="uni.previewImage({ urls: photoPaths, current: path })" /><text @click="removePhoto(index)">删除</text></view></view>
      <button class="primary-button" :loading="submitting" :disabled="submitting" @click="submit">提交报修</button>
    </view>
  </view>
</template>
