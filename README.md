# 企业消防小程序 + 管理后台

本仓库按标准方案搭建三端开发框架：

- `server`: Spring Boot 3.5.x 后端 REST API
- `admin-web`: Vue 3 + Element Plus 管理后台
- `miniapp`: uni-app 微信小程序
- `deploy`: Docker Compose 与 Nginx 部署配置

## 本地开发

### 后端

```powershell
$mavenDir = Join-Path $env:TEMP 'apache-maven-3.9.11'
& (Join-Path $mavenDir 'bin\mvn.cmd') test
& (Join-Path $mavenDir 'bin\mvn.cmd') spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/api/health
```

### 管理后台

```powershell
cd admin-web
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

### 微信小程序

```powershell
cd miniapp
npm install --registry=https://registry.npmmirror.com
npm run build:mp-weixin
```

然后使用微信开发者工具导入：

```text
miniapp/dist/build/mp-weixin
```

## 标准方案模块

- 组织与用户管理
- 权限开通工单
- 消防报修工单
- 消防培训题库、任务、答题记录
- 培训证书
- 站内消息与微信订阅消息
- 简易数据看板
- Excel 导入导出
- 操作日志

## 当前状态

当前已完成开发框架搭建，业务页面和模块已预留结构。下一步建议优先开发后端领域模型、数据库迁移和认证权限闭环。
