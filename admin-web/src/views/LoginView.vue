<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const session = useSessionStore()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    await session.login(form.username, form.password)
    await router.replace('/dashboard')
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span class="brand-mark">消</span>
        <div><strong>消防管理后台</strong><span>企业消防业务管理</span></div>
      </div>
      <h1>管理账号登录</h1>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码"
            :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" native-type="submit" :loading="submitting" class="full-button">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
