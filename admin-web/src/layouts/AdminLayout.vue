<template>
  <el-container class="admin-shell">
    <el-aside width="232px" class="admin-aside">
      <div class="brand">
        <div class="brand-mark">消</div>
        <div>
          <strong>消防管理后台</strong>
          <span>标准方案</span>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="admin-menu">
        <el-menu-item index="/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/enterprises">企业与用户</el-menu-item>
        <el-menu-item index="/permissions">权限工单</el-menu-item>
        <el-menu-item index="/repairs">消防报修</el-menu-item>
        <el-menu-item index="/trainings">消防培训</el-menu-item>
        <el-menu-item index="/messages">消息通知</el-menu-item>
        <el-menu-item index="/audits">操作日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div>
          <h1>{{ $route.meta.title }}</h1>
          <p>企业消防小程序 + 管理后台</p>
        </div>
        <el-tag type="success">一次性交付</el-tag>
      </el-header>
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
