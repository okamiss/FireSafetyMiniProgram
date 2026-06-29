# AGENTS.md

本文件适用于整个 `FireSafetyMiniProgram` 仓库，供在本项目中工作的编码代理和开发者使用。目标是让每次修改都基于真实仓库、保持业务边界清晰，并能通过可重复的验证完成交付。

## 1. 指令优先级与基本原则

- 用户在当前任务中的明确要求优先于本文件。
- 如子目录以后出现更具体的 `AGENTS.md`，其规则只覆盖对应子目录，并优先于本文件中的通用规则。
- 技术版本、脚本和运行行为以当前代码、`pom.xml`、`package.json`、锁文件及部署配置为准；业务范围以已确认的需求、范围和验收文档为准。
- 文档与代码冲突时，不要自行猜测。先指出冲突，再按用户确认的口径修改。
- 先检查真实代码和配置，再作结论。不要把规划中的模块描述成已经实现，也不要用通用经验替代仓库证据。
- 采用最小、兼容、可验证的改动。不要顺手重构无关模块或扩大业务范围。

## 2. 项目定位和当前状态

本项目是“企业消防微信小程序 + Web 管理后台 + REST API”的一次性交付项目。仓库目前已经搭建三端工程框架和部分示例页面、接口，但不代表需求文档中的全部业务模块已经完成。

标准交付范围包括：

- 组织、企业和用户管理。
- 账号准入及权限开通工单。
- 消防报修工单。
- 消防培训题库、任务和答题记录。
- 培训证书。
- 站内消息和微信订阅消息。
- 简易数据看板。
- Excel 导入导出。
- 操作日志。

本项目按一次性交付口径管理范围。不要引入“二期再做”的默认表述；未确认能力应标记为“本次做、不做或待确认”。

## 3. 需求与技术事实来源

按以下顺序理解项目：

1. 当前用户指令和已确认的变更范围。
2. `PRD/09-甲方一次性交付需求确认与费用清单.docx`：当前一次性交付确认口径。
3. `PRD/05-项目范围确认书.docx`、`PRD/01-项目建设方案.docx`：范围、费用和总体方案。
4. `PRD/08-验收测试用例.docx`：验收场景和结果要求。
5. `PRD/02` 至 `PRD/04`、`PRD/06`、`PRD/07` 系列文档：业务规则、产品体验、合规安全、上线配合和操作说明。
6. `PRD/11-需求详细说明与扩展分析.docx`：用于识别扩展项，不表示所有内容已纳入标准范围。
7. `README.md`、各模块清单和源码：当前可运行能力、版本与命令。

`PRD` 目录中的 Word 和 Markdown 文档属于需求及交付依据。修改前先确认用途和当前版本，保留版式；不要因为文件名相近就覆盖较新的甲方确认稿。

## 4. 仓库架构

| 路径 | 职责 | 主要约束 |
| --- | --- | --- |
| `server` | Spring Boot REST API、领域逻辑、持久化、鉴权和后端测试 | 所有安全校验和企业数据隔离必须在服务端执行 |
| `admin-web` | 管理后台、运营和审核页面 | 使用共享 HTTP 客户端、路由和 Element Plus，不复制鉴权逻辑 |
| `miniapp` | 微信小程序端用户流程 | 使用 uni-app API，适配微信权限、生命周期和审核约束 |
| `deploy` | Docker Compose 和 Nginx 部署 | 配置不得包含真实生产密钥；变更后验证 Compose 和反向代理 |
| `docs` | 技术设计、实施计划和项目辅助文档 | 行为或命令变化时同步维护 |
| `PRD` | 需求、范围、费用、验收和甲方确认材料 | 是业务边界依据，不作为随意重写的代码注释 |

当前请求链路：

- 后端监听 `8080`，API 路径使用 `/api`；健康检查为 `GET /api/health`。
- 管理后台开发服务器监听 `5173`，Vite 将 `/api` 代理到 `http://localhost:8080`。
- 小程序的 `miniapp/src/api/http.ts` 目前仍使用 `https://example.com/api` 占位地址；真实联调前必须改为按环境配置的合法 HTTPS 域名，不能把占位地址当成可用配置。
- Compose 提供 MySQL 8.4、Redis 7.4、后端和管理后台。MySQL 数据使用命名卷持久化。
- Redis 当前只出现在部署配置中，`server/pom.xml` 尚未声明对应 Spring Data Redis 依赖。不要声称缓存或会话已经接入，也不要为了“用上 Redis”擅自增加业务复杂度。

## 5. 当前技术栈

### 后端

- Java 21。
- Spring Boot 3.5.3。
- Spring Web、Validation、Spring Data JPA、Actuator。
- MySQL 运行时数据库，H2 测试数据库。
- Maven 构建，JUnit 5、Spring Boot Test 和 MockMvc 测试。

### 管理后台

- Vue 3.5、TypeScript 5.9、Vite 7。
- Element Plus、Pinia、Vue Router、Axios、ECharts。
- 严格 TypeScript 配置。

### 微信小程序

- uni-app 3、Vue 3.5、TypeScript 5.9、Pinia。
- Vite 5 和微信小程序构建目标。

### 部署

- Docker Compose、Nginx、MySQL 8.4、Redis 7.4。
- 生产环境目标为 HTTPS；微信小程序接口域名还需要在微信公众平台配置。

## 6. 修改前工作流

开始修改前：

1. 运行 `git status --short`，确认是否有用户未提交的工作。
2. 使用 `rg --files`、`rg` 和相邻源码定位真实实现，不根据文件名猜测。
3. 阅读目标模块的清单、配置、测试和调用方。
4. 明确任务属于后端、管理后台、小程序、部署还是文档，以及是否会改变跨端 API 合同。
5. 选择能解决问题的最小边界；未经要求不要改动数据库、接口、三端 UI 和部署的全部层级。

实施过程中：

- 遵循目标目录的现有命名、缩进和组件组织方式。
- 修复根因，不用隐藏错误、虚假成功响应或无条件兜底掩盖问题。
- 保持兼容性；必须破坏兼容时，先说明迁移影响并取得确认。
- 发现需求含糊时，优先引用具体文档条目、接口或测试场景提出问题。
- 不修改或删除与当前任务无关的用户变更。

## 7. Context7 文档规则

当任务涉及任何库、框架、SDK、API、CLI 工具或云服务的用法、配置、版本迁移、调试或安装时，即使是 React、Vue、Spring Boot、Prisma、Tailwind 等常见技术，也必须先通过 Context7 查询当前文档，优先于普通网页搜索。

执行顺序：

1. 除非用户已经给出 `/org/project` 格式的准确库 ID，否则先调用 `resolve-library-id`，参数应包含库名和用户的完整问题。
2. 按名称匹配度、问题相关性、代码示例数量、来源信誉和评分选择最佳 `/org/project`；版本明确时优先选择对应版本。结果不准确时，换用正式名称或改写问题再次解析。
3. 使用选定库 ID 调用 `query-docs`，查询内容必须是用户的完整问题，不要只传单个关键词。
4. 回答和实现以获取到的文档为依据；如文档与项目锁定版本不同，明确说明版本差异。

以下任务通常不需要 Context7：纯业务逻辑调试、与库无关的重构、从零编写简单脚本、一般代码审查和通用编程概念解释。

## 8. 源文件与生成文件

- `admin-web` 中的 `.ts`、`.vue` 是主要源码。当前仓库还跟踪了一批同名 `.js`、`.vue.js`、`vite.config.js` 和 `tsconfig.tsbuildinfo`，它们属于 TypeScript 构建产生的文件，不作为手工修改入口。
- 构建如果改写上述生成文件，不要把无关生成差异混入提交；处理前先确认文件中没有用户手工变更。
- `miniapp/dist`、`admin-web/dist`、`server/target`、`node_modules`、日志和本地环境文件不得提交。
- 优先使用已有资源、组件和 API 封装，不复制一套近似实现。
- 不通过脚本进行未经审阅的大范围机械改写。

## 9. 后端开发规范

- Java 包根为 `com.firesafety.platform`。按业务能力组织包；只有真正跨域的响应、错误、安全和工具能力才放入 `common`。
- REST 路径统一以 `/api` 开头，资源使用清晰、稳定的名词。不要让前端直接依赖数据库实体结构。
- 成功响应沿用 `ApiResponse<T>` 统一结构；新增错误处理时也应提供稳定的错误码、消息和可追踪信息，不能只返回裸字符串或堆栈。
- 输入使用明确 DTO 和 Bean Validation；在服务端校验长度、枚举、状态、文件类型和业务前置条件。
- 认证、角色权限、企业归属和数据范围必须由后端强制执行。前端隐藏按钮不是权限控制。
- 所有按企业访问的数据都必须带企业边界查询和授权校验，避免通过 ID 猜测访问其他企业数据。
- 权限工单、报修工单、培训任务和证书等流程使用显式状态和合法迁移；不要用任意字符串或在多个控制器中复制状态判断。
- 多步写操作使用清晰的事务边界。通知、日志等副作用失败时要定义是否回滚，不能产生半完成状态。
- 业务时间统一考虑 `Asia/Shanghai`。存储、序列化和展示的时区策略应一致，不依赖服务器默认时区碰巧正确。
- 当前 JPA 配置为 `ddl-auto: validate`。不要为方便开发静默改成 `create`、`create-drop` 或 `update`。当前仓库尚无数据库迁移工具；涉及表结构时，应先明确并引入可版本化的迁移方案，同时更新部署文档。
- 新增或修改领域行为时增加相应测试。Controller 合同可使用 MockMvc；服务层状态、权限和企业隔离应有针对性测试。

## 10. 管理后台开发规范

- 使用 Vue 单文件组件和 TypeScript；保持严格类型，避免无理由的 `any`、非空断言和重复接口类型。
- 请求通过 `admin-web/src/api/http.ts` 中的 Axios 实例或同一 API 层扩展，保持 `/api` 基础路径、超时、Token 和统一错误处理。
- Token 键当前为 `admin_token`。修改认证方案时同时检查拦截器、登录状态、路由守卫和退出流程。
- 页面路由统一在 `src/router` 管理，标题等页面信息放在路由 `meta`；不要在多个组件中复制菜单和路径常量。
- 跨页面状态使用 Pinia；仅组件内部使用的状态保留在组件内，不把所有数据塞入全局 Store。
- 使用 Element Plus 现有组件和反馈模式。用户可见文本默认使用中文，错误提示要可操作，不直接展示后端堆栈。
- 列表、表单和看板必须明确加载、空数据、错误、无权限和提交中状态。
- API 枚举、分页和日期类型应与后端合同一致；改变合同必须同步修改调用方。

## 11. 微信小程序开发规范

- 使用 uni-app、Vue 和 TypeScript 源码，不直接编辑构建产物。
- 网络请求集中在 `miniapp/src/api`，会话状态集中在 `miniapp/src/stores/session.ts` 或职责清晰的 Store；不要在页面中重复拼接域名、Token 和错误处理。
- 当前 Token 键为 `miniapp_token`。会话恢复、失效、重新登录和退出应保持一致。
- 接口地址按开发、测试和生产环境配置；生产必须使用微信后台已配置的 HTTPS 合法域名。
- 优先使用 `uni.*` API，只有微信平台专属能力才使用条件编译或平台 API，并为非微信构建提供明确边界。
- 调整页面时同步检查 `pages.json`、导航、分包和授权流程。
- 订阅消息必须由用户授权并受微信平台规则限制；小程序界面不能暗示通知一定送达。
- 在弱网、重复点击、页面返回和小程序切后台场景下，关键写操作应避免重复提交，并给出可恢复的反馈。

## 12. API、安全与数据规范

- 管理后台和小程序共享同一后端业务事实。状态枚举、字段含义和权限规则不得在两个客户端分别发明。
- 重要写操作应记录操作者、企业、动作、对象、时间和结果；日志中不要保存密码、Token、身份证号等敏感明文。
- 上传文件需验证大小、类型和内容风险，使用不可猜测的存储标识；下载和导出同样需要权限及企业隔离。
- Excel 导入应支持行级校验和可读错误反馈，避免部分成功却没有明细；导出需防止公式注入和越权数据泄漏。
- 不提交真实 AppID、AppSecret、数据库密码、令牌、证书、生产域名私钥或包含个人信息的测试数据。
- 配置通过环境变量注入；示例值必须显然是本地或占位值。
- 对外错误信息避免泄露 SQL、堆栈、内部路径和第三方密钥。

## 13. 业务边界与不变量

### 组织、用户与权限

- 用户、企业、角色和可操作数据范围必须明确绑定。
- 权限开通属于可审核工单，不应直接通过客户端改角色。
- 审批、驳回、撤回和关闭等动作必须记录原因、操作者和时间，并限制合法状态迁移。

### 消防报修

- 报修人、所属企业、问题描述、现场材料、受理或派单对象、处理过程和完成结果需要可追踪。
- 状态机在后端保持单一来源；列表筛选、按钮权限和统计口径必须使用相同状态定义。
- 未确认派单主体、处理时限或验收规则时，不自行固化业务假设。

### 消防培训与证书

- 题库、任务、作答记录、判分规则、通过条件和允许作答次数必须可追溯。
- 成绩计算要可重复，不能因页面刷新或重试产生不同结果。
- 证书必须基于已确认的通过记录和模板生成；模板字段、编号规则和有效期未确认时不得自行承诺。

### 消息通知

- 站内消息是业务通知的可查询记录，微信订阅消息是受平台限制的外部提醒。
- 微信发送失败、用户未授权或平台拒绝时，应保留站内消息或可追踪状态，不能把外部发送当作业务事务成功的唯一条件。
- 消息发送必须幂等，避免重试产生重复骚扰。

### 看板、导入导出与日志

- 看板指标必须定义统计口径、企业范围和时间范围，不在前端临时拼接互相矛盾的数字。
- 导入导出只能覆盖授权范围，并记录执行人和结果。
- 操作日志用于审计，不允许普通业务用户任意修改或删除。

## 14. 未经确认不得扩展的范围

以下能力来自扩展分析或外部设想，不因技术上可实现就自动纳入标准交付：

- 消防巡检、设备台账和隐患随手拍。
- HR、OA、维保系统、企业微信或其他第三方系统集成。
- IoT 设备、实时告警或硬件数据接入。
- 未在一次性交付确认稿中勾选的高级报表、复杂工作流和其他定制能力。

如用户要求新增上述能力，先明确范围、接口依赖、数据权属、验收口径和成本影响，再修改代码及交付文档。

## 15. 外部依赖与不可承诺项

- 微信订阅消息可以接入，但送达受用户授权、模板、频率和微信平台规则影响，不能承诺百分之百必达。
- 微信小程序审核、备案、域名配置和上架时间依赖甲方资料及平台审核，不能承诺完全可控的日期。
- 第三方系统对接依赖对方文档、权限、测试环境、数据质量和稳定性；在获得真实接口前只能做明确标注的模拟实现。
- “4G 首屏 3 秒”等性能指标应作为有测试条件的优化目标。必须写明设备、网络、数据量和测量方法，不作无条件绝对承诺。
- 对甲方表述应区分“系统能力可实现”和“外部结果可保证”。

## 16. 测试与验证

验证与改动范围相匹配。没有运行的检查必须明确说“未运行”及原因，不能写成已通过。

### 后端

仓库没有 Maven Wrapper。README 使用本机临时 Maven 3.9.11 路径：

```powershell
$mavenDir = Join-Path $env:TEMP 'apache-maven-3.9.11'
& (Join-Path $mavenDir 'bin\mvn.cmd') test
& (Join-Path $mavenDir 'bin\mvn.cmd') spring-boot:run
```

如该路径不存在，可使用已安装且兼容的 `mvn`。启动后检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

预期响应包含 `status = "ok"`。

### 管理后台

```powershell
Set-Location admin-web
npm ci
npm run build
```

当前没有独立的前端测试脚本；不要声称 `npm test` 已通过。页面行为变更除构建外还应人工或浏览器验证对应路由、请求和错误状态。

### 微信小程序

```powershell
Set-Location miniapp
npm ci
npm run build:mp-weixin
```

构建后使用微信开发者工具导入 `miniapp/dist/build/mp-weixin`。涉及授权、订阅消息、登录或真机网络行为时，需要微信开发者工具或真机验证，单纯构建成功不足以证明流程可用。

### 部署配置

```powershell
docker compose -f deploy/docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.yml up --build -d
docker compose -f deploy/docker-compose.yml ps
```

只有任务涉及部署或跨服务联调时才需要启动完整 Compose；不要为文档或单元级改动无意义地重建所有服务。

### 最低验证矩阵

| 改动范围 | 最低验证 |
| --- | --- |
| 仅文档 | `git diff --check`，核对链接、路径和命令 |
| 后端 Java | Maven 相关测试；接口改动再验证响应合同 |
| 管理后台 | `npm run build`，并验证受影响页面 |
| 微信小程序 | `npm run build:mp-weixin`，平台能力变更再用开发者工具或真机验证 |
| Compose/Nginx | `docker compose ... config --quiet`，必要时启动并检查健康状态 |
| 跨端业务流程 | 后端测试 + 两端相关构建 + 从用户入口到持久化结果的端到端检查 |

## 17. Git 与文件安全

- 保持提交聚焦，一个提交只包含一个清晰目的。
- 不使用 `git reset --hard`、强制覆盖、批量删除等方式处理不属于当前任务的改动。
- 不提交构建目录、依赖目录、临时文件、本地 IDE 配置或秘密配置。
- 提交前运行 `git diff --check`、查看 `git diff` 和 `git status --short`。
- 文档任务不要顺便格式化业务源码；功能任务不要无理由重写 Word 交付材料。

## 18. 完成标准

只有满足以下条件，才可以宣称任务完成：

- 实现与用户确认的范围一致，没有擅自增加扩展业务。
- 相关代码、配置、API 合同和文档保持同步。
- 已运行与改动范围对应的测试或构建，并基于实际输出报告结果。
- 未运行或受环境限制的验证已明确列出。
- 安全、权限、企业隔离、状态迁移和审计要求没有被客户端绕过。
- `git diff` 中没有无关修改、生成物、密钥或个人数据。
- 用户可见行为、部署方式或验收口径发生变化时，已同步更新对应说明和验收材料。


## CodeGraph (Required)

This repository uses CodeGraph.

Before performing code analysis, debugging, refactoring, or feature implementation:

- Use CodeGraph MCP tools as the primary navigation mechanism.
- Use CodeGraph to locate symbols, references, callers, callees, dependencies, and impact scope.
- Use CodeGraph to understand architecture before reading files.
- Determine affected code paths before making changes.

Do NOT start with:

- grep
- ripgrep
- find
- global text search
- reading large portions of the repository

Read source files only after the relevant locations have been identified through CodeGraph.

If CodeGraph is not initialized:

```bash
codegraph init -i
```

Preferred workflow:

1. Analyze repository structure with CodeGraph.
2. Locate relevant symbols and references.
3. Trace dependencies and call chains.
4. Determine impact scope.
5. Read only necessary files.
6. Implement changes.
7. Verify changes.
