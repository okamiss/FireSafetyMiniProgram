<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { createEnterprise, disableUser, listEnterprises, listEnterpriseUsers,
  type Enterprise, type EnterpriseUser } from '../api/organizations'

const loading = ref(false)
const enterprises = ref<Enterprise[]>([])
const dialogVisible = ref(false)
const userDrawer = ref(false)
const users = ref<EnterpriseUser[]>([])
const selectedEnterprise = ref<Enterprise | null>(null)
const submitting = ref(false)
const form = reactive({ parentId: null as number | null, name: '', contactName: '', contactPhone: '',
  administratorName: '', administratorPhone: '' })

async function load() {
  loading.value = true
  try { enterprises.value = await listEnterprises() }
  catch { ElMessage.error('企业数据加载失败') }
  finally { loading.value = false }
}

async function submit() {
  submitting.value = true
  try {
    await createEnterprise(form)
    ElMessage.success('企业和首个管理员已创建')
    dialogVisible.value = false
    Object.assign(form, { parentId: null, name: '', contactName: '', contactPhone: '', administratorName: '', administratorPhone: '' })
    await load()
  } catch { ElMessage.error('创建失败，请检查手机号是否已开通') }
  finally { submitting.value = false }
}

async function showUsers(enterprise: Enterprise) {
  selectedEnterprise.value = enterprise
  userDrawer.value = true
  try { users.value = await listEnterpriseUsers(enterprise.id) }
  catch { ElMessage.error('用户数据加载失败') }
}

async function stopUser(user: EnterpriseUser) {
  await ElMessageBox.confirm(`确认停用 ${user.displayName} 吗？`, '停用用户', { type: 'warning' })
  await disableUser(user.id)
  if (selectedEnterprise.value) users.value = await listEnterpriseUsers(selectedEnterprise.value.id)
  ElMessage.success('用户已停用')
}

onMounted(load)
</script>

<template>
  <section class="work-section">
    <div class="section-toolbar">
      <div><h2>企业列表</h2></div>
      <div><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" :icon="Plus" @click="dialogVisible = true">新增企业</el-button></div>
    </div>
    <el-table v-loading="loading" :data="enterprises" empty-text="暂无企业">
      <el-table-column prop="name" label="企业名称" min-width="180" />
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="150" />
      <el-table-column label="类型" width="110"><template #default="scope">{{ scope.row.parentId ? '子企业' : '总部' }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="110" fixed="right"><template #default="scope"><el-button link type="primary" @click="showUsers(scope.row)">查看用户</el-button></template></el-table-column>
    </el-table>
  </section>

  <el-dialog v-model="dialogVisible" title="新增企业" width="560px">
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="上级企业"><el-select v-model="form.parentId" clearable placeholder="不选择则创建总部" class="full-width"><el-option v-for="item in enterprises" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
      <div class="form-grid"><el-form-item label="企业名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item></div>
      <div class="form-grid"><el-form-item label="联系电话"><el-input v-model="form.contactPhone" maxlength="11" /></el-form-item><el-form-item label="首个管理员姓名"><el-input v-model="form.administratorName" /></el-form-item></div>
      <el-form-item label="首个管理员手机号"><el-input v-model="form.administratorPhone" maxlength="11" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">创建并开通管理员</el-button></template>
  </el-dialog>

  <el-drawer v-model="userDrawer" :title="`${selectedEnterprise?.name || ''} - 用户`" size="620px">
    <el-table :data="users" empty-text="暂无用户">
      <el-table-column prop="displayName" label="姓名" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="角色" width="110"><template #default="scope">{{ scope.row.role === 'ENTERPRISE_ADMIN' ? '企业管理员' : '员工' }}</template></el-table-column>
      <el-table-column label="微信" width="90"><template #default="scope">{{ scope.row.weChatBound ? '已绑定' : '未绑定' }}</template></el-table-column>
      <el-table-column label="操作" width="80"><template #default="scope"><el-button v-if="scope.row.enabled" link type="danger" @click="stopUser(scope.row)">停用</el-button><span v-else>已停用</span></template></el-table-column>
    </el-table>
  </el-drawer>
</template>
