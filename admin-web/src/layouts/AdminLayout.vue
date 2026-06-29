<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Bell, DataAnalysis, DocumentChecked, OfficeBuilding, Setting, Tickets, Tools } from '@element-plus/icons-vue'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const session = useSessionStore()

async function signOut() {
  await ElMessageBox.confirm('确认退出当前管理账号吗？', '退出登录', { type: 'warning' })
  await session.logout()
  await router.replace('/login')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="224px" class="admin-aside">
      <div class="brand"><div class="brand-mark">消</div><div><strong>消防管理后台</strong><span>企业消防业务管理</span></div></div>
      <el-menu router :default-active="$route.path" class="admin-menu">
        <el-menu-item index="/dashboard" title="数据看板"><el-icon><DataAnalysis /></el-icon><span>数据看板</span></el-menu-item>
        <el-menu-item index="/enterprises" title="企业与用户"><el-icon><OfficeBuilding /></el-icon><span>企业与用户</span></el-menu-item>
        <el-menu-item index="/permissions" title="权限工单"><el-icon><DocumentChecked /></el-icon><span>权限工单</span></el-menu-item>
        <el-menu-item index="/repairs" title="消防报修"><el-icon><Tools /></el-icon><span>消防报修</span></el-menu-item>
        <el-menu-item index="/trainings" title="消防培训"><el-icon><Tickets /></el-icon><span>消防培训</span></el-menu-item>
        <el-menu-item index="/messages" title="消息通知"><el-icon><Bell /></el-icon><span>消息通知</span></el-menu-item>
        <el-menu-item index="/audits" title="操作日志"><el-icon><Setting /></el-icon><span>操作日志</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div><h1>{{ $route.meta.title }}</h1><p>企业消防小程序业务管理</p></div>
        <div class="header-user"><span>{{ session.user?.displayName || '管理账号' }}</span><el-button text @click="signOut">退出</el-button></div>
      </el-header>
      <el-main class="admin-main"><RouterView /></el-main>
    </el-container>
  </el-container>
</template>
