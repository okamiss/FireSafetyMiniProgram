# 企业消防小程序 + 管理后台

本仓库按标准方案搭建三端开发框架：

- `server`: Spring Boot 3.5.x 后端 REST API
- `admin-web`: Vue 3 + Element Plus 管理后台
- `miniapp`: uni-app 微信小程序
- `deploy`: Docker Compose 与 Nginx 部署配置

## 本地开发

首次运行前，请先安装各端依赖，并确保 Java 21、Maven、Node.js、npm、Docker 与 Docker Compose 可用。仓库根目录提供统一的本地开发脚本：

```powershell
.\start-dev.ps1
.\status-dev.ps1
.\stop-dev.ps1
```

`start-dev.ps1` 会通过 Docker Compose 启动 MySQL 和 Redis，并在独立 PowerShell 窗口中启动缺失的后端、管理后台和小程序开发进程。可先使用 `.\start-dev.ps1 -DryRun` 查看计划执行的命令；该模式不会修改容器或打开窗口。`stop-dev.ps1` 只停止启动器记录的进程，不会结束启动脚本前已占用 `8080` 或 `5173` 端口的进程，MySQL 持久化卷也会保留。

启动后可访问：

```text
后端健康检查：http://localhost:8080/api/health
管理后台：http://localhost:5173
```

小程序开发进程将接口地址设置为 `http://127.0.0.1:8080/api`。使用微信开发者工具导入：

```text
miniapp/dist/dev/mp-weixin
```

本机模拟器联调时，需要在微信开发者工具中关闭“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”的域名校验。真机不能使用 `127.0.0.1` 访问电脑服务；真机联调需改用同一局域网内可访问的电脑地址，正式环境必须使用已在微信公众平台配置的 HTTPS 合法域名。

仍可按需分别进入 `server`、`admin-web` 或 `miniapp` 目录运行原有 Maven/npm 命令。生产构建的小程序导入目录仍为 `miniapp/dist/build/mp-weixin`。

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
