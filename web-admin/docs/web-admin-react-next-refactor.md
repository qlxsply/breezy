# web-admin Vue3 -> React + Next.js 重构方案

## 一、需求背景

- 本次只处理 `web-admin/`，暂不处理 `web/`。
- 目标不是改产品，不是改页面，不是改交互，而是将 `web-admin` 的前端技术方案从 `Vue 3 + Vite` 整体切换为 `React + Next.js`。
- 页面样式、视觉结构、功能、权限、动态菜单、通知、SSE、推送能力都需要保持现状。
- 用户已明确允许迁移过程中的阶段性不可用，因此重构可以按模块分段推进，不要求每一步都可运行。
- 最终状态要求 `web-admin/` 中不再保留任何 Vue 代码，相关依赖直接移除。

## 二、当前现状

### 1. 工程与构建

- 当前工程为独立前端模块：`web-admin/`
- 当前方案：`Vue 3 + Vue Router + Vite + TypeScript`
- 当前 `package.json` 关键点：
    - `packageManager: npm@11.11.1`
    - `engines.node: 22.22.1`
    - `engines.npm: 11.11.1`
    - `build` 依赖 `vue-tsc` 与 `vite build`
- 当前 Maven 前端构建：
    - `web-admin/pom.xml` 通过 `frontend-maven-plugin` 执行 `npm install` 与 `npm run build`
    - 根 `pom.xml` 固定前端工具链版本为 `node v22.22.1`、`npm 11.11.1`
- 当前部署方式：
    - `Dockerfile` 基于 `nginx:1.27-alpine`
    - 直接拷贝 `dist/` 静态产物
    - `nginx.conf` 通过 `/admin/ -> /index.html` 做 SPA 回退

### 2. 代码规模与目录

- 页面层：`src/pages/*.vue`，共 22 个页面
- API 层：`src/api/*.ts`，共 20 个模块
- registry 层：`src/registry/*.ts`，共 9 个模块
- 自研组件层：`src/components/bz/*.vue`，共 44 个 Vue 组件
- 布局层：
    - `src/App.vue`
    - `src/layout/AdminLayout.vue`
    - `src/layout/AppHeader.vue`
- 全局样式层：
    - `src/styles/theme.css`
    - `src/styles/list-page.css`
    - `src/styles/admin-page.css`
    - `src/styles/bz-ui.css`

### 3. 当前核心能力分布

#### 3.1 应用启动

- `src/main.ts` 中先加载认证，再按内部用户初始化动态路由、资源树、权限，再启动 SSE 生命周期，最后挂载 Vue 应用。

#### 3.2 路由与菜单

- `src/router/index.ts` 同时维护：
    - 静态路由
    - 基于后台资源树生成的动态路由
    - 登录跳转
    - 菜单访问权限判断
    - 面包屑与后台导航元信息
- 动态路由依赖：
    - `src/registry/bootstrap.ts`
    - `src/registry/resources.registry.ts`
    - `src/registry/permissions.registry.ts`
    - `src/utils/resourceLoader.ts`

#### 3.3 鉴权与个性化配置

- `src/registry/auth.registry.ts` 负责：
    - token 读取与登录态初始化
    - `getMe/login/logout`
    - 登录后权限刷新
    - 个性化配置缓存
    - 外部用户通知/推送初始化
- 浏览器持久化依赖 `localStorage`

#### 3.4 通知、SSE、推送

- `src/utils/sse-coordinator.ts` 使用 `EventSource`
- `src/registry/sse.registry.ts` 管理 SSE 生命周期
- `src/registry/todo-reminder.registry.ts` 依赖：
    - `Notification`
    - `navigator.serviceWorker`
    - `PushManager`
- 这些能力都天然偏客户端，不适合依赖 SSR 作为核心运行方式

#### 3.5 自研 Bz UI 组件

- 当前后台严重依赖 `Bz UI`，不是简单替换若干页面组件即可完成迁移
- 关键组件包括：
    - `BzTable` / `BzTableColumn`
    - `BzForm` / `BzFormItem`
    - `BzDialog`
    - `BzSelect` / `BzOption`
    - `BzPagination`
    - `BzTree`
    - `BzDropdown`
    - `BzMessageHost`
    - `BzConfirmHost`
    - `v-loading` 指令对应的加载态能力
- 结论：必须重写 React 版 `Bz UI`，不能直接搬运 Vue 组件

### 4. 页面迁移范围

- 后台壳体与登录自助页：
    - `AdminLoginPage`
    - `AdminPlaceholderPage`
    - `AdminProfilePage`
    - `AdminProfilePasswordPage`
    - `AdminProfilePreferencesPage`
    - `AdminHelpPage`
- 平台管理页：
    - `ConfigsAdminPage`
    - `ApisAdminPage`
    - `DictAdminPage`
    - `SystemFilesPage`
    - `DiagnosticPage`
    - `MethodStatPage`
- 权限中心页：
    - `UsersAdminPage`
    - `RolesAdminPage`
    - `LoginLogsPage`
    - `AuditLogsPage`
    - `ResourcesAdminPage`
- 用户中心页：
    - `WebUsersAdminPage`
    - `UserFeaturePackagesPage`
    - `UserFeatureApplicationsPage`
- 兜底页：
    - `NotFoundPage`
    - `AdminHomePage`

### 5. 与 `web/` 模块关系

- 当前 `vite.config.ts` 中仍存在 `@shared -> ../web/src` 别名
- 但当前 `web-admin/src` 已无 `@shared` 实际引用
- 这意味着本次可以将 `web-admin` 视为可独立重建的工程，不需要同步迁移 `web/`

## 三、重构目标

- 使用 `React + Next.js + TypeScript` 完整替代当前 `Vue 3 + Vite + TypeScript`
- 保持现有后台：
    - 页面布局不变
    - 交互流程不变
    - 样式表现不变
    - API 协议不变
    - 动态资源菜单机制不变
    - 权限判断不变
    - 通知、SSE、推送能力不变
- 最终删除：
    - 全部 `.vue` 文件
    - `vite.config.ts`
    - `index.html`
    - `vue-tsc`
    - `@vitejs/plugin-vue`
    - `eslint-plugin-vue`
    - `vue-eslint-parser`
    - `vue` / `vue-router`
- 同步升级工具链：
    - Node `v24.16.0`
    - npm `11.13.0`
- 默认不切换到 pnpm，除非迁移过程中证明确有必要

## 四、技术方案

### 1. 方案选择

- 采用 `Next.js App Router`
- 采用“Next 工程壳体 + 客户端主应用”的设计
- 不以 SSR 为目标，不追求把当前后台改造成服务端渲染系统
- Next 的核心作用：
    - 提供 React 工程基础设施
    - 提供更清晰的目录式路由入口
    - 承担新的构建与部署方案

### 2. 路由设计建议

- 页面入口建议：
    - `/admin/login`
    - `/admin`
    - `/admin/[[...slug]]`
- 通过客户端读取当前 pathname，再结合后台资源树完成：
    - 静态页匹配
    - 动态资源页匹配
    - 权限拦截
    - 菜单高亮
    - 面包屑生成
- 当前 Vue Router 守卫逻辑将重写为 React Hook + 路由壳体控制逻辑

### 3. 状态管理建议

- 不急于引入额外状态库
- 优先使用：
    - React Context
    - `useSyncExternalStore` 或轻量自定义 store
    - 自定义 hooks
- 原 `registry/*.ts` 可作为领域边界保留，但实现从 Vue 响应式改为 React 友好实现

### 4. UI 迁移建议

- React 版 `Bz UI` 单独放在 `src/components/bz/`
- 优先复刻而不是重设计
- 迁移顺序建议：
    - 容器/按钮/文本/Tag
    - 表单输入类
    - 弹窗/抽屉/消息/确认
    - 下拉/菜单/树
    - 表格/分页
    - 日期选择与复杂交互组件
- 样式尽量复用现有 CSS，组件层只替换模板与事件绑定实现

### 5. API 与浏览器能力迁移建议

- `src/api/http.ts` 可以延续 `fetch` 封装思路
- `import.meta.env` 统一迁移为 Next 环境变量约定
- 预计需要调整为 `NEXT_PUBLIC_*` 前缀的环境变量
- 以下模块必须保证只在客户端执行：
    - 鉴权存储
    - SSE
    - Notification
    - Service Worker
    - Push
    - 依赖 `window/localStorage` 的工具模块

### 6. 部署建议

- 当前 `nginx + dist` 静态部署不适合原样保留
- 推荐改为标准 Next 产物部署方案
- 对应要调整：
    - `Dockerfile`
    - `nginx.conf`
    - `web-admin/pom.xml` 的构建命令
- 若后续用户要求仍必须纯静态部署，再单独评估 `next export` 可行性

## 五、风险与重点

### 1. 最高风险点

- `Bz UI` 需要整体重写，工作量大于普通页面迁移
- 当前后台菜单是运行时资源树驱动，不是 Next 原生静态文件路由即可覆盖
- SSE、通知、Service Worker、Push 都要求严格的客户端边界控制
- 当前部分页面非常长且交互复杂，例如：
    - `ConfigsAdminPage.vue`
    - `DictAdminPage.vue`
    - `SystemFilesPage.vue`
    - `AuditLogsPage.vue`
    - `AdminLayout.vue`

### 2. 可降低风险的点

- `web-admin` 已基本不依赖 `web/` 源码，可独立重构
- `api/*.ts`、`types/*.ts` 中相当一部分模型可以保留
- 现有 CSS 文件可作为视觉保持的基础资产继续使用
- 用户已允许分步迁移期间暂时不可用，可以接受工程阶段性半成品状态

## 六、任务拆分

| 任务编号 | 任务内容 | 状态 |
| ---- | ---- | --- |
| T1 | 确认本次技术路线：`web-admin` 采用 `Next.js App Router + 客户端主应用`，不处理 `web/` | 已完成 |
| T2 | 升级工具链版本到 `node v24.16.0`、`npm 11.13.0`，同步评估 Maven 前端插件与本地包管理兼容性 | 已完成 |
| T3 | 移除 Vite/Vue 工程入口，建立 Next 基础目录、入口布局、全局样式装配方式 | 已完成 |
| T4 | 重建后台公共壳体：登录页、后台布局、侧边导航、顶部工具区、面包屑、消息/确认宿主 | 已完成 |
| T5 | 重写 React 版 `Bz UI` 第一批基础组件，先覆盖页面迁移所需的最小闭环 | 已完成 |
| T6 | 迁移基础能力层：`api`、`types`、`utils`、环境变量、鉴权存储、格式化与消息确认 | 已完成 |
| T7 | 迁移 registry 与客户端生命周期：认证、资源树、权限、通知、SSE、推送、快捷键 | 已完成 |
| T8 | 重写后台路由适配与动态资源菜单机制，保证权限拦截、菜单高亮、面包屑一致 | 已完成 |
| T9 | 按页面批次迁移 22 个后台页面及其专属子组件，完成后逐步删除对应 Vue 页面 | 开发中 |
| T10 | 清理全部 Vue 遗留与旧构建物，改造 Docker/Maven 构建，并完成最终验证与文档回写 | 未开始 |

## 七、建议执行顺序

1. 先搭 Next 壳体，不碰页面细节。
2. 先把 `AdminLayout`、登录、消息确认、路由容器跑起来。
3. 先做 React 版 `Bz UI` 最小集，再迁移复杂页面。
4. 页面迁移优先从结构稳定、组件复用高的后台列表页开始。
5. 最后处理 `SystemFiles`、`Dicts`、`Configs`、通知推送等复杂能力。
6. 最后一轮再清空 Vue 依赖与旧构建配置。

## 八、当前结论

- 这次不是“把几个页面改成 React”，而是“把一个完整的 Vue 后台前端工程替换成 Next/React 工程”。
- 真正的核心工作量在三块：
    - React 版 `Bz UI`
    - 动态资源路由与权限壳体
    - 复杂页面逐页迁移
- 从当前代码现状看，这项重构是可做的，但应按任务拆分推进，而不是一次性整体替换。
