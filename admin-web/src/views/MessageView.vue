<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listMessages, type ExternalDeliveryStatus, type StationMessage } from '../api/messages'

const loading = ref(false)
const messages = ref<StationMessage[]>([])
const type = ref('')
const externalStatus = ref<ExternalDeliveryStatus | ''>('')
const filtered = computed(() => messages.value.filter(item =>
  (!type.value || item.messageType === type.value)
  && (!externalStatus.value || item.externalStatus === externalStatus.value)))
const typeLabels: Record<string, string> = {
  PERMISSION_REQUEST: '权限申请', REPAIR_STATUS: '报修状态', TRAINING_TASK: '培训任务',
}
const deliveryLabels: Record<ExternalDeliveryStatus, string> = {
  PENDING: '待发送', SENT: '已发送', SKIPPED: '未发送', FAILED: '发送失败',
}

async function load() {
  loading.value = true
  try { messages.value = await listMessages() }
  catch { ElMessage.error('消息数据加载失败') }
  finally { loading.value = false }
}
function formatTime(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }
function deliveryType(value: ExternalDeliveryStatus) {
  return ({ PENDING: 'warning', SENT: 'success', SKIPPED: 'info', FAILED: 'danger' } as const)[value]
}
onMounted(load)
</script>

<template>
  <section class="work-section">
    <div class="section-toolbar">
      <div><h2>消息通知</h2><p>查看站内消息及微信订阅消息发送结果</p></div>
      <div>
        <el-select v-model="type" clearable placeholder="全部类型" style="width: 140px"><el-option v-for="(label, value) in typeLabels" :key="value" :label="label" :value="value" /></el-select>
        <el-select v-model="externalStatus" clearable placeholder="全部发送状态" style="width: 150px"><el-option v-for="(label, value) in deliveryLabels" :key="value" :label="label" :value="value" /></el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="filtered" empty-text="暂无消息">
      <el-table-column label="类型" width="110"><template #default="scope">{{ typeLabels[scope.row.messageType] || scope.row.messageType }}</template></el-table-column>
      <el-table-column prop="title" label="标题" min-width="170" />
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="recipientUserId" label="接收用户" width="100" />
      <el-table-column label="业务对象" width="130"><template #default="scope">{{ scope.row.businessType || '-' }} {{ scope.row.businessId || '' }}</template></el-table-column>
      <el-table-column label="站内状态" width="100"><template #default="scope"><el-tag :type="scope.row.read ? 'success' : 'warning'">{{ scope.row.read ? '已读' : '未读' }}</el-tag></template></el-table-column>
      <el-table-column label="微信状态" width="110"><template #default="scope"><el-tooltip :content="scope.row.externalErrorMessage || deliveryLabels[scope.row.externalStatus as ExternalDeliveryStatus]" placement="top"><el-tag :type="deliveryType(scope.row.externalStatus)">{{ deliveryLabels[scope.row.externalStatus as ExternalDeliveryStatus] }}</el-tag></el-tooltip></template></el-table-column>
      <el-table-column label="创建时间" width="180"><template #default="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
    </el-table>
  </section>
</template>
