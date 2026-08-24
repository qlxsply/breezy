# web-admin 结构性重构方案与任务清单

## 1. 文档状态

- 需求名称：`web-admin` 高可用、可维护、可扩展结构性重构
- 当前状态：已完成
- 实施状态：T1-T12 已完成
- 主要范围：`web-admin/`
- 关联范围：认证 Cookie 契约、Nginx 同源代理和部署配置涉及必要的后端及部署调整
- 实施原则：不保留旧结构兼容层，不引入临时转发文件，不允许新旧实现长期并存

## 2. 需求背景

`web-admin` 已完成 React 19 + Next.js 16 技术栈迁移，但当前实现较多保留了原 SPA 的组织方式：页面集中在 `components/admin-pages`，API 和类型按技术角色集中，后台路由通过 catch-all 页面和客户端 `pageMap` 再次分发，业务 CSS 全量进入全局作用域，认证与请求生命周期分散在多个 registry 和旁路请求中。

该结构可以继续开发，但已经产生以下系统性风险：

- 后台所有页面进入同一客户端模块图，路由级代码分割基本失效。
- 新增功能需要同时修改页面、API、类型、样式和路由等多个顶层目录。
- 网络 401 不能完整结束前端会话，SSE、通知和权限状态可能继续存活。
- Bootstrap URL 可由浏览器查询参数覆盖，并可能向非信任地址附加认证信息。
- 全局 CSS、超大页面、超大 registry 和重复分页逻辑持续提高回归概率。
- 包管理、测试、类型边界和 CI 门禁不足以支撑结构继续扩展。

本项目仍处于新项目阶段，本次重构不考虑旧前端目录、旧 Token 存储方式、旧客户端路由分发方式和旧样式选择器的兼容。

## 3. 当前现状

### 3.1 工程组织

- `src` 主要按 `api / types / core / components / styles` 技术角色划分。
- 页面集中在 `src/components/admin-pages`，业务组件又分散在多个 `*-admin` 目录。
- `src/components` 同时包含纯 UI、后台通用组件、业务页面、运行时和路由壳体。
- `ConfigManageDrawer.tsx`、`ApisAdminPage.tsx`、`DictAdminPage.tsx`、`AdminShell.tsx` 等文件体积过大。

### 3.2 路由与权限

- 后台业务页面统一进入 `/admin/[...slug]`。
- `page-map.tsx` 同步导入全部后台页面。
- 路由分别维护在 `staticAdminRoutes`、`pageMap`、`KNOWN_ADMIN_SLUGS` 中。
- 页面访问守卫集中，但按钮权限码散落在多个页面中。

### 3.3 请求、认证和运行时

- 普通 API 使用统一 fetch 封装，但文件下载、预览和 Bootstrap 请求存在旁路。
- 401 只清理持久化 Token，不清理内存会话、不跳转登录页，也不停止运行时连接。
- Token 存储于 `localStorage`。
- Bootstrap 地址可由查询参数、localStorage 和 window 全局变量覆盖。
- registry 同时承担 Store、API、数据转换、SSE、Service Worker 和跨标签页协调。

### 3.4 类型、表格和样式

- `types` 混合业务模型、API DTO、UI 状态和组件行为类型。
- HTTP JSON 通过泛型断言进入业务代码，缺少运行时信封校验。
- 多个列表页重复维护分页、筛选、loading 和请求生命周期。
- 业务样式全部进入全局作用域，没有 CSS Modules 或稳定的层级隔离。

### 3.5 构建和依赖

- Next 依赖及传递依赖存在高危安全公告。
- ESLint 配置直接使用未声明的 `globals`。
- `lightningcss` 和 `eslint-plugin-promise` 没有产生有效工程能力。
- 静态导出模式仍保留无效的 `next start`。
- 缺少 `preview`、`typecheck`、`test` 和完整 CI 门禁。

## 4. 重构目标

### 4.1 结构目标

- 按业务功能组织页面、API、类型、状态和业务组件。
- 纯 UI、通用后台组件和业务功能具有明确、可自动检查的依赖方向。
- 删除顶层 `src/api`、`src/types`、`src/components/admin-pages` 等技术大仓库。
- 删除无明确边界的 `src/core`，将能力归入 `shared`、`features` 或 `runtime`。

### 4.2 路由目标

- 每个后台页面使用真实 App Router 物理路由。
- 删除 catch-all 客户端页面分发器、`pageMap` 和手工 slug 清单。
- 登录守卫、账号类型守卫和页面权限守卫集中在受保护布局及统一权限模型中。
- Next 自动路由分包可以正常生效，页面按需加载。

### 4.3 安全和高可用目标

- 浏览器不能通过 URL、localStorage 或任意全局变量修改带认证请求的目标 Origin。
- 管理后台认证改为安全 Cookie 会话，不再将长期 Bearer Token 保存到 localStorage。
- 所有 HTTP、Blob、Bootstrap 请求通过统一 Transport。
- 401 只触发一次完整会话结束流程，并停止 SSE、Push、通知和权限运行时。
- 分页和筛选请求具备取消或过期响应丢弃能力，旧请求不能覆盖新结果。

### 4.4 可维护和扩展目标

- 新增一个业务功能时，绝大多数变更只发生在一个 `features/<feature>` 目录及一个 App Router 页面入口内。
- API Payload、业务模型和 UI 状态具有明确所有权。
- 普通后台列表统一使用分页查询 Hook 和分页栏组件。
- 页面样式使用 CSS Modules；全局 CSS 仅保留 tokens、reset、字体和少量明确 utility。
- 包管理、类型检查、测试、构建和安全审计形成可执行门禁。

## 5. 不兼容重构原则

以下规则适用于全部实施任务：

1. 不新增旧路径到新路径的 re-export 转发文件。
2. 不新增旧 API 与新 API 双写或双读逻辑。
3. 不保留 localStorage Token 作为 Cookie 认证失败时的回退。
4. 不保留查询参数、localStorage 或 window 对 Bootstrap Origin 的覆盖能力。
5. 不保留 catch-all 路由作为真实 App Router 路由的兜底。
6. 不为旧 CSS 类名增加映射层；组件迁移完成时直接删除旧选择器。
7. 每个文件迁移任务必须同步修改全部引用并删除旧文件。
8. 每个任务结束时必须能够独立编译、运行和验证。
9. 每次只执行一个任务，完成后暂停并等待用户确认。

## 6. 目标架构

### 6.1 目标目录

```text
web-admin/src/
├─ app/                              # Next App Router、布局、错误边界、路由入口
│  └─ admin/
│     ├─ login/
│     └─ (protected)/
│        ├─ layout.tsx
│        ├─ users/page.tsx
│        ├─ roles/page.tsx
│        ├─ resources/page.tsx
│        └─ ...
├─ features/                         # 业务垂直切片
│  ├─ auth/
│  │  ├─ api/
│  │  ├─ model/
│  │  └─ ui/
│  ├─ users/
│  │  ├─ api/
│  │  ├─ model/
│  │  ├─ ui/
│  │  └─ UsersPage.tsx
│  ├─ roles/
│  ├─ resources/
│  ├─ configs/
│  └─ ...
├─ shared/                           # 不包含业务语义的稳定公共能力
│  ├─ config/
│  ├─ hooks/
│  ├─ http/
│  ├─ lib/
│  ├─ types/
│  └─ ui/
│     ├─ bz/
│     └─ admin/
├─ runtime/                          # 应用组合根、SSE、Push、Service Worker、反馈桥接
└─ styles/                           # 仅全局 tokens、reset、字体、明确 utility
```

### 6.2 依赖方向

```text
app -> features -> shared
app -> runtime -> features/shared
```

强制规则：

- `shared` 禁止依赖 `features`、`app`、`runtime`。
- `features` 禁止依赖 `app`。
- 不同 feature 默认禁止直接互相引用；共享契约下沉到 `shared`，跨领域用例由 `app` 或明确的应用服务编排。
- `shared/ui/bz` 禁止读取认证、权限、业务 API 和业务类型。
- `shared/ui/admin` 可以依赖 Bz UI，但禁止调用业务 API。
- 仅 `shared/http` 可以执行普通 HTTP fetch。
- EventSource、Service Worker、Notification 等浏览器基础设施仅允许出现在 `runtime`。
- 跨顶层目录统一使用 `@admin/*`；同目录内部允许 `./`，禁止两层及以上父级相对引用。

### 6.3 路由模型

- 后台每个页面建立真实 `page.tsx`。
- `/admin/(protected)/layout.tsx` 负责登录态和账号类型边界。
- 页面资源权限由统一 `AdminRoutePolicy` 声明和检查。
- 后端资源树继续驱动菜单可见性，但只能指向编译期存在的物理路由。
- 未注册或无权限路由直接进入标准 404/403 边界，不通过客户端占位页面兼容。
- 页面按钮权限在对应 feature 的 `permissions.ts` 中集中声明，通过响应式 Hook 使用。

### 6.4 HTTP 与认证模型

- 管理后台采用同源安全 Cookie 会话，Cookie 使用 `HttpOnly`、`Secure`、合适的 `SameSite` 策略。
- 前端不读取、不保存长期认证 Token。
- 所有普通 API 默认使用 `credentials: "include"`。
- 写请求按最终认证方案统一增加 CSRF 防护，不由页面自行处理。
- Transport 支持 `envelope / json / blob / response` 响应模式。
- Transport 支持 `AbortSignal`、超时、统一 Header、结构化 `HttpError` 和 `ApiError`。
- Transport 不直接弹业务 Toast；反馈由用例层或统一查询层决定，避免重复提示。
- 401 进入唯一的 `endSession()`，清空内存状态并停止所有运行时连接。
- API 响应先按 `unknown` 处理，验证响应信封和关键 Payload 后再转换为业务模型。

### 6.5 运行时配置和部署模型

- 静态镜像采用单一公开运行时配置文件 `/runtime-config.json`，应用在认证和资源初始化前获取并验证。
- 运行时配置只允许公开 URL、路径和开关，不允许密钥、Token 或密码。
- API 和 Bootstrap 默认使用同源路径；如确需跨域，只允许部署时配置的 HTTPS Origin 白名单。
- 浏览器查询参数、localStorage 和任意 window 配置不再参与服务地址选择。
- Nginx 明确支持 Next 静态导出路径、运行时配置、同源 API 代理和安全响应头。
- Docker 构建使用 `.dockerignore`，镜像使用固定受支持版本、非 root 用户和健康检查。

### 6.6 状态和运行时模型

- Store 只保存跨组件共享状态和轻量 selector。
- API 编排、数据转换和浏览器生命周期从 Store 中移除。
- 认证、资源、通知等功能各自拥有 model 和 service。
- SSE、Web Push、Service Worker、跨标签页选主放入 `runtime`。
- 页面筛选、抽屉、表单和局部选择状态继续由组件本地 Hook 管理。
- 稳定字典数据通过统一缓存 Hook 获取，不由每个页面重复请求。

### 6.7 API 和类型模型

每个 feature 内明确区分：

```text
api/payload.ts       # 服务端原始请求和响应 DTO
api/client.ts        # HTTP 调用及 Payload 转换
model/types.ts       # 前端稳定业务模型
model/query.ts       # 查询、分页和业务状态
ui/*.tsx             # 组件 Props 和局部 UI 类型优先就近定义
```

规则：

- 网络 Payload 不直接复用 UI Entry 类型。
- `JSON.parse`、localStorage、SSE 和 Push 数据均从 `unknown` 开始验证。
- 删除重复的分页、认证和配置类型。
- 禁止显式 `any`，不增加 `ts-ignore`。
- 按真实契约区分 `undefined` 和 `null`，不默认使用 `field?: T | null`。

### 6.8 列表和表单模型

- `useAdminPagedQuery<T, F>` 统一管理查询条件、分页、loading、刷新、过期响应和错误。
- `AdminTablePagination` 统一总数、页大小、页码和有效页回退。
- `AdminSearchForm` 统一查询条件提交、重置和折叠布局。
- `AdminListPageTemplate` 只负责布局，不隐式承载业务请求。
- 简单列表必须使用统一分页抽象；复杂树表、详情表和配置编辑表允许使用专用实现。
- 抽象不接受业务权限码、业务字段名或业务提示文案。

### 6.9 样式模型

- 全局样式仅保留 theme tokens、reset、字体和明确 utility。
- Feature 页面和业务组件使用 `*.module.css`。
- Bz UI 和 Admin UI 的组件样式与组件同目录维护。
- 删除 `.el-*`、`#app` 和其他迁移遗留选择器。
- 禁止宽泛的 `.content`、`.brand`、`.filters`、`.pagination` 全局类名。
- 原则上禁止 `!important`；确需使用时必须由组件边界文档说明原因。

## 7. 问题整改矩阵

| 编号 | 评级     | 问题描述                                           | 问题范围                                                                 | 彻底修复方案                                                         | 修复目标                                  | 对应任务       |
| ---- | -------- | -------------------------------------------------- | ------------------------------------------------------------------------ | -------------------------------------------------------------------- | ----------------------------------------- | -------------- |
| P01  | 警告     | 顶层按 API、类型、页面、样式等技术角色聚合         | `src/api`、`src/types`、`src/components/admin-pages`、`src/styles/admin` | 迁移为 `features/*` 垂直切片，公共能力归 `shared` 和 `runtime`       | 单个功能的页面、API、类型、状态和样式集中 | T4、T7-T9      |
| P02  | 违规     | `src/components` 混合纯 UI、业务页面、运行时和路由 | `src/components/**`                                                      | 将纯 UI 移入 `shared/ui`，业务 UI 移入 feature，运行时移入 `runtime` | 公共 UI 不再依赖业务层                    | T4、T5、T7-T9  |
| P03  | 违规     | catch-all 客户端路由器同步导入全部页面             | `app/admin/(shell)`、`admin-pages/page-map.tsx`                          | 建立真实 App Router 页面并删除 `pageMap`、slug 清单                  | 物理路径与页面一一对应，自动按路由分包    | T10            |
| P04  | 警告     | 通用 Hook 和领域 Hook 缺少统一归属规则             | `core/registry`、`components/admin`、组件文件                            | 通用 Hook 归 `shared/hooks`，领域 Hook 归 feature model              | Hook 位置可由职责直接判断                 | T4、T5         |
| P05  | 合规改进 | 无三层深相对引用，但仍有两层引用                   | 8 个组件、15 处引用                                                      | 跨顶层目录统一 `@admin/*` 并增加 ESLint 限制                         | 两层及以上父级 import 为 0                | T4             |
| P06  | 合规     | `@admin/*` 映射正确                                | `tsconfig.json`、源码 import                                             | 保留别名，增加边界和深相对路径 lint 规则                             | 别名解析和依赖边界均由 CI 验证            | T1、T4         |
| P07  | 警告     | 存在无效或冗余开发依赖                             | `package.json`、ESLint 配置                                              | 删除 `lightningcss` 和无效 promise 插件，直接声明所有实际依赖        | 顶层依赖全部有明确用途                    | T1             |
| P08  | 违规     | Next 及传递依赖存在高危公告                        | `package.json`、`package-lock.json`                                      | 升级到已修复稳定版本并重新审计                                       | 生产依赖 high/critical 为 0               | T1             |
| P09  | 违规     | `globals` 属于幽灵依赖                             | `eslint.config.mjs`、`package.json`                                      | 将 `globals` 加入直接 devDependency                                  | 不依赖 node_modules 提升行为              | T1             |
| P10  | 违规     | 缺少 preview/typecheck/test，start 与静态导出冲突  | `package.json`、`.prettierignore`、Maven                                 | 重建 scripts 和 CI，静态预览与部署模型一致                           | 本地和 CI 使用同一质量命令                | T1、T2、T12    |
| P11  | 合规改进 | API 已抽离，但认证重复且存在请求旁路               | `src/api`、`core/http.ts`、registry、system-files                        | 所有请求接入统一 Transport，API 归属对应 feature                     | 组件和 registry 不直接 fetch 普通 HTTP    | T3、T5、T7-T9  |
| P12  | 警告     | DTO、业务模型、UI 类型混合，外部数据依赖断言       | `src/types`、`src/api`、`core/types.ts`                                  | 按 feature 拆 Payload、Model、UI 类型并增加边界验证                  | 网络响应不再未经验证进入业务模型          | T3、T7-T9      |
| P13  | 警告     | registry 同时承担状态、API 和浏览器生命周期        | `src/core/registry/**`                                                   | Store、Service、Runtime 三层拆分                                     | Store 只保存共享状态和 selector           | T5             |
| P14  | 违规     | 业务 CSS 全局加载并存在迁移遗留                    | 全部 CSS、根 layout                                                      | 使用 CSS Modules，清理遗留和宽泛类名                                 | 全局 CSS 不含 feature 选择器              | T4、T7-T9、T11 |
| P15  | 违规     | Bootstrap 地址可覆盖、Token 可外发、环境注入不可靠 | env、bootstrap、auth-storage、Docker、Nginx                              | 可信运行时配置、同源 Cookie、Origin 校验、安全部署                   | 浏览器输入不能改变认证请求 Origin         | T2、T3         |
| P16  | 违规     | 后台页面无法有效路由分包，首屏体积过大             | catch-all、pageMap、根 Provider、全局 CSS                                | 真实路由、局部 Provider、按 feature 加载                             | 后台页面按需加载并满足体积预算            | T4、T10-T12    |
| P17  | 警告     | 路由清单重复，按钮权限码散落                       | admin-routes、pageMap、后台页面                                          | 单一路由策略、feature 权限常量、响应式 Hook                          | 权限定义有唯一事实来源                    | T5、T7-T10     |
| P18  | 违规     | 列表页重复分页、筛选、loading 和请求生命周期       | 约 11 个后台列表页                                                       | 建立统一分页查询 Hook、查询表单和分页栏                              | 普通列表不再手写请求样板                  | T6、T7-T9      |
| P19  | 违规     | 401 会话结束不完整，请求旁路且不支持取消           | HTTP、auth、bootstrap、system-files、registry                            | 统一 Transport、Cookie 会话、endSession、AbortSignal                 | 401 完整退出且请求无竞态                  | T3、T5、T6     |

## 8. 实施任务

### T1：工具链、依赖和质量命令基线

问题描述：依赖存在高危公告、幽灵依赖和无效依赖；工程命令与静态导出模式不一致。

问题范围：

- `package.json`
- `package-lock.json`
- `eslint.config.mjs`
- `.prettierignore`
- `web-admin/pom.xml`

修复方案：

- 升级 Next、React 及兼容范围内的工具依赖。
- 添加直接依赖 `globals`。
- 删除 `lightningcss` 和无有效规则的 `eslint-plugin-promise`。
- 增加 `typecheck`，将 lint 改为零 warning 门禁。
- 建立 Vitest + Testing Library 单元测试基线及 `test` 命令，为后续结构任务提供即时验证能力。
- 排除 `.next` 等生成目录，收窄 format 范围。
- Maven/CI 安装改为 `npm ci`。
- 删除与静态导出冲突的 `next start`；preview 在 T2 按最终部署模型补齐。

修复目标：

- `npm ls --depth=0` 无 missing、invalid、extraneous。
- `npm audit --omit=dev` 无 high/critical。
- lint、format check、typecheck、test、build 均可独立执行。

验收标准：

- `npm ci`
- `npm run lint`
- `npm run format:check`
- `npm run typecheck`
- `npm run test`
- `npm run build`
- `npm audit --omit=dev`

实施结果（2026-08-20）：

- Next.js 升级到 `16.3.1`，React 和 React DOM 升级到 `19.2.8`。
- 直接声明 `globals`，删除 `lightningcss` 和 `eslint-plugin-promise`。
- Maven 前端依赖安装由 `npm install` 改为 `npm ci`。
- 删除与静态导出冲突的 `next start`，新增 `typecheck`、`test`、`test:watch` 和零 warning lint 门禁。
- 建立 Vitest 4 + Testing Library + jsdom 测试基线，首批 2 个组件测试通过。
- `.prettierignore` 已排除 Next、Maven、TypeScript 缓存和真实环境文件，现有源码格式已建立统一基线。
- `npm ci`、lint、format check、typecheck、test、build、`npm audit --omit=dev` 全部通过。
- `npm audit --omit=dev` 和完整依赖审计均为 0 漏洞。

依赖任务：无。

### T2：可信运行时配置与静态部署重构

问题描述：环境文件未可靠进入静态产物，Bootstrap Origin 可由浏览器输入覆盖，Nginx 与静态路由不匹配。

问题范围：

- `.env*`
- `src/core/env.ts`
- `src/core/bootstrap-config.ts`
- `next.config.ts`
- `nginx.conf`
- `Dockerfile`
- Maven Docker 构建配置

修复方案：

- 建立唯一 `/runtime-config.json`，由部署环境生成，禁止缓存，并在认证和资源初始化前加载及校验。
- 删除查询参数、localStorage 和任意 window Bootstrap 覆盖机制。
- API 与 Bootstrap 强制使用同源相对路径，不向浏览器开放跨域认证请求配置。
- 修复 Next 静态导出与 Nginx 路由匹配，明确 trailing slash 策略。
- 增加 CSP、`nosniff`、Referrer Policy、Permissions Policy 等安全响应头。
- 增加 `.dockerignore`、非 root 运行、健康检查和固定镜像版本策略。
- 停止跟踪真实环境文件，仅保留示例模板。
- 增加与生产 Nginx 一致的 preview 命令。

修复目标：

- 同一静态镜像可通过可信部署配置运行于不同环境。
- 浏览器 URL 和本地存储不能修改认证请求目标。
- 深层后台路由直接访问和刷新均返回正确页面。

验收标准：

- 开发、生产配置加载测试通过。
- 非白名单 Origin 配置被拒绝。
- `/admin`、`/admin/users`、`/admin/profile/password` 直接访问和刷新通过。
- Docker 健康检查通过，响应包含预期安全头。

实施结果（2026-08-20）：

- 删除旧 `env.ts`、`bootstrap-config.ts` 和 `.env.corwin`，不再读取 `NEXT_PUBLIC_*`、查询参数、localStorage 或 window Bootstrap 配置。
- 新增严格校验的 `/runtime-config.json`，仅接受同源 `apiBaseUrl` 和 `bootstrapPath`，未知字段和绝对 URL 直接拒绝。
- 根布局增加 `RuntimeConfigGate`，运行配置完成前不挂载认证、资源和页面运行时。
- Next 开发环境通过服务端 `ADMIN_API_UPSTREAM` 代理 `/api`，浏览器始终只访问同源路径。
- 静态导出启用 trailing slash，新增与生产路由规则一致的 Node preview。
- Docker 改用固定版本 `nginx-unprivileged`，以 UID 101 运行，启动时生成运行配置并提供健康检查。
- Nginx 增加同源 API 代理、CSP、`nosniff`、Referrer Policy、Permissions Policy、禁止嵌入和分层缓存策略。
- `.dockerignore` 和仓库 `.env*` 忽略规则已收口，真实环境文件不再进入版本库或 Docker 上下文。
- 10 个单元测试、lint、format check、typecheck、静态 build、preview 路由及 Docker 容器验收全部通过。

依赖任务：T1。

### T3：认证会话与统一 HTTP Transport 重构

问题描述：Bearer Token 长期保存于 localStorage，401 不结束完整会话，普通 HTTP 存在旁路和重复错误提示。

问题范围：

- `web-admin` 认证、HTTP、文件下载、Bootstrap 请求
- 后端登录、登出、当前用户及必要的 CSRF 契约
- Nginx 同源 API 代理

修复方案：

- 后端改为安全 HttpOnly Cookie 会话，不保留前端 Bearer Token 回退。
- 删除 `auth-storage` 中 Token 读写和所有 localStorage Token 数据。
- 建立统一 Transport，覆盖 envelope、raw JSON、Blob 和原始 Response。
- Transport 支持 `AbortSignal`、超时、结构化错误和统一凭据策略。
- 建立单一 `endSession()`，处理 401、登出、状态清理和登录跳转。
- Transport 不直接弹业务错误；页面和查询层按错误类型决定反馈。
- 网络 JSON 从 `unknown` 开始验证，至少严格验证通用响应信封。

修复目标：

- 前端存储中不存在认证 Token。
- 所有普通 HTTP 请求具有一致的认证、错误、取消和 401 行为。
- 并发 401 只执行一次会话结束和跳转。

验收标准：

- 登录、刷新恢复、登出、Cookie 过期流程通过。
- 401 后认证、资源、通知和连接状态全部清空。
- 文件预览、下载和 Bootstrap 请求遵循同一错误策略。
- 源码中除统一 Transport 外不存在普通 `fetch`。

完成记录（2026-08-20）：

- 后台 opaque session 已迁移到 `__Host-breezy-admin-session` HttpOnly、Secure、SameSite=Strict Cookie；后台登录响应不再向 JavaScript 返回 Token。
- 新增双提交 CSRF Cookie/Header 校验，登录、登出及全部 Cookie 认证写请求统一受保护；外部用户 JWT Bearer 协议保持独立。
- 删除 `auth-storage.ts` 及全部 localStorage Token、Authorization Header 拼装和认证 scope 分支。
- 统一 Transport 已覆盖 envelope、JSON、Blob、Response，支持同源凭据、CSRF、AbortSignal、超时、严格信封校验、`HttpError`、`ApiError` 和并发 401 合并。
- 新增唯一 `endSession()`，统一清理认证、资源、通知、快捷键、Web Push 和 SSE 生命周期并跳转登录页。
- Bootstrap、文件预览和下载已删除直接 `fetch` 旁路；除运行时配置加载和 Transport 自身外不再直接调用普通 HTTP fetch。
- 后台 session 缓存 TTL 已限制为 `min(session 剩余有效期, session-cache-ttl)`，避免缓存认证超过撤销检查窗口。
- 后端认证过滤器测试 3/3、前端测试 14/14、lint、typecheck、生产 build 和后端编译通过。

依赖任务：T2。

### T4：共享层、公共 UI 和依赖边界重构

问题描述：`components` 和 `core` 职责混杂，纯 UI 依赖应用状态，通用 Hook 和工具缺少稳定归属。

问题范围：

- `src/components/bz`
- `src/components/admin`
- `src/components/admin-inputs`
- `src/core` 中纯公共能力
- 跨目录 import

修复方案：

- 创建 `shared/ui/bz`、`shared/ui/admin`、`shared/hooks`、`shared/lib`、`shared/types`。
- 移动公共组件并同步全部引用，迁移后直接删除旧文件。
- 将反馈桥接和用户配置适配从纯 UI 中移出。
- 清理两层相对引用并增加 ESLint 依赖边界规则。
- Bz UI 不再依赖认证、registry、业务 API 或业务类型。
- 公共组件样式迁移到组件同目录 CSS Modules 或明确的 UI 样式边界。

修复目标：

- `shared` 层无业务依赖。
- `src/components` 不再作为混合职责大目录存在。
- 所有跨顶层引用符合统一规则。

验收标准：

- 两层及以上父级 import 为 0。
- 依赖边界 ESLint 规则通过。
- 旧公共组件路径和转发文件为 0。
- 全量 build 通过。

完成记录（2026-08-20）：

- `components/bz`、`components/admin`、`components/admin-inputs` 已分别迁入 `shared/ui/bz`、`shared/ui/admin` 和 `shared/ui/admin/inputs`，旧目录及转发文件全部删除。
- HTTP Transport 与运行时配置迁入 `shared/transport`；所有 API 直接依赖 canonical Transport，`api/http.ts`、`core/http.ts` 等旧路径已删除。
- formatter、storage、message、confirm、store 迁入 `shared/lib`，React Store adapter 和公共布局 Hook 迁入 `shared/hooks`，公共用户配置结构迁入 `shared/types`。
- Bz 反馈桥接移至 runtime，纯 Bz UI 不再依赖认证、registry、业务 API、业务类型或应用级反馈适配。
- 日期时间公共组件改为通过 props 接收 pattern/timeZone；认证用户配置由应用层 `useDateTimePreferences()` 适配，不再由 shared UI 隐式读取。
- Bz 与表格输入样式迁入组件目录下的明确 UI 样式边界；无消费者的旧 `AdminDateTimeField` 已删除。
- ESLint 新增 shared 反向依赖、Bz UI 依赖和旧路径限制；两层及以上父级 import、shared 业务反向依赖、旧公共 import 均为 0。
- 前端测试 14/14、lint、format check、typecheck 和生产 build 通过。

依赖任务：T1。

### T5：全局状态、运行时和权限基础设施重构

问题描述：registry 混合 Store、API、数据转换和浏览器生命周期，权限读取存在命令式和响应式双轨。

问题范围：

- `src/core/registry/**`
- SSE、通知、Web Push、Service Worker、快捷键
- 认证和资源状态
- 权限 selector

修复方案：

- 将每个全局能力拆为 Store、Service、Runtime Adapter。
- Store 只保留状态、action 和 selector。
- API 调用归对应 feature，运行时连接归 `runtime`。
- 提供响应式 `usePermission`、`usePermissions`、`useAuthSession`。
- 资源 Store 预构建 `byId`、`byCode` 索引。
- 删除未使用的平行权限 API 和重复认证类型。
- 401/登出通过 T3 的统一会话结束接口停止所有运行时能力。

修复目标：

- Store 不直接执行普通 HTTP、EventSource、Service Worker 注册。
- 权限和认证状态变更能够可靠触发页面更新。
- 运行时能力有统一启动和停止生命周期。

验收标准：

- registry 旧目录删除。
- 登录、登出、多标签页、SSE 重连和通知流程通过。
- 权限变化后页面和按钮响应式更新。

完成记录（2026-08-20）：

- 认证、资源和通知能力已分别迁入 `features/auth`、`features/resources`、`features/notifications`，并按 API、Model Store、Service 分离职责。
- SSE、Web Push、Service Worker、跨标签页会话同步和统一运行时协调已迁入 `runtime`；`core/registry`、`components/runtime` 和未使用的快捷键 registry 已直接删除。
- 资源 Store 原子维护 `items/byId/byCode/byPath`，加载和错误期间清空旧授权；空权限码、未知资源、禁用资源、断裂父级和循环父级均默认拒绝。
- 页面、按钮和菜单已统一使用响应式 `usePermission`、`usePermissions`、`useAuthSession` 及资源 selector，命令式权限双轨和旧平行权限 API 已删除。
- 管理运行时按认证、资源、通知、SSE、Push 顺序启动，支持 StrictMode 租约、初始化失败重试，并在 401、登出和跨标签页会话结束时统一停止。
- SSE 增加基于租约的多标签页选主、锁变更监听、连接代次、重连定时器失效和 cursor 恢复，避免重复连接与重复消息。
- Push 启停通过串行生命周期队列协调，订阅同步和删除严格排序；Push/CSRF 的 401 采用非阻塞会话终止通知，避免清理流程循环等待。
- 新增 `public/sw.js` 处理 Push 广播、系统通知和同源点击跳转；ESLint 增加 feature、model、runtime、旧路径和浏览器副作用边界。
- 前端测试 14/14、lint、format check、typecheck 和生产 build 通过。

依赖任务：T3、T4。

### T6：后台分页查询与表单基础抽象

问题描述：普通列表页重复维护筛选、分页、loading、刷新和请求竞态。

问题范围：

- `AdminListPageTemplate`
- `BzTable`、`BzPagination`
- 约 11 个分页列表页的公共逻辑

修复方案：

- 新增 `useAdminPagedQuery<T, F>`。
- 新增 `AdminTablePagination` 和 `AdminSearchForm`。
- 支持 draft/applied filters、页码回退、保留旧数据、刷新当前页。
- 使用 AbortSignal 或请求序号丢弃过期响应。
- 以一个列表页作为完整验证用例，验证后由 T7-T9 迁移其余页面。
- 不创建包含业务字段和权限判断的万能 ProTable。

修复目标：

- 普通分页列表不再手写请求生命周期样板。
- 快速翻页和筛选不会被旧响应覆盖。
- 查询、重置、刷新和分页行为全后台一致。

验收标准：

- Hook 级请求竞态测试通过。
- 验证页的查询、刷新、分页和错误流程通过。
- 组件 API 不包含业务语义。

完成记录（2026-08-21）：

- 新增 `useAdminPagedQuery<T, F>`，统一 draft/applied filters、分页状态、当前页刷新、错误状态、旧数据保留和禁用态清理。
- 请求生命周期同时使用 `AbortController` 和请求序号；分页 action 会同步失效旧请求，过期响应和 stale loading 不再覆盖当前状态。
- 页码越界时在同一请求生命周期内回退最新末页，连续缩页有界重试，无法稳定时保留旧数据并返回可见错误。
- Hook 支持 React Strict Mode、同批次分页 action 和 action 后立即刷新，不产生丢失操作或重复 effect 请求。
- 新增无业务字段和权限语义的 `AdminSearchForm`、`AdminSearchField`、`AdminTablePagination`，继续复用 Bz UI 和既有后台布局。
- 配置管理页作为完整验证用例迁入新抽象，列表 API 接入 `AbortSignal`，查询、重置、刷新、分页、错误和操作后刷新流程统一。
- 分页基础类型迁入 `shared/types/pagination`，旧 `types/page.ts` 及全部旧引用删除。
- 修复共享 CSRF 初始化绑定首个调用方 signal 的问题，调用方取消不会中止其他等待同一 CSRF 初始化的请求。
- Hook、Transport 和现有组件共 22 项测试通过，lint、format check、typecheck 和生产 build 通过。

依赖任务：T3、T4。

### T7：平台管理功能垂直切片迁移

问题描述：核心平台功能的页面、API、类型、组件和样式分散在技术目录。

问题范围：

- users
- roles
- resources
- apis
- configs
- dicts

修复方案：

- 每个功能迁移到独立 `features/<feature>`。
- 同步拆分 Payload、API Client、业务 Model、UI 和页面。
- 应用 T6 的列表查询抽象。
- 页面和业务组件样式迁移为 CSS Modules。
- 集中定义每个 feature 的权限码。
- 每个功能迁移完成时直接删除原页面、API、类型、组件和旧 CSS。

修复目标：

- 平台管理功能形成完整垂直切片。
- 修改一个功能不需要在多个顶层技术目录中查找文件。

验收标准：

- 六个功能的列表、详情、新增、编辑、权限和分页流程通过。
- 原技术目录中的对应文件全部删除。
- build、lint、typecheck 通过。

完成记录（2026-08-21）：

- users、roles、resources、apis、configs、dicts 已迁入独立 feature；页面、API Client、Payload、Model、UI、权限常量和样式可在单一切片内定位。
- 六个旧页面、顶层 API、顶层类型及专用组件目录已直接删除，旧 import、转发文件和兼容入口为 0；`page-map` 保持 T10 前的路由职责并改为引用 feature 页面。
- users、roles、apis、configs、dicts 普通列表统一使用 T6 的分页 Hook、查询表单和分页栏，查看权限控制请求启停，CRUD 后刷新及末页回退行为一致。
- roles 改为服务端分页；users 的可分配角色使用自身轻量投影，不直接依赖 roles 内部模型和 Client。
- resources 管理能力并入既有资源 feature，树形列表和详情使用独立 AbortController/请求代次；保存删除后同步运行时资源树，并由页面重挂载刷新管理树。
- 字典管理 API 与公共字典查询分离；跨 feature 只依赖匿名 public 字典入口，批量字典数据由 public endpoint 聚合，不隐含 `dict.view` 管理权限。
- 各切片集中定义按钮权限码；跨 feature 基础能力通过 `auth/preferences`、`resources/permissions`、`dicts/public` 明确公开入口访问。
- API Payload 与稳定 Model 显式分离并映射；Apis 进一步拆出 UI ViewModel，不再把字典标签和 tooltip 写入业务 Model。
- 六功能专属样式已迁入 CSS Modules；旧 resources/configs 全局文件和其他专属选择器删除，通用实体抽屉及授权样式中性化后归 `shared/ui/admin`。
- `resources.xml` 中六个历史 Vue component 元数据已同步为当前 feature 页面标识，资源维护提示不再引用旧页面。
- 前端 22 项测试、lint、format check、typecheck、生产 build 和后端 compile 通过。

依赖任务：T4、T5、T6。

### T8：运维与可观测功能垂直切片迁移

问题描述：日志、诊断、方法统计和系统文件仍采用页面大文件及分散 API/类型结构。

问题范围：

- audit-logs
- login-logs
- method-stat
- diagnostic
- system-files

修复方案：

- 按 feature 迁移页面、API、类型、组件和样式。
- 普通列表使用 T6 抽象。
- 文件预览和下载只使用 T3 Transport。
- 统一详情抽屉分组，删除独立的诊断分组样式体系。
- 大页面按查询区、表格、详情抽屉和业务 Hook 拆分。

修复目标：

- 运维功能不再绕过请求层。
- 详情抽屉、分页和错误行为统一。

验收标准：

- 五个功能的列表、详情、下载、开关和清理流程通过。
- 不存在文件请求旁路。
- 原技术目录中的对应文件全部删除。

完成情况：

- audit-logs、login-logs、method-stat、diagnostic、system-files 已迁入独立 feature，API Payload、稳定 Model 和按钮权限码按切片集中维护。
- 三个普通分页列表已统一使用 `useAdminPagedQuery`、`AdminSearchForm` 和 `AdminTablePagination`，具备请求取消、请求代次、末页回退和内联错误反馈。
- Diagnostic 已改为完成后再调度的串行轮询，刷新请求使用 AbortController 与 generation 双重隔离；服务端配置和抽屉草稿分离，后台刷新不再覆盖未保存输入。
- MethodStat 的开关和清理操作增加同步 mutation 锁，详情请求支持取消，列表刷新统一复用 T6 生命周期。
- SystemFiles 的列表、预览、物理详情和反向引用使用独立请求生命周期；Blob URL 由稳定 ref 统一释放，反向引用请求已按后端契约修正为 POST，预览和下载均只经过统一 Transport。
- 五个功能的专属样式已迁入 CSS Modules，日志详情的真正共享预格式化样式归入 `shared/ui/admin`；旧日志、诊断、方法统计及系统文件全局选择器已删除。
- 原 `src/components/admin-pages`、`src/api`、`src/types` 中五个功能对应文件已删除，`page-map.tsx` 和 `resources.xml` 已更新为 feature 页面标识。
- 前端 22 项测试、lint、format check、typecheck、生产 build 和后端 compile 通过。

依赖任务：T4、T5、T6。

### T9：用户能力、自助页和认证功能垂直切片迁移

问题描述：用户能力、应用包、个人资料、自助页和认证仍分散在页面、API、类型和 registry 中。

问题范围：

- web-users
- user-feature-applications
- user-feature-packages
- auth/login
- admin home
- profile
- profile/password
- profile/preferences
- help
- permission policy placeholder

修复方案：

- 按 feature 迁移页面、API、类型、组件和样式。
- 合并重复认证 API 和认证类型。
- 删除无实际功能的占位路由，或在本任务中实现为明确功能；不保留永久占位页面。
- 用户功能管理使用统一表格输入和查询抽象。
- 个人资料和偏好 API 使用统一错误反馈，避免双 Toast。

修复目标：

- 用户能力和自助页面具有明确功能边界。
- 认证只有一个 API 和状态事实来源。

验收标准：

- 登录、个人资料、偏好、应用包和用户能力维护流程通过。
- 重复 `auth.ts`、认证 Payload 和未使用 API 删除。
- 原技术目录中的对应文件全部删除。

完成情况：

- web-users、user-feature-applications、user-feature-packages、profile、home、help 和登录 UI 已迁入独立 feature；原顶层 `src/api`、`src/types` 及对应业务组件目录已迁空。
- 三个用户能力普通列表已统一使用 `useAdminPagedQuery`、`AdminSearchForm` 和 `AdminTablePagination`，列表具备取消、请求代次、末页回退和内联错误反馈。
- applications 与 packages 通过明确的 `public/catalog` 边界共享应用和应用包目录，web-users 不再进入其他 feature 的内部 API 或 Model。
- 用户能力、应用包和应用详情抽屉补充 AbortController 生命周期；状态维护、删除和保存增加同步 mutation 锁及单一错误反馈。
- 认证核心继续保持单 Cookie、单 Transport、单 authStore；历史重复 `auth.ts` 已在前置任务删除，本任务进一步拆分 Auth Payload、补齐 `mustChangePassword`、删除 `username/account` 双字段和未使用 Store 订阅导出。
- 登录重定向仅接受安全的后台站内路径，拒绝协议相对地址和登录页自循环；首次强制改密用户直接进入修改密码页，修改成功后同步认证模型。
- profile API 已拆分 Payload 与 Model，错误且未使用的登录活动 GET 分页 API 删除；密码不再 trim，密码策略由 profile 边界验证后进入页面。
- 偏好页请求具备取消与统一加载反馈，编辑草稿不再被外部配置更新覆盖；自助配置后端改为仅要求 ADMIN 登录态，与不进入资源授权树的自助页约定一致。
- 空白工作台已实现为真实账号工作台；帮助页删除未实现快捷键说明；没有独立领域能力的 permission policy 占位路由、静态路由、资源定义和组件已全部删除。
- 相关业务样式已迁入 CSS Modules，旧 auth、profile、help、user-feature 和占位全局选择器删除；`resources.xml` 已同步当前 feature 页面标识。
- 前端 22 项测试、lint、format check、typecheck、生产 build、后端 system 编译及 `git diff --check` 通过。

依赖任务：T3-T6。

### T10：真实 App Router、路由权限与代码分割重构

问题描述：全部后台页面通过 catch-all 和客户端 pageMap 分发，路由定义重复且无法有效按页分包。

问题范围：

- `src/app/admin/**`
- `admin-shell`
- `page-map.tsx`
- `admin-routes.ts`
- 后端资源路径到前端页面的解析

修复方案：

- 为 T7-T9 的每个页面建立真实 `page.tsx`。
- 建立受保护后台 layout 和标准 loading/error/not-found 边界。
- 删除 catch-all 页面分发、`pageMap`、`KNOWN_ADMIN_SLUGS`。
- 后端资源树只负责菜单和权限，不负责动态加载任意前端组件。
- 自助页路由策略与资源菜单路由策略明确分离。
- 将页面权限策略集中到路由边界，feature 只保留按钮权限。
- 验证 Next 路由级自动分包。

修复目标：

- 页面物理路径与 URL 一一对应。
- 路由和页面组件不再维护三份清单。
- 每个页面按需加载并具备独立错误边界。

验收标准：

- 所有后台 URL 直接访问、刷新、前进后退和标签切换通过。
- 无权限、未登录、非管理员、404 行为通过。
- catch-all 客户端分发代码为 0。
- 后台不同页面不再共享完全相同的全量业务 chunk。

完成情况：

- 15 个资源菜单页和 4 个后台自助页均已建立真实 `page.tsx`，登录页保持在受保护布局之外，静态导出共生成 20 个后台页面。
- 受保护布局统一处理登录态和 ADMIN 身份，资源页与自助页采用独立路由分组；资源菜单权限集中在路由边界，自助页只要求 ADMIN 登录态。
- catch-all、`pageMap`、`KNOWN_ADMIN_SLUGS`、静态业务路由表和客户端页面二次分发已删除；菜单 bootstrap 响应不再返回组件加载目标。
- 标签页保活改为缓存 App Router 提供的页面节点，不再从壳体同步导入业务页面；关闭和刷新标签时同步清理或重建缓存实例。
- 已建立标准 loading、error 和 not-found 边界；未登录、非管理员、资源未就绪、资源加载失败和无菜单权限均由集中路由边界处理。
- Next 生产构建清单确认页面物理路径与 URL 一一对应；不同页面的 client reference manifest 只包含各自 feature，不再共享全部业务页面模块。
- 前端 22 项测试、lint、format check、typecheck、生产 build、后端 system 编译及 `git diff --check` 通过。

依赖任务：T7、T8、T9。

### T11：全局 CSS 清理与样式隔离收口

问题描述：业务 CSS 全局加载，存在宽泛类名、迁移遗留和大量选择器覆盖。

问题范围：

- `src/style.css`
- `src/styles/**`
- `src/app/globals.css`
- `src/app/admin-shell.css`
- Bz UI 和各 feature 样式

修复方案：

- 删除已迁入组件和 feature 的旧全局样式。
- 全局仅保留 tokens、reset、字体和明确 utility。
- 删除 `.el-*`、`#app`、未使用分页样式和宽泛全局类名。
- 清理 `!important`，通过组件 variant、CSS 变量和模块作用域解决覆盖。
- 将后台业务 CSS 从根 layout 下沉到后台布局或组件模块。

修复目标：

- 登录页和根页面不再加载后台全部业务样式。
- 全局样式不包含 feature 专属选择器。
- 页面样式修改不会影响其他功能。

验收标准：

- `*.module.css` 覆盖所有 feature 和组件业务样式。
- `.el-*`、`#app` 遗留为 0。
- 非必要 `!important` 为 0。
- 关键页面视觉回归通过。

完成情况：

- 根级 `globals.css` 仅保留主题 token 与基础 reset，登录页和根页面不再通过全局入口加载后台壳体、页面或 Bz 业务样式。
- 后台壳体、shared admin、表格输入、Bz UI 及全部 feature 样式已迁入组件级或功能级 CSS Modules，旧 `style.css`、`admin-shell.css`、`styles/admin/**` 和聚合样式入口已删除。
- `.el-*`、`--el-*`、`#app`、历史分页/占位规则、宽泛未归属类和 `!important` 已清零；portal、拖拽状态及跨组件定制改用模块类或 CSS 变量。
- Next 生产构建切换为 webpack 严格 CSS 分块，导出产物确认登录页不加载 `AdminShell` 或后台 feature CSS，后台页面按路由加载各自样式。
- 前端 22 项测试、lint、format check、typecheck、生产 build 及 `git diff --check` 通过。

依赖任务：T4、T7-T10。

### T12：测试、CI、体积预算与最终清理

问题描述：缺少自动化测试和完整门禁，无法持续防止结构退化和高可用问题回归。

问题范围：

- 单元测试和组件测试
- 关键后台 E2E
- CI 命令
- 构建体积和依赖审计
- 文档、无效文件和生成目录规则

修复方案：

- 在 T1 的 Vitest + Testing Library 基线上补齐结构性测试，关键流程增加 Playwright。
- 完整覆盖 HTTP 错误、401 会话结束、运行时配置、权限、分页竞态和路由守卫。
- 增加登录、页面访问、列表筛选分页、抽屉保存等关键 E2E。
- CI 依次执行安装、审计、格式、lint、typecheck、测试、build 和 E2E。
- 基于 Next 构建诊断建立路由首屏和单 chunk 预算。
- 删除未使用 API、类型、样式、组件和旧目录。
- 更新 README、架构说明和开发约束。

修复目标：

- 关键安全、权限、路由和请求并发行为可以自动回归。
- 不符合依赖边界、质量标准和体积预算的变更不能合入。
- 重构后不存在旧结构残留和无效兼容代码。

验收标准：

- `npm ci`
- `npm audit --omit=dev`
- `npm run format:check`
- `npm run lint`
- `npm run typecheck`
- `npm run test`
- `npm run build`
- `npm run test:e2e`
- 架构依赖检查和 bundle budget 通过。

完成情况：

- Vitest/Testing Library 扩展为 8 个测试文件、35 项测试，覆盖 HTTP timeout/network/业务错误/响应模式、401 会话结束、运行时配置并发与重试、资源继承权限、分页竞态和路由守卫。
- 新增 Playwright Chromium 门禁，5 条 E2E 覆盖匿名跳转、管理员登录、菜单 403、用户筛选分页及新增抽屉保存，使用静态 preview 与集中 API mock 独立运行。
- 新增 `check:architecture`，持续检查最终目录、层级和跨 feature public 契约、普通 fetch 归属、显式 `any`、TypeScript 绕过、旧路径和旧样式残留。
- 新增 `check:bundle` 和固定预算；当前登录路由峰值 679.7 KiB raw / 212.7 KiB gzip，后台最大路由 911.5 KiB / 277.9 KiB，20 个后台路由保持 20 个独立 page chunk。
- 新增 GitHub Actions，固定 Node 24.16.0 与 npm 11.13.0，执行安装、生产依赖审计、格式、lint、typecheck、架构、单测、构建、体积及 E2E 全门禁。
- 后台壳体与守卫迁入 app 组合层，删除顶层 `components`、未使用 storage、显式 `any` 和 `.gitignore` Markdown 围栏；跨 feature auth 能力收敛到 `public` 契约。
- Preview Origin 校验补齐凭据、query 和 hash 拒绝；Docker 路径契约统一；Nginx 静态资源启用 gzip。
- `npm ci`、`npm audit --omit=dev`、全部质量命令、生产构建、bundle budget、35 项单测和 5 项 E2E 均通过。

依赖任务：T1-T11。

## 9. 任务总表

| 任务编号 | 任务内容                                | 主要解决问题                 | 状态   |
| -------- | --------------------------------------- | ---------------------------- | ------ |
| T1       | 工具链、依赖和质量命令基线              | P05-P10                      | 已完成 |
| T2       | 可信运行时配置与静态部署重构            | P10、P15                     | 已完成 |
| T3       | 认证会话与统一 HTTP Transport 重构      | P11、P12、P15、P19           | 已完成 |
| T4       | 共享层、公共 UI 和依赖边界重构          | P01、P02、P04-P06、P14、P16  | 已完成 |
| T5       | 全局状态、运行时和权限基础设施重构      | P11-P13、P17、P19            | 已完成 |
| T6       | 后台分页查询与表单基础抽象              | P18、P19                     | 已完成 |
| T7       | 平台管理功能垂直切片迁移                | P01、P02、P12、P14、P17、P18 | 已完成 |
| T8       | 运维与可观测功能垂直切片迁移            | P01、P02、P11、P12、P14、P18 | 已完成 |
| T9       | 用户能力、自助页和认证功能垂直切片迁移  | P01、P02、P11-P14、P17、P18  | 已完成 |
| T10      | 真实 App Router、路由权限与代码分割重构 | P03、P16、P17                | 已完成 |
| T11      | 全局 CSS 清理与样式隔离收口             | P14、P16                     | 已完成 |
| T12      | 测试、CI、体积预算与最终清理            | 全部问题的持续门禁           | 已完成 |

## 10. 全局完成标准

只有同时满足以下条件，本次重构才视为完成：

- `src` 已符合 `app / features / shared / runtime / styles` 目标结构。
- 顶层 `src/api`、`src/types`、`src/components/admin-pages` 和混合职责 `src/core` 已删除。
- `shared` 不依赖任何 feature 或 app。
- 普通 HTTP fetch 只存在于统一 Transport。
- 浏览器存储中不存在认证 Token。
- Bootstrap 请求目标不能由查询参数或 localStorage 修改。
- catch-all 客户端页面分发器、`pageMap` 和手工 slug 清单已删除。
- 普通分页列表均使用统一查询生命周期抽象。
- feature 样式全部隔离，全局 CSS 不含业务页面选择器。
- 显式 `any`、`ts-ignore`、两层及以上父级 import 均为 0。
- 生产依赖 high/critical 漏洞为 0。
- lint warning 为 0。
- 所有质量命令和关键 E2E 通过。
- Next 路由首屏和 chunk 体积满足最终确认的预算。
- 不存在旧路径转发、旧 API 回退、旧 Token 兼容和旧 CSS 映射。

## 11. 执行规则

1. 用户确认本清单后才允许开始实施。
2. 严格按 T1 到 T12 的顺序推进。
3. 一次只执行一个任务。
4. 每个任务完成后必须更新本文档和 README 中的任务状态。
5. 每个任务完成后必须暂停，等待用户确认是否继续。
6. 每个任务必须独立恢复到可编译、可运行、可验证状态。
7. 若实施中发现新增结构性问题，先补充问题矩阵和任务范围，再等待用户确认，不临时绕过。
8. 不得以兼容旧实现为理由保留双路由、双 API、双 Store、双样式或双认证机制。
