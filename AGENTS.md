Breezy Project AI Agent Guide (AGENTS.md)

> **重要说明：本仓库所有后续对话和交互均使用简体中文。**

本文件用于规范在 Breezy 项目中运行的 AI Agent 的行为，包括：

- 项目结构理解
- 构建与运行方式
- 架构与分层约束
- 代码规范
- AI 开发流程
- 需求处理与任务拆分机制
- README 文档同步规则

本规范旨在确保 AI 在开发过程中：

- 不破坏现有架构
- 不违反 DDD 分层
- 不引入混乱的代码结构
- 保持可维护性
- 保证开发流程可控

---

## 1. 项目概览

Breezy 是一个全栈工具型系统。整体采用 **前后端分离架构**，并遵循 **DDD（领域驱动设计）分层思想**。

当前仓库结构（已按多模块重构）：

```
.
├─ dependencies/           # 依赖管理 BOM 模块
├─ server/                 # 后端聚合模块（pom）
│  ├─ framework/           # 框架层模块
│  ├─ system/              # 系统域模块
│  ├─ business/            # 业务域模块（datasource/storage/...）
├─ web/                    # Vue 前端
├─ pom.xml                 # Maven 父工程
├─ README.md               # 项目说明与需求记录
└─ AGENTS.md               # AI Agent 开发规范
```

技术栈：

### 后端

- Java 21
- Spring Boot 3.5.x
- Spring MVC
- JPA
- JOOQ
- JWT
- Log4j2
- HikariCP

支持数据库：

- H2
- MySQL
- PostgreSQL
- Oracle

### 前端

- Vue 3
- Vite
- TypeScript
- Bz UI（`web/src/components/bz` 自研组件体系）

---

## 2. 构建与运行

### 2.1 后端编译、测试、运行

在项目根目录执行。

#### Linux / macOS

* **全量编译与打包**:
  
  ```bash
  ./mvnw clean package -pl server -am
  ```
* **启动应用**:
  
  ```bash
  ./mvnw spring-boot:run -pl server/business
  ```
* **运行全部测试**:
  
  ```bash
  ./mvnw test -pl server -am
  ```
* **运行单测**（按模块）:
  
  ```bash
  ./mvnw test -pl server/system -Dtest=ClassName
  ./mvnw test -pl server/system -Dtest=ClassName#methodName
  ```

#### Windows

* **全量编译与打包**:
  
  ```powershell
  .\mvnw.cmd clean package -pl server -am
  ```
* **启动应用**:
  
  ```powershell
  .\mvnw.cmd spring-boot:run -pl server/business
  ```
* **运行全部测试**:
  
  ```powershell
  .\mvnw.cmd test -pl server -am
  ```
* **运行单测**（按模块）:
  
  ```powershell
  .\mvnw.cmd test -pl server/system -Dtest=ClassName
  .\mvnw.cmd test -pl server/system -Dtest=ClassName#methodName
  ```

### 2.2 前端开发

进入 `web/` 目录。

```bash
cd web
# 安装依赖
npm install
# 日常开发
npm run dev
# 检查与构建
npm run build
# 预览构建
npm run preview
```

### 2.3 AI 执行构建验证

```bash
# 后端（根目录）
./mvnw compile -pl server -am

# 前端
cd web && npm run build
```

当 AI 修改代码后，必须确保：

- 无编译错误
- 无类型错误

---

## 3. DDD 分层规范

后端代码采用 **DDD 分层结构**。

当前核心包结构（与实际代码一致）：

```
com.corwin.framework    # framework 模块
com.corwin.system       # system 模块
com.corwin.datasource   # business 模块
com.corwin.storage      # business 模块
com.corwin.jsonfmt      # business 模块
com.corwin.schemaforge  # business 模块
com.corwin.reminder     # business 模块
com.corwin.web          # business 模块
```

典型业务模块结构（允许按场景增减子包）：

```
module
 ├─ interfaces
 │   └─ web
 │       ├─ req
 │       └─ res
 │
 ├─ application
 │   ├─ service
 │   ├─ command
 │   └─ view
 │
 ├─ domain
 │   ├─ model
 │   └─ repo
 │
 └─ infrastructure
     ├─ persistence
     ├─ security
     ├─ scheduling
     └─ config
```

### 3.1 interfaces 层

职责：

- 提供 REST API
- 参数校验
- 返回 DTO

禁止：

- 编写业务逻辑
- 直接调用 Repository

只允许调用：

```
Application Service
```

### 3.2 application 层

职责：

- 用例编排
- 调用领域模型
- 调用 Repository（领域仓储接口）

禁止：

- 直接操作数据库连接细节
- 写绕过仓储抽象的业务 SQL

### 3.3 domain 层

职责：

- 表达业务规则
- 表达领域行为

特点：

- 不依赖 Spring
- 不依赖基础设施实现细节

### 3.4 infrastructure 层

职责：

- 持久化实现（JPA/JDBC 等）
- 安全认证实现
- 调度实现
- 外部系统调用

### 3.4.1 分页与动态查询实现约定

AI 在实现带有分页、筛选、搜索、排序的查询接口时必须遵守以下规则：

- 只要查询存在 **分页** 或 **动态查询条件**，默认优先使用 `XSQL` 实现
- 不要优先使用 Spring Data JPA 方法名派生查询、`@Query` 拼接分页查询来承载复杂列表页
- 简单主键查询、唯一键查询、固定条件存在性判断、少量静态条件查询可以继续使用 JPA Repository
- 一旦进入后台列表页、条件筛选页、统计检索页等场景，优先在 `infrastructure.persistence` 中使用 `XSql` / `XNativeQuery` / `XTableQuery`
- 分页查询应在仓储实现层统一处理：条件构造、排序映射、分页执行、`PageData` 返回
- Application Service 负责用例编排，不承载 SQL 细节

### 3.5 DDD 重要规则

AI 必须遵守：

1. Controller 不写业务逻辑
2. Application Service 负责用例编排
3. Domain Model 承载行为
4. Infrastructure 实现技术细节

---

## 4. 框架与业务模块依赖规则

本项目存在两类代码：

```
com.corwin.framework      # 框架层
com.corwin.(system|...)   # 业务层
```

### 4.1 依赖方向

允许：

```
system -> framework
business -> framework
business -> system
app -> business
```

禁止：

```
framework -> system
framework -> business
framework -> 任意业务域包
```

即：**框架层不得依赖业务层。**

### 4.2 设计原则

框架层负责：

- 抽象接口
- 基础能力
- 通用组件

业务层负责：

- 具体业务实现

### 4.3 示例

`TokenService<T>` 定义在：

```
com.corwin.framework.web.auth
```

业务实现 `JwtTokenService` 位于：

```
com.corwin.system.infrastructure.security
```

### 4.4 Auto Configuration 机制

框架层可以通过：

```
Spring Boot Auto Configuration
```

强制要求业务层提供实现。

### 4.5 AI 开发约束

AI 在新增能力时必须遵守：

如果属于框架能力，放在：

```
com.corwin.framework
```

如果属于业务能力，放在：

```
com.corwin.system / com.corwin.datasource / ...
```

框架层禁止：

- 依赖业务实体
- 依赖业务模块
- 包含业务逻辑

代码审查规则：

AI 修改框架代码时必须检查是否引用了 `com.corwin.system`、`com.corwin.datasource`、
`com.corwin.storage`、`com.corwin.jsonfmt`、`com.corwin.schemaforge`、`com.corwin.reminder`、`com.corwin.web`，
若存在则必须移除。

---

## 5. Java 代码规范

本项目使用 **Java 21**。

AI 在编写 Java 代码时必须遵守以下规范。

---

### 5.1 语言特性

优先使用现代 Java 特性：

- `record`
- `sealed`
- `switch pattern`
- `instanceof pattern`
- `var`（仅在不影响可读性时）

示例：

```java
public record UserView(
        String id,
        String name
) {}
```

### 5.2 DTO 类型规范

DTO 类型使用固定后缀：

| 类型     | 后缀    |
| -------- | ------- |
| 请求 DTO | Req     |
| 响应 DTO | Res     |
| 命令对象 | Command |
| 视图对象 | View    |

示例：

```java
CreateUserReq
CreateUserCommand
UserView
UserRes
```

### 5.3 record 优先原则

DTO / Command / View **优先使用 record**。

示例：

```java
public record CreateUserReq(
        String username,
        String password
) {}
```

### 5.4 Lombok 使用规范

若必须使用 class：

优先使用 Lombok：

```java
@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
```

禁止：

```java
手写 getter/setter
```

### 5.5 依赖注入

必须使用 **构造器注入**。

推荐：

```java
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repo;

}
```

禁止：

```java
@Autowired
private UserRepository repo;
```

### 5.6 Import 规范

禁止：

```
import java.util.*;
```

必须：

```java
import java.util.List;
import java.util.Map;
```

### 5.7 Controller 规范

Controller 必须：

- 返回 `ApiResponse<T>`
- 使用 `@Permission`
- 不包含业务逻辑

示例：

```java
@GetMapping("/users")
@Permission("user.list")
public ApiResponse<List<UserView>> list() {
    return ApiResponse.ok(service.listUsers());
}
```

### 5.8 异常处理

Controller **不得捕获异常并吞掉**。

统一使用：

```java
BizException
BizAssert
```

示例：

```java
BizAssert.notNull(user, "用户不存在");
```

### 5.9 日志规范

统一使用：

```java
@Slf4j
```

禁止：

```java
System.out.println
printStackTrace
```

### 5.10 作者注释

所有 Java 文件必须包含作者注释：

```java
/**
 * @author Corwin yyyy/M/d
 */
```

必须位于文件顶部。

---

## 6. 前端代码规范

前端技术栈：

- Vue 3
- Composition API
- TypeScript
- Bz UI（自研）

### 6.1 Vue 组件结构

统一使用：

```vue
<script setup lang="ts">
```

示例：

```vue
<script setup lang="ts">
import { ref } from "vue";

const users = ref([]);
</script>
```

### 6.2 类型规范

严禁使用：

```typescript
any
```

必须定义类型：

```typescript
interface UserView {
  id: string;
  name: string;
}
```

### 6.3 组件命名

组件文件：

```
PascalCase
```

示例：

```
UserTable.vue
DataSourceList.vue
```

### 6.4 变量命名

变量与函数：

```
camelCase
```

示例：

```
loadUsers
createDataSource
fetchDatabaseList
```

### 6.5 UI 组件规范

统一使用 **Bz UI 组件**（`Bz*`）。

常用组件：

```
BzButton
BzTable
BzForm
BzDialog
BzPagination
BzTooltip
BzInput
BzSelect
```

禁止：

- 页面内重复造轮子（同类交互必须优先复用已有 Bz 组件）
- 引入其他 UI 框架

### 6.6 图标规范

统一使用：

```
BzIcon（优先文本/系统图标）
```

禁止：

```
私有图标实现导致风格割裂
```

### 6.7 主题颜色

统一主题色：

```
#3B82F6
```

### 6.8 页面结构

页面结构统一：

```
AppHeader
内容区
StatusBar
```

内容区 **独立滚动**。

---

## 7. AI 开发工作流程

AI 在进行代码开发时必须遵循以下流程。

### 7.1 理解需求

首先：

```
理解用户需求
```

然后：

```
搜索相关代码
```

工具：

- grep
- 文件搜索
- 阅读 DTO / Service / Controller

### 7.2 分析影响

判断是否涉及：

- 数据库结构
- 领域模型
- API 接口
- 前端页面

### 7.3 实现功能

实现代码时必须：

- 遵循 DDD 分层
- 保持代码风格一致
- 不破坏框架结构

### 7.4 自我验证

AI 必须进行基本验证：

后端：

```
./mvnw compile -pl server -am
```

前端：

```
cd web && npm run build
```

### 7.5 清理代码

提交前必须：

- 删除未使用 import
- 修复格式
- 保持缩进一致

---

## 8. 需求处理模式

AI 处理需求时存在 **两种模式**。

### 8.1 普通实现模式（默认）

如果用户 **没有要求任务拆分**。

AI 应直接：

```
分析需求
阅读代码
实现功能
```

流程：

```
理解需求
↓
定位代码
↓
实现功能
↓
验证
↓
返回结果
```

### 8.2 禁止自动拆分任务

AI **不得自行决定拆分任务**。

只有在用户明确提出以下请求时才允许：

```
拆分任务
给出任务计划
先做任务拆分
```

否则：

```
直接实现功能
```

### 8.3 小需求示例

例如：

```
增加一个接口
修复一个 bug
增加一个字段
```

AI 应：

```
直接实现
```

### 8.4 大需求示例

例如：

```
新增模块
重构架构
实现完整工具
```

若用户要求：

```
拆分任务
```

才进入：

```
任务拆分开发流程
```

---

## 9. 任务拆分开发流程

⚠️ **重要规则**

AI **不得自行决定拆分任务**。  
只有当用户明确提出：

- 请拆分任务
- 给我任务计划
- 这个需求需要拆分

AI 才进入本流程。

否则必须使用 **普通实现模式**。

### 9.1 任务拆分目标

任务拆分的目的：

- 将复杂需求分解为可验证的小步骤
- 保证开发过程可控
- 每一步都可以运行和验证
- 避免 AI 一次生成大量不可控代码

### 9.2 需求设计文档结构

当进入任务拆分模式时，AI 必须先生成需求设计文档。

结构如下：

```
需求名称
一、需求背景

说明需求产生的背景，例如：

为什么需要该功能

哪个模块存在问题

二、当前现状

描述当前系统的能力和问题，例如：

当前功能如何实现

存在哪些限制

为什么无法满足需求

三、目标

描述需求完成后的效果，例如：

新增什么功能

支持什么能力

四、技术方案

说明实现方案，例如：

新增模块

API设计

数据库结构

前端页面

权限控制

五、任务拆分

| 任务编号 | 任务内容 | 状态  |
| ---- | ---- | --- |
| T1   | xxxx | 未开始 |
| T2   | xxxx | 未开始 |
| T3   | xxxx | 未开始 |

```

### 9.3 任务执行规则

AI 在执行任务拆分开发时必须遵守以下规则。

规则1：一次只允许执行一个任务

例如：当前任务是T1，那么AI 只能实现 T1。禁止 T1 + T2 + T3 一次实现。



规则2：任务完成必须暂停

任务完成后必须停止并等待用户确认。

例如：任务 T1 已完成，请确认是否继续执行 T2

只有在用户确认后才允许继续。



规则3：任务必须可独立验证

每个任务必须满足：

\- 可以编译
\- 可以运行
\- 不依赖未来任务

禁止设计：

T1 写一半代码，T2 写另一半。



规则4：任务粒度建议

推荐任务拆分粒度：

- 数据库设计
- 实体模型
- Repository
- Application Service
- Controller
- 前端页面
- 测试

每一步尽量独立。



规则5：任务状态管理

任务状态定义：

| 状态   | 含义                   |
| ------ | ---------------------- |
| 未开始 | 尚未开发               |
| 开发中 | AI 正在实现            |
| 待验证 | 已开发完成等待用户确认 |
| 已完成 | 用户确认完成           |

---

## 10. README 文档同步规则

README.md 是 **项目文档与需求记录中心**。

AI 在以下情况必须更新 README.md：

| 操作 | 是否需要更新 |
|-----|-------------|
|新增模块 | 是 |
|新增工具 | 是 |
|新增系统能力 | 是 |
|任务拆分开发 | 是 |

### 10.1 任务拆分需求记录

当启用任务拆分模式时，必须在 README.md 新增需求记录。

示例：

```
需求：JSON Diff 工具
一、需求背景

开发人员在调试接口时需要对比 JSON。

当前系统没有 JSON diff 功能。

二、当前现状

系统仅支持 JSON 格式化。

无法对比两个 JSON 的差异。

三、目标

新增 JSON Diff 工具：

支持左右 JSON 对比

标记差异字段

支持忽略顺序

四、技术方案

前端：

使用 Monaco Editor + diff 模式

后端：

无需后端逻辑

五、任务拆分
任务编号	任务内容	状态
T1	创建页面结构	未开始
T2	集成 diff 编辑器	未开始
T3	实现 JSON diff 逻辑	未开始
```

### 10.2 小需求不需要记录

以下情况 无需更新 README：

- Bug 修复
- 小功能
- 代码优化
- UI 调整

---

## 11. 其他注意事项

AI 在开发过程中必须遵守以下原则。

### 11.1 不得破坏项目结构

禁止：

- 随意移动包结构
- 重命名核心模块
- 修改框架层设计

### 11.2 不得修改框架层设计

以下包属于框架层：com.corwin.framework

AI 修改前必须确认：是否真的需要修改

### 11.3 不得引入新的技术栈

例如：

- 新的 ORM
- 新的 UI 框架
- 新的日志框架

必须保持项目统一技术栈。

### 11.4 禁止破坏现有 API 兼容性

例如：

- 随意修改已有接口返回结构
- 随意删除字段
- 随意改变字段类型

除非主动要求变更

### 11.5 敏感信息

禁止：

- 数据库密码
- API Key
- Token

写入代码或文档。

### 11.6 注释原则

注释应解释：为什么这样设计

而不是：代码在做什么

### 11.7 文档优先级

AI 在开发前应优先阅读：

- README.md
- AGENTS.md

确保理解项目结构与规则。

### 11.8 资源与权限定义同步约定

- 系统资源定义文件路径为 `server/bootstrap/src/main/resources/bootstarp/resources.xml`（目录名 `bootstarp` 为历史拼写，按现状保留）
- 新增/修改/删除页面、菜单、按钮、权限点时，必须同步更新 `resources.xml`
- `resources.xml` 只定义 `菜单 -> 功能 -> 按钮 -> 权限码` 资源树，不承担 API 绑定声明

### 11.9 数据建模与开发期结构变更约定

AI 在设计数据库结构与实体映射时必须遵守以下规则：

- 默认禁止创建数据库物理外键（Foreign Key）
- 关联关系统一采用逻辑外键字段维护，例如 `xxxId`
- 实体之间的关联由应用程序维护，不依赖数据库 FK 保证
- 禁止将 JPA 级联删除、`orphanRemoval`、`@ElementCollection` 集合表关联作为数据管理手段
- 若确有必要引入物理外键，必须由用户明确提出后才允许设计

开发期表结构策略：

- 当前项目开发阶段保持 `spring.jpa.hibernate.ddl-auto=update`
- AI 不得主动将其改为 `validate`、`none` 或其他模式，除非用户明确要求

历史数据处理约定：

- 默认不考虑历史数据兼容
- 默认不考虑旧表结构、旧字段、旧约束、旧数据迁移方案
- 后续功能调整时，除非用户明确要求，否则无需为历史数据编写兼容逻辑或迁移逻辑

Bootstrap Schema SQL 同步约定：

- 引导程序建库建表使用的 schema SQL 文件位于：
- `server/bootstrap/src/main/resources/bootstarp/sql/*/system_schema.sql`
- `server/bootstrap/src/main/resources/bootstarp/sql/*/business_schema.sql`
- 应用中的实体类表结构必须与上述 SQL 资源文件保持一致
- 后续凡是新增、删除、修改实体对应的表名、字段、字段类型、长度、索引、唯一约束时，必须同步更新对应的 schema SQL 文件
- 仅修改 JPA 实体而不更新 bootstrap SQL，视为不完整变更

### 11.10 AI 代码提交约定

- 绝对禁止 AI Agent 自动执行`git add`、 `git commit` 或 `git push` 命令将代码提交或推送到仓库。
- AI 的职责仅限于生成代码、修改文件、运行验证。代码的最终提交必须由用户手动确认并执行。

### 11.11 单元测试新增限制

- 若用户未明确提出“新增/补充单元测试”，AI 禁止主动创建或修改单元测试代码。
- 功能开发、缺陷修复、重构、文档调整等需求，默认不新增单元测试。
- 仅当用户明确要求测试时，才允许按最小必要范围补充对应单元测试。

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**This project has a knowledge graph. Start with the code-review-graph
MCP tools to narrow scope, then read the source.** The graph is cheaper than scanning files and
gives you structural context (callers, dependents, test coverage) that file search cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

### Verify in the source

- Narrow scope with the graph, then read the source. Do not change code from graph output alone.
- For any non-trivial change, read the implementation and the relevant tests before concluding.
- Verify the exact source when touching behavior, database logic, migrations, retries, fallbacks,
  recovery, or compatibility code.
- When the graph and the source disagree, the source wins. The graph may be stale or may not
  model that relationship.
- An empty graph result can mean "not indexed" or "not statically visible", not "does not exist".

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes_tool` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context_tool` | Need source snippets for review — token-efficient |
| `get_impact_radius_tool` | Understanding blast radius of a change |
| `get_affected_flows_tool` | Finding which execution paths are impacted |
| `query_graph_tool` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool` | Finding functions/classes by name or keyword |
| `get_architecture_overview_tool` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.
<!-- /code-review-graph MCP tools -->
