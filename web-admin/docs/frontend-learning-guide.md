# Breezy Web Admin 前端学习指南

## 1. 文档目标

本文面向没有前端开发经验、希望通过 `web-admin` 学习现代前端开发的读者。

本文不讲解 CSS 和 HTML，重点介绍：

- JavaScript、TypeScript、TSX 和 ES Module。
- React 19 的组件、状态、Hook 和渲染模型。
- Next.js 16 App Router、静态导出和路由组织。
- 本项目的 feature 垂直切片、状态管理和组件分层。
- HTTP、Cookie、CSRF、异步请求和错误处理。
- SSE、Web Push、Service Worker 和跨标签页通信。
- Vitest、Playwright、ESLint、Prettier、构建与部署。

文中的路径均相对于 `web-admin/`。行号用于帮助首次定位，代码变化后应优先按文件名和符号名搜索。

## 2. 先建立整体认识

### 2.1 项目是什么

`web-admin` 是一个 React 19 + Next.js 16 管理后台，主要特点如下：

| 项目能力    | 当前实现                               |
| ----------- | -------------------------------------- |
| 开发语言    | TypeScript、TSX、少量 JavaScript/MJS   |
| UI 框架     | React 19                               |
| 应用框架    | Next.js 16 App Router                  |
| 生产形态    | 静态导出，不运行 Next.js Node 服务     |
| HTTP 客户端 | 浏览器原生 `fetch` 的统一封装          |
| 全局状态    | 自研 Store + `useSyncExternalStore`    |
| UI 组件     | 自研 Bz UI                             |
| 单元测试    | Vitest + Testing Library + jsdom       |
| 端到端测试  | Playwright                             |
| 代码质量    | TypeScript、ESLint、Prettier、架构检查 |
| 实时通信    | SSE、Web Push、BroadcastChannel        |

项目没有使用 Redux、Axios、React Query、SWR、React Hook Form、Formik、Zod 或其他 UI 框架。理解这一点很重要，因为你在源码中看到的状态、请求、表单和校验逻辑大多是项目自己实现的。

### 2.2 一次典型页面访问

以 `/admin/users` 为例，主流程是：

```text
Next.js 物理路由 page.tsx
    -> UsersPage 页面组件
    -> useAdminPagedQuery 管理筛选、分页和请求生命周期
    -> users/api/client.ts 组装请求并转换数据
    -> shared/transport/http.ts 发送 HTTP 请求
    -> Java 后端 API
```

对应文件：

| 阶段           | 典型文件                                              |
| -------------- | ----------------------------------------------------- |
| 路由入口       | `src/app/admin/(protected)/(resource)/users/page.tsx` |
| 页面编排       | `src/features/users/UsersPage.tsx`                    |
| 分页状态       | `src/shared/hooks/useAdminPagedQuery.ts`              |
| API Client     | `src/features/users/api/client.ts`                    |
| 网络 Payload   | `src/features/users/api/payload.ts`                   |
| 前端 Model     | `src/features/users/model/types.ts`                   |
| 通用 Transport | `src/shared/transport/http.ts`                        |

### 2.3 浏览器与 Node.js 的区别

前端项目同时包含两类 JavaScript 运行环境：

| 运行环境 | 运行内容                              | 典型文件                                         |
| -------- | ------------------------------------- | ------------------------------------------------ |
| 浏览器   | React 页面、HTTP、SSE、Push、用户交互 | `src/**/*.ts`、`src/**/*.tsx`、`public/sw.js`    |
| Node.js  | 开发服务器、构建、检查、静态预览      | `next.config.ts`、`scripts/*.mjs`、`preview.mjs` |

Node.js、npm、ESLint 和 Next.js 构建器不会作为完整工具进入浏览器。生产浏览器最终执行的是构建生成的 JavaScript 文件。

## 3. 前端开发语言

### 3.1 JavaScript

JavaScript 是浏览器实际执行的主要编程语言。TypeScript 和 TSX 最终也会转换成 JavaScript。

本项目中的直接 JavaScript 文件主要用于 Node 脚本和 Service Worker：

- `scripts/check-architecture.mjs`：检查源码目录和依赖规则。
- `scripts/check-bundle-budget.mjs`：检查构建产物体积。
- `preview.mjs`：提供本地静态预览和 API 代理。
- `public/sw.js`：在浏览器 Service Worker 环境中处理 Push 消息。

#### 变量声明

项目只使用现代的 `const` 和 `let`，ESLint 禁止 `var`。

典型位置：`src/shared/transport/http.ts:50-58`。

- `const`：变量不能再次赋值，但它指向的对象内容不一定不可变。
- `let`：变量会在后续流程中改变。
- 模块顶层变量：由所有导入该模块的代码共享，类似 Java 类的静态字段。

#### 函数是一等值

JavaScript 函数可以保存到变量、作为参数传入，也可以作为结果返回。

典型位置：`src/shared/lib/store.ts:9-23`。

`createStore` 返回 `getState`、`setState`、`subscribe` 三个函数。这些函数仍能访问 `createStore` 内部的 `state` 和 `listeners`，这种能力称为闭包。

可以把闭包暂时类比为：不用显式声明 Java class，也能创建带私有状态和方法的对象实例。

#### 对象与展开语法

典型位置：`src/features/auth/model/auth-store.ts` 和 `src/shared/hooks/useAdminPagedQuery.ts:269-307`。

```ts
{ ...state, pageNo: 1 }
```

含义是先复制 `state` 的全部属性，再覆盖 `pageNo`。React 中通常使用这种不可变更新，而不是直接修改旧状态对象。

#### 数组方法

本项目经常使用：

| 方法      | 作用                     | 典型位置                                       |
| --------- | ------------------------ | ---------------------------------------------- |
| `map`     | 将每个元素转换成新元素   | `src/features/users/api/client.ts:51-55`       |
| `filter`  | 保留满足条件的元素       | `src/features/users/api/client.ts:87-90`       |
| `find`    | 查找第一个满足条件的元素 | `src/shared/transport/runtime-config.ts:40-42` |
| `some`    | 是否至少一个元素满足条件 | `src/shared/ui/bz/BzTable.tsx`                 |
| `every`   | 是否全部元素满足条件     | `src/shared/ui/bz/BzTable.tsx`                 |
| `forEach` | 对每个元素执行操作       | `src/shared/lib/store.ts:17`                   |

`map` 和 `filter` 返回新数组，适合 React 的不可变数据处理。

#### `Map` 和 `Set`

- `Set` 保存不重复值，见 `src/shared/lib/store.ts:11` 的监听器集合。
- 资源权限建立多个索引，见 `src/features/resources/model/resource-store.ts`。
- 用户批量选择使用 ID 集合去重，见 `src/features/users/UsersPage.tsx`。

可类比 Java 的 `HashMap` 和 `HashSet`。

#### 可选链和空值合并

```ts
inputRef.current?.focus();
const value = modelValue ?? "";
```

- `?.`：左侧为 `null` 或 `undefined` 时停止访问。
- `??`：左侧为 `null` 或 `undefined` 时才使用默认值。
- `||`：左侧为 `""`、`0`、`false` 等假值时也会使用默认值。

典型位置：`src/features/auth/ui/AuthLoginPageCard.tsx:36-38`、`src/shared/ui/bz/BzInput.tsx`。

#### 异步函数和 Promise

`async` 函数一定返回 Promise，`await` 用于等待 Promise 完成。

典型位置：`src/features/auth/ui/AuthLoginPageCard.tsx:59-77`。

```ts
const current = await login(username.trim(), password);
```

项目中也经常出现：

```ts
void submit();
```

这里的 `void` 表示调用方明确不等待该 Promise，不表示取消任务。被忽略的异步任务仍应在内部处理异常。

多个互不依赖的异步操作可使用 `Promise.all` 并发执行。典型位置：`src/features/dicts/public/dictionary-client.ts`。

#### Event Loop

JavaScript 在浏览器主线程上按事件循环调度同步任务、微任务和定时任务。

典型位置：

- `queueMicrotask`：`src/app/admin/(protected)/_components/AdminShell.tsx`。
- `setTimeout`：`src/runtime/admin-runtime.ts` 的失败重试。
- `requestAnimationFrame`：`src/shared/hooks/useAdminQueryPanelLayout.ts`。

初学时应记住：`await` 不会阻塞整个浏览器，它会暂停当前异步函数，并把后续代码安排到 Promise 完成之后继续执行。

### 3.2 TypeScript

TypeScript 在 JavaScript 上增加静态类型。浏览器不会直接执行 TypeScript，类型信息在构建后会被移除。

项目配置见 `tsconfig.json`：

- `strict: true`：启用严格类型检查。
- `allowJs: false`：主源码不接受普通 JS 文件。
- `noEmit: true`：`tsc` 只检查类型，不单独输出文件。
- `target: ES2022`：目标 JavaScript 语言级别。
- `module: ESNext`：使用现代模块语法。
- `jsx: react-jsx`：使用 React JSX 转换。

#### `.ts` 与 `.tsx`

- `.ts`：普通 TypeScript，适合类型、API、Store、工具函数。
- `.tsx`：允许编写 JSX，主要用于 React 组件。

TSX 不是 HTML。它是 TypeScript 中描述 React 元素树的一种语法，最终会转换为 JavaScript 函数调用。

#### `interface` 与 `type`

典型位置：`src/features/users/model/types.ts`。

- `interface`：常用于描述对象结构和可扩展契约。
- `type`：可描述对象，也适合联合类型、别名和组合类型。

项目常用字符串字面量联合代替 TypeScript `enum`：

```ts
type UserStatus = "ENABLED" | "DISABLED";
```

这与 Java enum 的目标相似，可以阻止任意字符串进入状态字段，但不会在运行时生成 enum class。

#### 类型推导与 `typeof`

`src/features/users/UsersPage.tsx:54-60` 从默认值推导筛选类型：

```ts
const INITIAL_FILTERS = {
  keyword: "",
  status: "" as "" | UserStatus,
};

type UserFilters = typeof INITIAL_FILTERS;
```

这样默认值和类型不会重复维护。

#### 泛型

`src/shared/hooks/useAdminPagedQuery.ts:63-72` 定义：

```ts
useAdminPagedQuery<T, F>(options);
```

- `T`：列表行类型。
- `F`：筛选条件类型。

用户页在 `src/features/users/UsersPage.tsx:107-121` 传入：

```ts
useAdminPagedQuery<UserEntry, UserFilters>(...)
```

可类比 Java 的 `Page<T>`、`List<T>` 和泛型方法，但 TypeScript 还能根据对象和回调自动推导大量类型。

#### 判别联合

`src/shared/hooks/useAdminPagedQuery.ts:52-58` 的 `QueryAction<F>` 是多个动作对象组成的联合，每种动作都有唯一的 `type`。

`queryReducer` 在 `269-307` 行通过 `switch (action.type)` 处理动作。进入某个分支后，TypeScript 自动知道当前动作还具有哪些字段。

可类比 Java 21 的 sealed 类型层次与模式匹配。

#### `unknown` 与类型守卫

外部数据不可信时应先使用 `unknown`，再逐步验证。

典型位置：

- `src/shared/transport/http.ts:75-90`：验证后端响应信封。
- `src/shared/transport/runtime-config.ts:31-68`：验证运行时配置。
- `src/runtime/sse/sse-message-parser.ts`：验证 SSE 消息。

```ts
function isObject(value: unknown): value is Record<string, unknown>;
```

返回类型中的 `value is ...` 称为类型谓词。判断成功后，编译器会缩小变量类型。

项目禁止显式 `any`。`any` 会绕过类型检查，`unknown` 则要求使用者先判断。

#### 工具类型

| 工具类型       | 含义             | 典型位置                                 |
| -------------- | ---------------- | ---------------------------------------- |
| `Readonly<T>`  | 所有字段只读     | `src/app/layout.tsx`、分页请求类型       |
| `Partial<T>`   | 所有字段可选     | `src/shared/ui/bz/store.tsx`             |
| `Pick<T, K>`   | 从类型中选择字段 | `src/features/users/api/client.ts:43`    |
| `Omit<T, K>`   | 从类型中排除字段 | `src/shared/ui/bz/store.tsx`             |
| `Record<K, V>` | 键值映射         | `src/features/users/UsersPage.tsx:40-51` |
| `keyof T`      | 类型的全部属性名 | `src/shared/ui/bz/BzTable.tsx`           |

这些工具只生成新类型，不会生成运行时对象。

#### `as const` 与 `satisfies`

- `as const` 保留具体字面量并将属性视为只读。
- `satisfies` 检查值符合某个类型，同时保留值自身更精确的类型。

典型位置：

- `src/features/users/permissions.ts`：权限常量。
- `src/app/admin/(protected)/_components/admin-routes.ts`：路由配置。

#### 编译期类型不等于运行时校验

下面的泛型只告诉 TypeScript 期望结果类型：

```ts
post<PageResult<UserPayload>>("/users/page", payload);
```

它不会自动验证服务器 JSON。项目严格验证了通用响应信封和部分安全关键数据，但不是所有业务 Payload 都有完整运行时校验。

这是从 Java 转向 TypeScript 时最容易误解的地方之一。

### 3.3 ES Module

`package.json` 中的 `"type": "module"` 表示 Node.js 脚本也使用 ES Module。

#### 导入与导出

- 默认导出：Next.js 的 `page.tsx` 和 `layout.tsx` 常用，导入方可自定义名称。
- 具名导出：业务函数和组件常用，导入名通常与导出名一致。
- `import type`：只导入类型，编译后消失。

典型位置：`src/features/users/api/client.ts:1-16`。

#### 路径别名

`tsconfig.json:21-24` 配置：

```json
"@admin/*": ["src/*"]
```

因此可以写：

```ts
import { post } from "@admin/shared/transport";
```

而不必写多层 `../../../../shared/transport`。ESLint 会限制跨目录深相对路径。

#### 聚合导出

`src/shared/transport/index.ts` 是聚合出口，调用者不需要知道具体实现位于 `http.ts` 还是 `runtime-config.ts`。

#### feature 公共契约

跨 feature 默认不能访问内部文件，只能通过 `public` 目录提供的稳定契约。

用户页通过 `src/features/dicts/public/dictionary-client.ts` 获取字典，而不是直接调用字典 feature 的内部 API。

这类似 Java 模块只向外暴露 Facade/API 包。

## 4. React 19

### 4.1 函数组件

本项目全部使用函数组件，没有 React class 组件。

最小路由组件见 `src/app/admin/login/page.tsx`。带 Props 的组件见 `src/features/auth/ui/AuthLoginPageCard.tsx:22-28`。

```ts
interface AuthLoginPageCardProps {
  formTitle: string;
  returnLabel: string;
  returnTo: string;
}
```

组件函数会在每次渲染时重新执行。不要把组件函数理解为只实例化一次的 Java 对象方法。

### 4.2 Props、State 和单向数据流

- Props：父组件传入的数据，只读。
- State：组件内部会变化的数据。
- Event callback：子组件通知父组件发生了操作。

典型位置：`src/features/users/UsersPage.tsx` 将数据和 `onEdit`、`onRemove` 等回调传给 `UserTable`。

数据通常按以下方向流动：

```text
父组件 state
    -> Props 传给子组件
    -> 子组件触发回调
    -> 父组件更新 state
    -> React 重新渲染
```

### 4.3 渲染与不可变更新

React state 更新后会重新执行相关组件。React 通常依赖新引用判断数据变化，因此应创建新对象、新数组或新集合，而不是只修改旧值。

典型位置：

- `src/features/auth/model/auth-store.ts`：对象展开更新状态。
- `src/features/users/UsersPage.tsx`：复制集合后更新批量选择。
- `src/shared/hooks/useAdminPagedQuery.ts:269-307`：reducer 返回新状态。

### 4.4 Client Component 与 Server Component

Next.js App Router 中，没有 `"use client"` 的组件默认是 Server Component。

- `src/app/layout.tsx`：Server Component，可组合客户端组件。
- `src/features/users/UsersPage.tsx:1`：Client Component。
- `src/features/auth/ui/AuthLoginPageCard.tsx:1`：Client Component。

以下能力需要客户端边界：

- `useState`、`useEffect` 等客户端 Hook。
- 点击、键盘等浏览器事件。
- `window`、`document`、localStorage 等浏览器 API。
- 客户端 Store 和 HTTP 请求。

一个没有写 `"use client"` 的文件如果被 Client Component 导入，也会进入客户端模块图。因此不能只看当前文件，还要看导入关系。

本项目生产使用静态导出，Server Component 主要在构建期执行，不是每次请求时动态执行。

### 4.5 常用 Hook

| Hook                   | 用途                              | 项目典型场景                          |
| ---------------------- | --------------------------------- | ------------------------------------- |
| `useState`             | 保存组件本地状态                  | 登录字段、抽屉开关、筛选展开状态      |
| `useEffect`            | 与网络、DOM、定时器等外部系统同步 | 加载字典、安装监听器、请求清理        |
| `useRef`               | 保存 DOM 或不触发渲染的可变值     | 输入框引用、AbortController、请求代次 |
| `useReducer`           | 管理复杂状态转换                  | `useAdminPagedQuery`                  |
| `useCallback`          | 保持回调引用稳定                  | 分页查询动作和 Effect 依赖            |
| `useMemo`              | 缓存派生计算                      | 页码、菜单索引                        |
| `useContext`           | 在组件子树共享值                  | Bz 消息和确认框                       |
| `useSyncExternalStore` | 将外部 Store 接入 React           | 认证、资源、通知状态                  |
| `useId`                | 生成稳定唯一 ID                   | `TableSelect`                         |
| `useLayoutEffect`      | 浏览器绘制前同步处理 DOM          | 日期时间输入滚动定位                  |
| `useImperativeHandle`  | 控制 ref 对外暴露的能力           | `BzInput` 暴露 `focus`、`blur`        |

#### `useState`

`src/features/auth/ui/AuthLoginPageCard.tsx:31-34` 保存账号、密码、错误和提交状态。

State 应保存真正影响界面结果的数据，不要把所有临时变量都放入 State。

#### `useEffect`

`src/features/users/UsersPage.tsx:139-151` 展示了两个重要模式：

- Effect 启动异步加载，清理时取消请求。
- 组件卸载时使请求代次失效并中止当前请求。

Effect 更准确的理解是“让渲染结果与 React 外部系统保持同步”，而不是通用的初始化函数。

React Strict Mode 在开发环境会执行额外的挂载、清理和再挂载探测，所以 Effect 必须正确清理资源。

#### `useRef`

`ref.current` 变化不会触发重新渲染，适合保存：

- DOM 元素或组件句柄。
- Timer。
- AbortController。
- 请求序号或 generation。
- 防重复提交锁。

如果值变化后界面需要立即更新，应优先使用 State。

#### `useReducer`

`src/shared/hooks/useAdminPagedQuery.ts:83-95` 使用 reducer 管理筛选、页码、页大小和查询修订号。

当多个状态必须按固定规则一起变化时，reducer 比多个独立 `setState` 更容易维护。

### 4.6 受控组件和表单

登录页中的输入数据流：

```text
useState 保存 username
    -> modelValue 传给 BzInput
    -> 用户输入触发 onValueChange
    -> setUsername 更新 state
    -> 组件重新渲染
```

典型位置：`src/features/auth/ui/AuthLoginPageCard.tsx:85-103`。

项目没有使用 React Hook Form、Formik 或 Schema 校验库，表单采用：

- 受控 State。
- 普通 TypeScript 校验函数。
- `async/await` 提交。
- loading State 防止重复交互。
- 页面内错误提示或消息反馈。

前端校验只用于尽快反馈，不能替代 Java 后端校验。

### 4.7 组件组合

React 倾向组合而不是继承。

`src/shared/ui/admin/AdminListPageTemplate.tsx` 接收多个 `ReactNode` 插槽，用户页在 `src/features/users/UsersPage.tsx` 组装查询区、工具栏、表格、分页和弹层。

通用表格 `src/shared/ui/bz/BzTable.tsx` 通过 render 回调决定每个单元格如何显示。这种模式称为 Render Props。

### 4.8 Context 与外部 Store

项目按用途选择状态方案：

| 状态类型               | 方案         | 示例               |
| ---------------------- | ------------ | ------------------ |
| 当前组件临时状态       | `useState`   | 登录输入、抽屉开关 |
| 当前页面复合状态       | `useReducer` | 分页筛选           |
| 跨组件业务状态         | 外部 Store   | 认证、资源、通知   |
| 明确组件子树的 UI 服务 | Context      | 消息、确认框       |
| 浏览器连接生命周期     | 模块级状态   | SSE、Push          |

不要因为 Redux 流行就认为所有共享状态都必须使用 Redux。本项目的 Store 很小，核心实现只有 `src/shared/lib/store.ts:1-24`，通过 `src/shared/hooks/useStoreValue.ts:6-14` 接入 React。

### 4.9 Portal

Dialog、Dropdown、Tooltip 使用 React `createPortal`，典型文件：

- `src/shared/ui/bz/BzDialog.tsx`
- `src/shared/ui/bz/BzDropdown.tsx`
- `src/shared/ui/bz/BzOverflowTooltip.tsx`

Portal 改变元素实际挂载位置，但不会改变其 React 组件关系，Context 和事件仍按 React 树工作。

### 4.10 React 19 `Activity`

`src/app/admin/(protected)/_components/AdminShell.tsx` 使用 React 19 `Activity` 管理后台多标签页。

非当前标签页被隐藏但保留组件状态，切回时不必从头创建页面。这比简单条件渲染更适合后台标签页保活，但也会增加内存和生命周期管理复杂度。

## 5. Next.js 16 App Router

### 5.1 文件系统路由

App Router 根据 `src/app` 目录生成 URL。

| 文件                                                  | URL/职责           |
| ----------------------------------------------------- | ------------------ |
| `src/app/page.tsx`                                    | 根路径并重定向     |
| `src/app/layout.tsx`                                  | 全站根布局         |
| `src/app/admin/login/page.tsx`                        | `/admin/login`     |
| `src/app/admin/(protected)/(resource)/users/page.tsx` | `/admin/users`     |
| `src/app/admin/(protected)/layout.tsx`                | 受保护后台公共布局 |
| `src/app/admin/(protected)/loading.tsx`               | 路由加载状态       |
| `src/app/admin/(protected)/error.tsx`                 | 渲染错误边界       |
| `src/app/admin/not-found.tsx`                         | 后台 404           |

括号目录 `(protected)`、`(resource)` 和 `(self-service)` 是 Route Group，只用于组织代码和布局，不进入 URL。

### 5.2 薄路由入口

`page.tsx` 通常只做 URL 到 feature 页面的映射，不承载业务逻辑。

```text
app/.../users/page.tsx
    -> features/users/UsersPage.tsx
```

这样路由职责和业务页面职责清晰分离，也使 Next.js 能按路由生成独立代码块。

### 5.3 布局组合

根布局 `src/app/layout.tsx` 的组合顺序：

```text
RuntimeConfigGate
    -> SessionLifecycleBridge
    -> BzUiRoot
    -> 当前路由页面
```

受保护布局 `src/app/admin/(protected)/layout.tsx` 的组合顺序：

```text
AdminRuntimeBootstrap
    -> AdminAccessBoundary
    -> AdminResourceBoundary
    -> AdminShell
    -> 当前后台页面
```

这体现了 React 的组件组合，也体现了启动顺序和安全边界。

### 5.4 导航

- `redirect()`：在 Server Component 中直接重定向，见 `src/app/page.tsx`。
- `useRouter().push()`：在客户端事件中导航，见登录页。
- `window.location.replace()`：完整替换当前地址，见会话结束跳转。
- `Link`：声明式客户端链接，见 `AdminShell.tsx`。

### 5.5 静态导出

`next.config.ts` 配置 `output: "export"`，生产构建生成 `dist/` 静态文件。

由此带来的重要结论：

- 生产环境没有 Next.js Node 服务。
- 页面不能依赖每次请求时的 SSR。
- 项目不使用 Server Action。
- 项目不使用 Next Route Handler 提供后端 API。
- 浏览器数据通过 Java 后端 API 获取。
- API 同源代理由开发服务器、Preview 或 Nginx 提供。

静态导出不等于只有一个页面文件。真实 App Router 路由仍会生成各自的页面产物和代码分块。

### 5.6 前端守卫不是安全边界

`AdminAccessBoundary` 和 `AdminResourceBoundary` 控制页面是否展示和请求是否启动，但用户可以控制自己的浏览器。

真正的数据安全必须由后端继续验证：

- Cookie 会话。
- 用户类型。
- API 权限码。
- CSRF Token。
- 数据访问范围。

## 6. 项目代码架构

### 6.1 顶层目录

| 目录           | 职责                                          |
| -------------- | --------------------------------------------- |
| `src/app`      | Next 路由、布局、错误边界和应用组合           |
| `src/features` | 按业务功能组织页面、API、Model、权限和 UI     |
| `src/shared`   | 不带业务语义的 Transport、Hook、类型和通用 UI |
| `src/runtime`  | 会话、SSE、Push、Service Worker、运行时桥接   |
| `src/assets`   | 静态源码资源                                  |
| `src/test`     | 单元测试公共配置                              |

依赖方向：

```text
app -> runtime/features/shared
runtime -> features/shared
features -> shared
shared -> 不反向依赖业务层
```

规则由 `scripts/check-architecture.mjs` 和 `eslint.config.mjs` 自动检查。

### 6.2 feature 垂直切片

以 `src/features/users` 为例：

```text
users/
├─ api/
│  ├─ client.ts       请求、URL、Payload 转换
│  └─ payload.ts      Java 后端网络 DTO
├─ model/
│  └─ types.ts        前端稳定业务模型
├─ ui/                用户功能专属组件
├─ permissions.ts     用户功能权限码
└─ UsersPage.tsx      页面用例编排
```

与按 `components/api/types` 横向堆放相比，垂直切片使修改一个功能时，大部分变更留在同一个 feature 中。

### 6.3 Payload 与 Model 分离

- `api/payload.ts`：描述后端原始数据。
- `model/types.ts`：描述前端希望稳定使用的数据。
- `api/client.ts`：在两者之间转换。

`src/features/users/api/client.ts:21-34` 的 `toUserEntry` 是典型转换函数。

这种设计可以把后端的可空字段、数值 ID、命名差异限制在 API 层，不让 UI 到处处理网络细节。

### 6.4 shared 与 runtime 的区别

- `shared` 是稳定、通用、尽量纯粹的能力，例如分页类型、Store、HTTP 和 UI。
- `runtime` 管理浏览器和应用级生命周期，例如 SSE 连接、Push 订阅、跨标签页会话和后台启动。

普通 feature 不能自己创建 EventSource、BroadcastChannel 或管理 Service Worker，这些规则在 ESLint 中有明确限制。

## 7. 状态管理

### 7.1 组件本地状态

`src/features/users/UsersPage.tsx:62-81` 使用多个 `useState` 保存：

- 查询面板是否展开。
- 当前批量动作。
- 已选择 ID。
- 编辑抽屉状态。
- 当前编辑对象。

这些数据只服务当前页面，不需要进入全局 Store。

### 7.2 通用分页状态

`src/shared/hooks/useAdminPagedQuery.ts` 封装了后台列表共有逻辑：

- 草稿筛选与已应用筛选分离。
- 页码和页大小。
- loading 和 error。
- 自动请求与手动刷新。
- 取消旧请求。
- 丢弃过期响应。
- 删除数据后的末页回退。
- 权限失效时清除敏感数据。

用户输入筛选条件时只更新 `draftFilters`，点击搜索后才复制为 `appliedFilters` 并发起请求。

### 7.3 外部 Store

`src/shared/lib/store.ts` 使用闭包和 `Set<Listener>` 实现轻量 Store：

```text
getState   读取状态
setState   更新状态并通知监听器
subscribe  订阅更新并返回取消订阅函数
```

React 组件通过 `src/shared/hooks/useStoreValue.ts` 的 `useSyncExternalStore` 订阅。

典型业务 Store：

- `src/features/auth/model/auth-store.ts`
- `src/features/resources/model/resource-store.ts`
- `src/features/notifications/model/notification-store.ts`

### 7.4 状态选择原则

```text
只影响当前控件 -> useState
当前页面多个状态联动 -> useReducer 或自定义 Hook
跨组件共享业务状态 -> 外部 Store
组件子树 UI 服务 -> Context
浏览器基础设施生命周期 -> runtime 模块级状态
```

## 8. HTTP、异步与安全

### 8.1 统一 Transport

`src/shared/transport/http.ts` 是普通 API 请求的唯一底层入口，提供：

- `get<T>`
- `post<T>`
- `put<T>`
- `patch<T>`
- `del<T>`
- `getJson<T>`
- `getBlob`
- `getResponse`

feature API 不直接调用 `fetch`。例如 `src/features/users/api/client.ts` 只依赖 shared Transport。

### 8.2 一次用户分页请求

```text
UsersPage 调用 useAdminPagedQuery
    -> query 回调调用 pageUsers
    -> pageUsers 组装筛选与分页 Payload
    -> post<PageResult<UserPayload>>
    -> request() 添加 Header、Cookie、CSRF、超时和 signal
    -> fetch(resolveApiUrl(endpoint))
    -> 校验响应信封
    -> UserPayload 转换为 UserEntry
    -> Hook 更新页面状态
```

### 8.3 Cookie 会话

`request()` 使用 `credentials: "same-origin"`。浏览器自动携带同源 Cookie。

项目不在 localStorage 保存长期 Bearer Token，也不由 JavaScript 读取 HttpOnly 会话 Cookie。

### 8.4 CSRF

写请求在 `src/shared/transport/http.ts:103-124,205-207` 自动完成：

1. 检查 CSRF Cookie。
2. 不存在时请求 `/admin/auth/csrf`。
3. 读取服务端写入的 CSRF Cookie。
4. 添加 `X-CSRF-Token` 请求头。

页面和 feature API 不需要重复实现 CSRF。

Cookie 登录、CORS 和 CSRF 是不同问题：

- Cookie 登录解决“用户是谁”。
- CORS 控制其他 Origin 能否读取响应。
- CSRF 防止其他站点诱导浏览器带着 Cookie 发起写操作。

### 8.5 AbortController 与请求竞态

`AbortController` 用于发出取消信号：

```ts
const controller = new AbortController();
controller.abort();
```

`useAdminPagedQuery` 同时使用 AbortController 和请求序号：

- AbortController 尽量停止旧工作。
- sequence/generation 防止旧 Promise 即使完成也覆盖新结果。

取消请求不会回滚已经到达后端的事务，这是另一个重要边界。

### 8.6 超时和错误分类

Transport 将错误区分为：

| 错误               | 含义                                  |
| ------------------ | ------------------------------------- |
| `HttpError`        | HTTP 状态不是 2xx                     |
| `ApiError`         | HTTP 成功，但业务响应 `success=false` |
| `network`          | 网络失败                              |
| `timeout`          | 前端超时                              |
| `invalid-response` | JSON 或响应信封格式错误               |
| `AbortError`       | 调用方主动取消                        |

取消通常是正常控制流，不应提示成业务失败。

### 8.7 并发 401 与会话结束

多个请求同时返回 401 时，Transport 会合并为一次会话终止操作。

`src/runtime/session/session-service.ts:17-47` 的 `endSession` 顺序是：

```text
停止 Admin Runtime
    -> 清空通知
    -> 清空资源权限
    -> 清空认证状态
    -> 广播到其他标签页
    -> 跳转登录页
```

`endingPromise` 保证并发调用基本幂等。

### 8.8 运行时配置

静态构建需要在不同环境连接不同后端，因此项目使用公开的 `/runtime-config.json`，而不是把地址永久写入 bundle。

`src/shared/transport/runtime-config.ts` 负责：

- 从 `unknown` 开始验证配置。
- 只允许同源路径。
- 拒绝绝对 URL、查询参数、Hash 和未知字段。
- 合并并发加载。
- 拼装最终 API URL。

`src/runtime/config/RuntimeConfigGate.tsx` 在配置成功前不会挂载认证和业务运行时。

运行时配置是公开数据，绝不能存放 Token、密码或私钥。

## 9. 权限模型

### 9.1 认证边界

`src/app/admin/(protected)/_components/AdminAccessBoundary.tsx` 检查：

- 会话是否加载完成。
- 用户是否登录。
- 用户类型是否为 ADMIN。
- 是否必须先修改密码。

### 9.2 资源边界

`src/app/admin/(protected)/_components/AdminResourceBoundary.tsx` 检查当前路径是否对应可访问菜单资源。

资源 Store 默认拒绝未知、禁用、缺少父级或形成循环的资源，见 `src/features/resources/model/resource-store.ts`。

### 9.3 按钮权限

`src/features/users/permissions.ts` 集中声明用户功能权限码。

`src/features/users/UsersPage.tsx:82-93` 使用 `usePermission` 获取响应式权限，再决定按钮是否展示、操作函数是否可以执行、分页请求是否启用。

这种“展示检查 + 操作检查”可减少异常 UI 路径，但仍不能替代后端权限校验。

## 10. 浏览器实时能力

这部分比普通页面开发复杂，建议掌握 React、HTTP 和异步竞态后再学习。

### 10.1 Admin Runtime 启动链

`src/runtime/admin-runtime.ts:57-92` 按顺序执行：

```text
loadAuthSession
    -> 确认 ADMIN 会话仍有效
    -> loadResources
    -> 再次确认会话未变化
    -> loadUnreadNotifications
    -> startSseRuntime
    -> startWebPushRuntime
```

每次异步阶段后都验证 generation 和用户 ID，避免旧会话初始化污染新会话。

### 10.2 SSE

SSE 是服务器到浏览器的单向长连接，主要文件：

- `src/runtime/sse/sse-client.ts`：申请短期 SSE ticket。
- `src/runtime/sse/sse-runtime.ts`：连接、重连、选主和广播。
- `src/runtime/sse/sse-message-parser.ts`：校验消息。
- `src/runtime/sse/sse-store.ts`：连接状态。

浏览器原生 EventSource 不方便添加自定义认证 Header，所以项目先通过普通受保护 API 获取短期 ticket，再创建 EventSource。

多标签页只选一个 leader 建立 SSE，其余标签页通过 BroadcastChannel 接收 leader 转发的消息，减少重复连接。

### 10.3 BroadcastChannel

BroadcastChannel 用于同源标签页之间通信，不经过服务器，也不是持久消息队列。

典型用途：

- `src/runtime/session/session-channel.ts`：一个标签页登出后通知其他标签页。
- `src/runtime/sse/sse-runtime.ts`：leader 将 SSE 消息分发给 follower。

会话同步还使用 localStorage 的 `storage` 事件作为降级方案。

### 10.4 Web Push、Service Worker 和 Notification

三个概念需要分开：

| 概念             | 作用                              |
| ---------------- | --------------------------------- |
| Web Push         | 推送服务向浏览器订阅地址投递消息  |
| Service Worker   | 页面关闭或进入后台后仍可处理 Push |
| Notification API | 展示操作系统通知                  |

主要文件：

- `src/runtime/push/push-runtime.ts`
- `src/runtime/push/device-id.ts`
- `src/features/notifications/api/push-client.ts`
- `public/sw.js`

当前 Service Worker 只处理 Push 和通知点击，没有离线缓存、请求拦截或 PWA precache。

Web Push 和安全 Cookie 在生产环境通常都要求 HTTPS。Docker 中的 Nginx 只监听内部 HTTP，生产环境需要外层代理终止 TLS。

## 11. 典型业务实现

### 11.1 用户列表与分页

建议按以下顺序阅读：

1. `src/app/admin/(protected)/(resource)/users/page.tsx`
2. `src/features/users/model/types.ts`
3. `src/features/users/api/payload.ts`
4. `src/features/users/api/client.ts`
5. `src/features/users/UsersPage.tsx`
6. `src/shared/hooks/useAdminPagedQuery.ts`
7. `src/shared/transport/http.ts`

你会看到一个完整的“路由 -> 页面 -> Hook -> API -> Transport”调用链。

### 11.2 表单提交

登录页面 `src/features/auth/ui/AuthLoginPageCard.tsx` 展示了：

- Props。
- 受控输入。
- `useState`。
- `useRef` 聚焦输入框。
- `useEffect`。
- `async/await`。
- `try/catch/finally`。
- 客户端路由跳转。
- 开放重定向防护。

密码修改页 `src/features/profile/PasswordPage.tsx` 还展示了手工校验和 ref 防重复提交锁。

### 11.3 文件预览与下载

`src/features/system-files/api/client.ts` 展示了普通 JSON 之外的响应：

- Blob：用于文件内容。
- 原始 Response：用于读取响应 Header 和文件名。
- Object URL：浏览器临时访问内存中的文件数据。

### 11.4 页面错误边界

`src/app/admin/(protected)/error.tsx` 捕获路由子树渲染错误并提供重试。

它不会自动捕获事件回调、定时器或未处理的 Promise 异常，因此异步业务函数仍需自己处理错误。

## 12. 测试

### 12.1 Vitest

配置见 `vitest.config.ts`：

- 使用 jsdom 模拟浏览器 DOM 环境。
- 加载 `src/test/setup.ts`。
- 匹配 `src/**/*.test.ts` 和 `src/**/*.test.tsx`。
- 每个测试后恢复 mock。

### 12.2 Testing Library

Testing Library 从用户可观察行为测试 React 组件和 Hook。

典型测试：

| 文件                                                                 | 主要知识点                                 |
| -------------------------------------------------------------------- | ------------------------------------------ |
| `src/shared/hooks/useAdminPagedQuery.test.tsx`                       | `renderHook`、`act`、请求竞态、Strict Mode |
| `src/shared/transport/http.test.ts`                                  | fetch mock、Cookie、CSRF、401、错误分类    |
| `src/app/admin/(protected)/_components/AdminAccessBoundary.test.tsx` | Router mock、认证跳转                      |
| `src/features/resources/model/resource-store.test.ts`                | 纯状态与权限逻辑                           |

jsdom 不是真实浏览器，不能完整验证 Service Worker、Push、原生 EventSource、多标签页和真实 Cookie 安全策略。

### 12.3 Playwright

配置见 `playwright.config.ts`，当前使用 Desktop Chrome。

`e2e/fixtures/admin-api.ts` 在浏览器层拦截 `/api/**`，为每个测试提供确定的后端响应。

`e2e/admin.spec.ts` 覆盖：

- 匿名访问跳转登录。
- 管理员登录。
- 无菜单权限显示 403。
- 用户筛选和分页。
- 新增用户后刷新列表。

Playwright 比 jsdom 更接近真实浏览器，但当前 E2E 使用 API mock，不是完整的前后端数据库集成测试。

### 12.4 测试分层

```text
纯函数测试       快、定位准确
Hook/组件测试    验证 React 状态和交互
Transport 测试   验证网络契约和错误处理
Playwright E2E   验证浏览器中的完整用户流程
后端集成测试     验证真实权限、数据库和事务
```

## 13. 工程化

### 13.1 Node.js 与 npm

`package.json` 固定：

- Node `24.16.0`
- npm `11.13.0`

项目推荐 `npm ci`：严格按 `package-lock.json` 安装，适合 CI 和可重复构建。

`npm install` 可能重新解析版本范围并修改 lockfile。

### 13.2 常用命令

在 `web-admin/` 目录执行：

| 命令                         | 用途                               |
| ---------------------------- | ---------------------------------- |
| `npm ci`                     | 按 lockfile 安装依赖               |
| `npm run dev`                | 启动 Next.js 开发服务              |
| `npm run format:check`       | 检查格式                           |
| `npm run lint`               | 执行 ESLint                        |
| `npm run typecheck`          | 生成路由类型并执行 TypeScript 检查 |
| `npm run check:architecture` | 检查目录和依赖边界                 |
| `npm run test`               | 执行 Vitest                        |
| `npm run quality`            | 执行格式、lint、类型、架构和单测   |
| `npm run build`              | 生成 `dist/` 静态产物              |
| `npm run check:bundle`       | 检查构建体积和路由分块             |
| `npm run preview`            | 本地预览已有 `dist/`               |
| `npm run test:e2e`           | 执行 Playwright E2E                |

`npm run quality` 不包含依赖审计、生产构建、bundle 检查和 E2E。

### 13.3 ESLint

`eslint.config.mjs` 负责：

- 禁止 `var` 和显式 `any`。
- 禁止未使用 import。
- 统一 import 顺序。
- 限制深层相对路径。
- 限制 feature、model、shared 和 Bz UI 的依赖方向。
- 限制 EventSource、BroadcastChannel、Service Worker 等只能由 runtime 管理。

命令使用 `--max-warnings=0`，所以 warning 也会导致检查失败。

### 13.4 Prettier

`prettier.config.mjs` 负责代码排版，例如行宽、缩进、引号、分号和换行。

Prettier 不检查业务正确性，ESLint 也不会自动执行 Prettier，两者职责不同。

### 13.5 TypeScript 检查

`npm run typecheck` 先执行 Next 路由类型生成，再运行 `tsc --noEmit`。

主 `tsconfig.json` 主要检查 `src/**/*.ts` 和 `src/**/*.tsx`。E2E、工具脚本和配置文件还需要依靠对应工具执行与 ESLint 检查。

### 13.6 架构检查

`scripts/check-architecture.mjs` 使用 TypeScript AST 检查：

- 顶层目录是否合法。
- shared、features、runtime、app 的依赖方向。
- 跨 feature 是否只访问 public 契约。
- 是否出现 `any` 或 TypeScript 检查绕过。
- 普通 `fetch` 是否只位于 Transport。

这相当于把架构规则变成可执行测试，而不只写在文档里。

### 13.7 Bundle Budget

`bundle-budget.json` 和 `scripts/check-bundle-budget.mjs` 限制：

- 登录路由首次加载体积。
- 后台路由首次加载体积。
- 单个 JavaScript chunk 体积。
- 每个后台路由是否拥有独立 page chunk。

它检测下载体积和分块，不等于浏览器性能测试，不能代替真实的加载、解析和交互性能分析。

## 14. 构建与部署

### 14.1 开发环境

```text
浏览器请求 /api/**
    -> Next.js 开发服务器 rewrite
    -> ADMIN_API_UPSTREAM
    -> Java 后端
```

配置见 `.env.example` 和 `next.config.ts`。

### 14.2 静态构建

`npm run build` 使用 webpack 生成 `dist/`。生产运行时不再需要 Node.js。

### 14.3 Preview

`preview.mjs` 模拟生产环境的关键行为：

- 静态路由。
- 同源 API 代理。
- 运行时配置。
- 安全响应头。
- 缓存策略。

Preview 不会自动执行 build，首次运行前必须先有 `dist/`。

### 14.4 Docker 与 Nginx

Docker 镜像只复制已经生成的 `dist/`，不在镜像内运行 npm 构建。

Nginx 负责：

- 提供静态文件。
- 代理 `/api/**`。
- 提供 `/runtime-config.json`。
- 为 SSE 关闭代理缓冲。
- 设置缓存和安全响应头。

容器使用 `API_UPSTREAM`，本地开发和 Preview 使用 `ADMIN_API_UPSTREAM`，两者名称不同。

## 15. 项目没有使用的常见技术

| 常见技术               | 本项目替代方案                              |
| ---------------------- | ------------------------------------------- |
| Redux/Zustand/MobX     | 自研 `createStore` + `useSyncExternalStore` |
| Axios                  | 原生 `fetch` + shared Transport             |
| React Query/SWR        | `useAdminPagedQuery` 和 feature Service     |
| React Hook Form/Formik | 受控 State + 手工提交                       |
| Zod/Yup                | 普通类型守卫和校验函数                      |
| Server Action          | 浏览器 API Client 调用 Java 后端            |
| Next Route Handler     | Preview/Nginx 代理 Java 后端                |
| Middleware 认证        | 客户端权限边界 + 后端权限校验               |
| React class 组件       | 函数组件和 Hook                             |
| 离线 PWA               | 当前 Service Worker 只处理 Push             |

不使用并不代表这些技术不好，只表示学习当前项目时不需要先掌握它们。

## 16. 推荐学习顺序

### 第一阶段：语言与模块

1. 阅读 `package.json`，理解依赖和 scripts。
2. 阅读 `src/features/users/model/types.ts`，学习 interface、type、联合类型。
3. 阅读 `src/features/users/api/client.ts`，学习 import/export、async/await、map/filter 和泛型。
4. 阅读 `src/shared/transport/runtime-config.ts`，学习 unknown、类型收窄和运行时校验。

### 第二阶段：React 基础

1. 阅读 `src/app/admin/login/page.tsx`，认识最小组件。
2. 阅读 `src/features/auth/ui/AuthLoginPageCard.tsx`，学习 Props、State、Effect、Ref 和事件。
3. 阅读 `src/shared/ui/bz/BzInput.tsx`，学习受控组件和 imperative ref。
4. 阅读 `src/features/users/UsersPage.tsx`，学习页面组件如何编排多个子组件。

### 第三阶段：Next.js 和架构

1. 阅读 `src/app/layout.tsx`。
2. 阅读 `src/app/admin/(protected)/layout.tsx`。
3. 对照一个 `page.tsx` 和对应的 feature Page。
4. 阅读 `README.md` 的目录边界。
5. 阅读 `scripts/check-architecture.mjs`，理解规则如何自动执行。

### 第四阶段：状态与异步

1. 阅读 `src/shared/lib/store.ts`。
2. 阅读 `src/shared/hooks/useStoreValue.ts`。
3. 阅读 `src/shared/hooks/useAdminPagedQuery.ts`。
4. 对照 `useAdminPagedQuery.test.tsx` 理解请求竞态和 Effect 清理。

### 第五阶段：网络与安全

1. 阅读 `src/shared/transport/http.ts`。
2. 对照 `src/shared/transport/http.test.ts`。
3. 阅读 `src/runtime/session/session-service.ts`。
4. 阅读认证和资源权限边界。

### 第六阶段：浏览器高级能力

1. 阅读 `src/runtime/admin-runtime.ts`。
2. 阅读 `src/runtime/session/session-channel.ts`。
3. 阅读 `src/runtime/sse/sse-runtime.ts`。
4. 阅读 `src/runtime/push/push-runtime.ts` 和 `public/sw.js`。

### 第七阶段：测试与部署

1. 阅读 `vitest.config.ts` 和典型单测。
2. 阅读 `playwright.config.ts` 和 `e2e/admin.spec.ts`。
3. 阅读 `next.config.ts` 和 `preview.mjs`。
4. 阅读 `Dockerfile`、`docker-runtime-config.sh` 和 `nginx.conf`。

初学时不要先阅读大型 `AdminShell.tsx` 或复杂日期、表格选择组件。先掌握组件、Props、State、Effect、受控输入和异步请求，再研究复杂交互实现。

## 17. 建议练习

这些练习只用于学习，可以先在阅读时手工推演，不必立刻修改项目。

1. 从 `/admin/users` 的 `page.tsx` 开始，手工画出到 `fetch` 的完整调用图。
2. 为 `UserStatus` 增加一个假想状态，记录 TypeScript 会在哪些位置提示修改。
3. 在纸上推演第一页请求慢于第二页请求返回时，`useAdminPagedQuery` 如何阻止旧数据覆盖。
4. 比较 `draftFilters` 和 `appliedFilters`，解释为什么输入一个字符不会立刻发请求。
5. 比较组件本地 State、外部 Store 和 Context，分别列出最适合它们的数据。
6. 阅读 401 测试，解释为什么并发多个 401 只结束一次会话。
7. 比较 SSE 与 Web Push，说明页面关闭后哪一种仍可能收到消息。
8. 比较 Vitest/jsdom 和 Playwright，说明各自能发现什么问题。
9. 比较 `ADMIN_API_UPSTREAM` 与 `API_UPSTREAM` 的使用阶段。
10. 执行 `npm run quality`，逐项确认每个子命令检查的内容。

## 18. Java 开发者对照表

| 前端概念             | 可参考的 Java 概念         | 关键差异                               |
| -------------------- | -------------------------- | -------------------------------------- |
| TypeScript interface | Java interface/DTO 结构    | TS interface 运行时消失                |
| 字符串联合类型       | Java enum                  | TS 联合通常不生成运行时对象            |
| 泛型 `<T>`           | Java 泛型                  | TS 类型结构化且会被擦除                |
| ES Module            | Java package/module import | 每个模块还可拥有共享顶层状态           |
| 闭包                 | 带私有字段的对象           | 不需要显式 class                       |
| Promise              | `CompletableFuture`        | `await` 由事件循环恢复执行             |
| React Props          | 方法参数/只读 DTO          | 每次渲染重新传入                       |
| React State          | 对象字段                   | 更新后驱动重新渲染                     |
| Hook                 | 可组合状态与生命周期函数   | 必须遵守 React 调用规则                |
| Context              | 组件树范围依赖提供         | 只沿 React 子树传播                    |
| reducer              | 状态机/命令处理器          | 返回新状态而非修改旧状态               |
| Next layout          | 路由层公共壳体             | 通过组件树嵌套组合                     |
| feature 切片         | 按领域组织的模块           | 同时包含 UI、API 和前端 Model          |
| API Client           | HTTP Client/Adapter        | 在浏览器运行，受同源和 Cookie 规则影响 |
| Service Worker       | 独立事件处理运行时         | 不属于页面线程，生命周期由浏览器管理   |

类比只用于建立初步认识，不能把 React 组件生命周期、JavaScript 事件循环和浏览器安全模型直接套用成 Java 服务端模型。

## 19. 阅读时应持续追问的问题

阅读任意一段前端代码时，可以依次回答：

1. 这段代码运行在构建期、Node.js、浏览器页面还是 Service Worker？
2. 数据来自 Props、State、Store、URL、配置文件还是后端 API？
3. 数据变化后是否需要重新渲染？
4. 外部数据是否从 `unknown` 开始验证？
5. 异步操作是否会与下一次请求产生竞态？
6. Effect 是否正确清理请求、监听器、Timer 或连接？
7. 这里的权限检查是 UI 体验还是后端安全边界？
8. 当前依赖方向是否符合 `app -> runtime/features -> shared`？
9. 该逻辑应该放在组件、Hook、feature Service、Store 还是 runtime？
10. 应该由 Vitest、Playwright 还是后端测试验证？

能够稳定回答这些问题后，就已经掌握了本项目大部分前端设计思想。
