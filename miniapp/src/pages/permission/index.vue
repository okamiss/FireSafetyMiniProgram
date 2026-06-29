<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createPermissionRequest, listPermissionRequests, type PermissionRequest,
  type PermissionStatus } from '../../api/permissions'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const name = ref('')
const phone = ref('')
const requests = ref<PermissionRequest[]>([])
const loading = ref(false)
const submitting = ref(false)

async function load() {
  loading.value = true
  try { requests.value = await listPermissionRequests() }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' }) }
  finally { loading.value = false }
}

async function submit() {
  if (!name.value.trim() || !/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: '请填写姓名和正确手机号', icon: 'none' }); return
  }
  submitting.value = true
  try {
    await createPermissionRequest(name.value.trim(), phone.value)
    name.value = ''; phone.value = ''
    uni.showToast({ title: '申请已提交', icon: 'success' })
    await load()
  } catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' }) }
  finally { submitting.value = false }
}

function statusText(status: PermissionStatus) { return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] }
onShow(() => {
  session.restore()
  if (!session.isEnterpriseAdmin) { uni.showToast({ title: '仅企业管理员可使用', icon: 'none' }); return }
  load()
})
</script>

<template>
  <view class="page">
    <view class="panel form-panel">
      <text class="section-title">员工权限申请</text>
      <text class="muted">平台审核通过后，员工可使用该手机号绑定微信登录。</text>
      <input v-model="name" class="field" maxlength="30" placeholder="员工姓名" />
      <input v-model="phone" class="field" type="number" maxlength="11" placeholder="员工手机号" />
      <button class="primary-button" :loading="submitting" @click="submit">提交申请</button>
    </view>
    <view class="list-header"><text class="section-title">申请记录</text><text class="muted">{{ loading ? '加载中' : `${requests.length} 条` }}</text></view>
    <view v-if="!loading && requests.length === 0" class="empty-state">暂无申请记录</view>
    <view v-for="item in requests" :key="item.id" class="request-row">
      <view><text class="request-name">{{ item.requestedName }}</text><text class="request-phone">{{ item.requestedPhone }}</text></view>
      <view class="request-state"><text :class="`status status-${item.status.toLowerCase()}`">{{ statusText(item.status) }}</text><text v-if="item.reviewRemark" class="request-remark">{{ item.reviewRemark }}</text></view>
    </view>
  </view>
</template>
