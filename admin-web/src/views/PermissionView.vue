<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listPermissionRequests, reviewPermissionRequest, type PermissionRequest, type PermissionStatus } from '../api/permissions'

const loading = ref(false)
const requests = ref<PermissionRequest[]>([])
const status = ref<PermissionStatus | ''>('')
const dialogVisible = ref(false)
const current = ref<PermissionRequest | null>(null)
const action = ref<'approve' | 'reject'>('approve')
const remark = ref('')
const submitting = ref(false)
const filtered = computed(() => status.value ? requests.value.filter(item => item.status === status.value) : requests.value)

async function load() {
  loading.value = true
  try { requests.value = await listPermissionRequests() }
  catch { ElMessage.error('权限工单加载失败') }
  finally { loading.value = false }
}
function openReview(item: PermissionRequest, nextAction: 'approve' | 'reject') {
  current.value = item; action.value = nextAction; remark.value = ''; dialogVisible.value = true
}
async function submit() {
  if (!current.value || !remark.value.trim()) { ElMessage.warning('请填写审核意见'); return }
  submitting.value = true
  try {
    await reviewPermissionRequest(current.value.id, action.value, remark.value.trim())
    ElMessage.success(action.value === 'approve' ? '申请已通过' : '申请已驳回')
    dialogVisible.value = false
    await load()
  } catch { ElMessage.error('审核失败，工单状态可能已变化') }
  finally { submitting.value = false }
}
function statusText(value: PermissionStatus) { return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[value] }
function statusType(value: PermissionStatus) { return value === 'PENDING' ? 'warning' : value === 'APPROVED' ? 'success' : 'danger' }
onMounted(load)
</script>

<template>
  <section class="work-section">
    <div class="section-toolbar">
      <div><h2>权限开通工单</h2></div>
      <div><el-select v-model="status" clearable placeholder="全部状态" style="width: 140px"><el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" /></el-select><el-button :icon="Refresh" @click="load">刷新</el-button></div>
    </div>
    <el-table v-loading="loading" :data="filtered" empty-text="暂无权限工单">
      <el-table-column prop="id" label="工单号" width="90" />
      <el-table-column prop="enterpriseId" label="企业编号" width="100" />
      <el-table-column prop="requestedName" label="员工姓名" min-width="120" />
      <el-table-column prop="requestedPhone" label="手机号" width="150" />
      <el-table-column label="提交时间" width="180"><template #default="scope">{{ new Date(scope.row.createdAt).toLocaleString('zh-CN') }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="140" fixed="right"><template #default="scope"><template v-if="scope.row.status === 'PENDING'"><el-button link type="success" @click="openReview(scope.row, 'approve')">通过</el-button><el-button link type="danger" @click="openReview(scope.row, 'reject')">驳回</el-button></template><span v-else>{{ scope.row.reviewRemark }}</span></template></el-table-column>
    </el-table>
  </section>
  <el-dialog v-model="dialogVisible" :title="action === 'approve' ? '通过权限申请' : '驳回权限申请'" width="480px">
    <el-descriptions v-if="current" :column="1" border><el-descriptions-item label="员工">{{ current.requestedName }}</el-descriptions-item><el-descriptions-item label="手机号">{{ current.requestedPhone }}</el-descriptions-item></el-descriptions>
    <el-input v-model="remark" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审核意见" class="review-remark" />
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button :type="action === 'approve' ? 'success' : 'danger'" :loading="submitting" @click="submit">确认{{ action === 'approve' ? '通过' : '驳回' }}</el-button></template>
  </el-dialog>
</template>
