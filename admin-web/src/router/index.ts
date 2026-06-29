import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import EnterpriseView from '../views/EnterpriseView.vue'
import PermissionView from '../views/PermissionView.vue'
import RepairView from '../views/RepairView.vue'
import TrainingView from '../views/TrainingView.vue'
import MessageView from '../views/MessageView.vue'
import AuditView from '../views/AuditView.vue'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: LoginView, meta: { title: '登录', public: true } },
  {
    path: '/', component: AdminLayout, redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: DashboardView, meta: { title: '数据看板' } },
      { path: 'enterprises', component: EnterpriseView, meta: { title: '企业与用户' } },
      { path: 'permissions', component: PermissionView, meta: { title: '权限工单' } },
      { path: 'repairs', component: RepairView, meta: { title: '消防报修' } },
      { path: 'trainings', component: TrainingView, meta: { title: '消防培训' } },
      { path: 'messages', component: MessageView, meta: { title: '消息通知' } },
      { path: 'audits', component: AuditView, meta: { title: '操作日志' } },
    ],
  },
]

export const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  document.title = `${String(to.meta.title || '消防管理')} - 消防管理后台`
  const authenticated = Boolean(localStorage.getItem('admin_token'))
  if (!to.meta.public && !authenticated) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/login' && authenticated) return '/dashboard'
  return true
})
