# 企业消防小程序与管理后台

一次性交付项目，包含 Spring Boot REST API、Vue 管理后台、uni-app 微信小程序和 Docker Compose 基础设施。

## 当前进度

已完成工程基础、身份权限、消防报修和消防培训里程碑：

- MySQL Flyway 迁移、JPA 持久化、Redis 不透明会话和本地文件存储。
- 平台管理账号登录、会话刷新、退出和鉴权。
- 微信登录、手机号授权绑定和已开通账号校验。
- 企业及首个企业管理员创建、企业用户查看与停用。
- 企业管理员提交员工权限申请，平台审核通过或驳回。
- 审核结果写入站内消息表。
- 企业用户提交报修并上传最多 6 张现场照片，按本人或企业数据范围查看工单。
- 平台后台受理、完成和关闭报修，保留完整流转记录并写入站内消息。
- 报修照片使用受保护文件接口读取，服务端校验类型、内容、大小和数据权限。
- 平台维护单选、多选和判断题题库，支持下载标准 `.xlsx` 模板及整批校验导入。
- 平台创建培训任务，按企业或指定人员生成参训名单并发布；默认支持配置及格分和最多答题次数。
- 小程序完成在线答题，多选题必须完全匹配才得分，系统保存每次答案、成绩和最高分。
- 培训通过后自动生成带编号的 PDF 证书，小程序和管理后台均通过受保护接口查看。
- 管理后台可查询培训任务、答题记录和证书，答题记录包含任务、人员、企业、次数和成绩。
- 权限审核、报修状态和培训任务统一写入站内消息；小程序支持全部/未读查看和已读状态。
- 微信订阅消息在业务事务提交后独立发送，未配置、未授权或平台拒绝均保留可审计状态，不影响主业务结果。
- 管理后台可按消息类型和微信发送状态查询通知记录。

看板、通用导入导出和操作日志查询仍按一次性交付计划继续开发。

## 消防培训说明

- 题库导入只接受 `.xlsx`，单次文件不超过 5MB、最多 1000 道；任一行校验失败则整批不写入，并返回具体行号和原因。
- 导入表头为：题型、题干、选项A、选项B、选项C、选项D、正确答案、分值、分类、解析。建议直接在管理后台题库页下载模板。
- 判断题答案支持“正确/错误”“对/错”或 `TRUE/FALSE`；多选答案使用逗号、中文逗号或分号分隔。
- 证书使用项目内嵌的 Noto Sans CJK 字体生成，默认编号前缀为 `FS`，可通过 `app.certificate` 配置调整前缀和签发单位。

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
- `WECHAT_SUBSCRIBE_ENABLED=true`
- `WECHAT_PERMISSION_TEMPLATE_ID`、`WECHAT_REPAIR_TEMPLATE_ID`、`WECHAT_TRAINING_TEMPLATE_ID`
- 小程序构建变量 `VITE_API_BASE_URL=https://已配置的合法域名/api`

订阅模板的字段键默认使用 `thing1`、`thing2`、`time3`。若甲方申请的模板字段不同，分别通过
`WECHAT_SUBSCRIBE_TITLE_KEY`、`WECHAT_SUBSCRIBE_CONTENT_KEY`、`WECHAT_SUBSCRIBE_TIME_KEY` 配置；
体验版联调还需设置 `WECHAT_MINIPROGRAM_STATE=trial`。微信消息送达依赖用户授权、模板审核和平台规则，
系统只保证站内消息可查询，并记录微信发送结果，不承诺百分之百送达。

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
