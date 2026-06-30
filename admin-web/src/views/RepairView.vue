<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View } from '@element-plus/icons-vue'
import {
  getAttachmentObjectUrl,
  getRepairAttachments,
  getRepairHistory,
  listRepairs,
  transitionRepair,
  type RepairAttachment,
  type RepairHistory,
  type RepairStatus,
  type RepairTicket,
} from '../api/repairs'

const loading = ref(false)
const repairs = ref<RepairTicket[]>([])
const status = ref<RepairStatus | ''>('')
const drawerVisible = ref(false)
const current = ref<RepairTicket | null>(null)
const history = ref<RepairHistory[]>([])
const attachments = ref<RepairAttachment[]>([])
const attachmentUrls = ref<Record<number, string>>({})
const detailLoading = ref(false)
const actionLoading = ref(false)
const filtered = computed(() => status.value
  ? repairs.value.filter(item => item.status === status.value)
  : repairs.value)

const statusLabels: Record<RepairStatus, string> = {
  PENDING_ACCEPTANCE: '待受理', PROCESSING: '处理中', COMPLETED: '已完成', CLOSED: '已关闭',
}
const statusTypes: Record<RepairStatus, 'warning' | 'primary' | 'success' | 'info'> = {
  PENDING_ACCEPTANCE: 'warning', PROCESSING: 'primary', COMPLETED: 'success', CLOSED: 'info',
}

async function load() {
  loading.value = true
  try { repairs.value = await listRepairs() }
  catch { ElMessage.error('报修工单加载失败') }
  finally { loading.value = false }
}

function releaseAttachmentUrls() {
  Object.values(attachmentUrls.value).forEach(url => URL.revokeObjectURL(url))
  attachmentUrls.value = {}
}

async function openDetail(item: RepairTicket) {
  releaseAttachmentUrls()
  current.value = item
  drawerVisible.value = true
  detailLoading.value = true
  try {
    const [historyResult, attachmentResult] = await Promise.all([
      getRepairHistory(item.id), getRepairAttachments(item.id),
    ])
    history.value = historyResult
    attachments.value = attachmentResult
    const entries = await Promise.all(attachmentResult.map(async attachment =>
      [attachment.id, await getAttachmentObjectUrl(attachment.id)] as const))
    attachmentUrls.value = Object.fromEntries(entries)
  } catch { ElMessage.error('工单详情加载失败') }
  finally { detailLoading.value = false }
}

async function runAction(action: 'accept' | 'complete' | 'close') {
  if (!current.value) return
  const titles = { accept: '受理工单', complete: '完成工单', close: '关闭工单' }
  const prompts = { accept: '请填写受理说明', complete: '请填写处理结果', close: '请填写关闭说明' }
  try {
    const result = await ElMessageBox.prompt(prompts[action], titles[action], {
      confirmButtonText: '确认', cancelButtonText: '取消', inputType: 'textarea',
      inputValidator: value => Boolean(value.trim()) || '说明不能为空',
    })
    actionLoading.value = true
    current.value = await transitionRepair(current.value.id, action, result.value.trim())
    ElMessage.success(`${titles[action]}成功`)
    await load()
    await openDetail(current.value)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('工单状态更新失败，请刷新后重试')
  } finally { actionLoading.value = false }
}

function statusText(value: RepairStatus) { return statusLabels[value] }
function statusType(value: RepairStatus) { return statusTypes[value] }
function formatTime(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }
function formatSize(value: number) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB` }

onMounted(load)
</script>

<template>
  <section class="work-section">
    <div class="section-toolbar">
      <div><h2>消防报修管理</h2><p>统一受理、处理并关闭企业报修工单</p></div>
      <div>
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="filtered" empty-text="暂无报修工单">
      <el-table-column prop="id" label="工单号" width="90" />
      <el-table-column label="紧急程度" width="100"><template #default="scope"><el-tag :type="scope.row.urgency === 'URGENT' ? 'danger' : 'info'">{{ scope.row.urgency === 'URGENT' ? '紧急' : '普通' }}</el-tag></template></el-table-column>
      <el-table-column prop="faultType" label="故障类型" min-width="130" />
      <el-table-column prop="location" label="位置" min-width="160" show-overflow-tooltip />
      <el-table-column label="联系人" width="160"><template #default="scope"><div>{{ scope.row.contactName }}</div><small>{{ scope.row.contactPhone }}</small></template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="提交时间" width="180"><template #default="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="90" fixed="right"><template #default="scope"><el-button link type="primary" :icon="View" @click="openDetail(scope.row)">详情</el-button></template></el-table-column>
    </el-table>
  </section>

  <el-drawer v-model="drawerVisible" title="报修工单详情" size="680px" @closed="releaseAttachmentUrls">
    <div v-loading="detailLoading" class="repair-detail">
      <template v-if="current">
        <div class="repair-actions">
          <el-tag :type="statusType(current.status)" size="large">{{ statusText(current.status) }}</el-tag>
          <el-button v-if="current.status === 'PENDING_ACCEPTANCE'" type="primary" :loading="actionLoading" @click="runAction('accept')">受理</el-button>
          <el-button v-if="current.status === 'PROCESSING'" type="success" :loading="actionLoading" @click="runAction('complete')">完成</el-button>
          <el-button v-if="current.status === 'COMPLETED'" :loading="actionLoading" @click="runAction('close')">关闭</el-button>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ current.id }}</el-descriptions-item>
          <el-descriptions-item label="企业编号">{{ current.enterpriseId }}</el-descriptions-item>
          <el-descriptions-item label="故障类型">{{ current.faultType }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">{{ current.urgency === 'URGENT' ? '紧急' : '普通' }}</el-descriptions-item>
          <el-descriptions-item label="现场位置" :span="2">{{ current.location }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ current.description }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ current.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ current.contactPhone }}</el-descriptions-item>
          <el-descriptions-item v-if="current.result" label="处理结果" :span="2">{{ current.result }}</el-descriptions-item>
        </el-descriptions>

        <h3>现场照片</h3>
        <el-empty v-if="attachments.length === 0" description="未上传现场照片" :image-size="64" />
        <div v-else class="repair-photos">
          <figure v-for="item in attachments" :key="item.id">
            <el-image :src="attachmentUrls[item.id]" :preview-src-list="Object.values(attachmentUrls)" fit="cover" />
            <figcaption>{{ item.originalName }} · {{ formatSize(item.fileSize) }}</figcaption>
          </figure>
        </div>

        <h3>流转记录</h3>
        <el-timeline>
          <el-timeline-item v-for="item in history" :key="item.id" :timestamp="formatTime(item.createdAt)" placement="top">
            <strong>{{ statusText(item.toStatus) }}</strong><div class="timeline-remark">{{ item.remark }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-drawer>
</template>
