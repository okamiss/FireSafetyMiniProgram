<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listAuditLogs, type AuditAction, type AuditModule, type OperationLog } from '../api/audits'

const loading = ref(false)
const logs = ref<OperationLog[]>([])
const moduleFilter = ref<AuditModule | ''>('')
const actionFilter = ref<AuditAction | ''>('')
const moduleLabels: Record<AuditModule, string> = { AUTH: '认证', PERMISSION: '权限工单', REPAIR: '消防报修' }
const actionLabels: Record<AuditAction, string> = {
  ADMIN_LOGIN: '管理员登录', APPROVE: '审批通过', REJECT: '审批驳回',
  ACCEPT: '受理工单', COMPLETE: '完成工单', CLOSE: '关闭工单',
}
const filtered = computed(() => logs.value.filter(item =>
  (!moduleFilter.value || item.module === moduleFilter.value)
  && (!actionFilter.value || item.action === actionFilter.value)))

async function load() {
  loading.value = true
  try { logs.value = await listAuditLogs() }
  catch { ElMessage.error('操作日志加载失败') }
  finally { loading.value = false }
}
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN') }
onMounted(load)
</script>

<template>
  <section class="work-section">
    <div class="section-toolbar">
      <div><h2>操作日志</h2><p>管理员登录、权限审批与工单状态变更</p></div>
      <div>
        <el-select v-model="moduleFilter" clearable placeholder="全部模块" style="width: 140px">
          <el-option v-for="(label, value) in moduleLabels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="actionFilter" clearable placeholder="全部操作" style="width: 150px">
          <el-option v-for="(label, value) in actionLabels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="filtered" empty-text="暂无操作日志">
      <el-table-column label="模块" width="110"><template #default="scope">{{ moduleLabels[scope.row.module as AuditModule] }}</template></el-table-column>
      <el-table-column label="操作" width="120"><template #default="scope">{{ actionLabels[scope.row.action as AuditAction] }}</template></el-table-column>
      <el-table-column label="操作人" width="100"><template #default="scope">#{{ scope.row.operatorId }}</template></el-table-column>
      <el-table-column label="业务对象" width="110"><template #default="scope">{{ scope.row.businessId ? `#${scope.row.businessId}` : '-' }}</template></el-table-column>
      <el-table-column prop="detail" label="结果说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="IP 地址" width="150"><template #default="scope">{{ scope.row.ipAddress || '-' }}</template></el-table-column>
      <el-table-column label="操作时间" width="180"><template #default="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
    </el-table>
  </section>
</template>
