# 企业消防小程序与管理后台

一次性交付项目，包含 Spring Boot REST API、Vue 管理后台、uni-app 微信小程序和 Docker Compose 基础设施。

## 当前进度

已完成工程基础、身份权限和消防报修里程碑：

- MySQL Flyway 迁移、JPA 持久化、Redis 不透明会话和本地文件存储。
- 平台管理账号登录、会话刷新、退出和鉴权。
- 微信登录、手机号授权绑定和已开通账号校验。
- 企业及首个企业管理员创建、企业用户查看与停用。
- 企业管理员提交员工权限申请，平台审核通过或驳回。
- 审核结果写入站内消息表。
- 企业用户提交报修并上传最多 6 张现场照片，按本人或企业数据范围查看工单。
- 平台后台受理、完成和关闭报修，保留完整流转记录并写入站内消息。
- 报修照片使用受保护文件接口读取，服务端校验类型、内容、大小和数据权限。

消防培训与证书、完整消息中心、看板、导入导出和操作日志查询仍按一次性交付计划继续开发。

## 一起启动

前置环境：Java 21、Maven、Node.js 22、npm、Docker Desktop。

```powershell
.\start-dev.ps1
.\status-dev.ps1
.\stop-dev.ps1
```

启动器会先启动 MySQL 和 Redis，再分别启动后端、管理后台和小程序编译进程。

- 后端健康检查：`http://localhost:8080/api/health`
- 管理后台：`http://localhost:5173`
- 微信开发者工具导入目录：`miniapp/dist/dev/mp-weixin`

本地开发脚本会创建以下管理账号，且仅用于本机开发：

- 用户名：`admin`
- 密码：`LocalAdmin123!`

可以先执行 `./start-dev.ps1 -DryRun` 查看命令而不启动服务。

## 本地微信登录联调

本地启动默认启用微信模拟模式。先在管理后台创建企业和首个管理员，再在小程序登录页输入该管理员手机号完成绑定。模拟模式不会调用微信正式接口。

真机或正式环境必须配置：

- `WECHAT_APP_ID`
- `WECHAT_APP_SECRET`
- `WECHAT_MOCK_ENABLED=false`
- 小程序构建变量 `VITE_API_BASE_URL=https://已配置的合法域名/api`

模拟器联调可在微信开发者工具中关闭域名校验。真机联调需把 `VITE_API_BASE_URL` 改为同一局域网内可访问的电脑地址；真机不能使用 `127.0.0.1` 访问电脑上的后端。正式接口域名必须是微信公众平台已配置的 HTTPS 合法域名。

## 分别运行

后端：

```powershell
Set-Location server
mvn test
mvn spring-boot:run
```

管理后台：

```powershell
Set-Location admin-web
npm ci
npm run dev
```

小程序：

```powershell
Set-Location miniapp
npm ci
npm run dev:mp-weixin
```

生产构建目录为 `miniapp/dist/build/mp-weixin`。

## Compose 部署

```powershell
docker compose -f deploy/docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.yml up --build -d
docker compose -f deploy/docker-compose.yml ps
```

生产环境通过环境变量注入数据库、管理员、微信和域名配置，不要提交真实密钥。
