# Breezy

Breezy 是一个全栈工具箱式应用，后端采用 Spring Boot 3.5.x + Java 21，管理后台前端采用 React + Next.js，用户端前端采用 Vue 3 + Vite + TypeScript。项目以 DDD 分层为主线，强调清晰的领域边界、统一的 API 响应、资源化的菜单与权限控制，以及可扩展的工具与系统管理能力。

## 技术选型

- 后端
    - Java 21 / Spring Boot 3.5.10
    - Spring MVC / JPA / MyBatis / JOOQ
    - JWT 鉴权 / AOP 权限控制
    - Log4j2 + JSON Template 日志
    - HikariCP、p6spy（SQL 监听）
    - 数据库驱动：H2 / MySQL / PostgreSQL / Oracle
- 前端
    - 管理后台（`web-admin/`）：React 19 + Next.js 16 + TypeScript
    - 用户端（`web/`）：Vue 3 + Vue Router + Vite 6 + TypeScript
    - Bz UI（`web/src/components/bz` 与 `web-admin/src/components/bz` 自研组件体系）

## 架构设计

- 后端 DDD 分层
    - `interfaces.web`：REST Controller 与 Req/Res DTO
    - `application.service`：用例编排与流程协调
    - `application.command` / `application.view`：命令对象与输出视图
    - `domain.model` / `domain.repo`：领域模型与仓储接口
    - `infrastructure.*`：持久化、调度、安全等基础设施实现
    - ORM 约定：基础单表增删改查使用 JPA；分页查询、动态条件查询统一使用 MyBatis Mapper XML 实现
- 前端资源驱动的菜单与权限
    - `/registry` 返回资源树，驱动动态路由与菜单
    - 资源类型/范围/打开方式统一约定（MENU/BUTTON，SETTING/TOOL 等）
    - 权限以资源 ID 与权限码双通道控制
    - 系统资源定义文件为 `server/bootstrap/src/main/resources/bootstarp/resources.xml`（历史目录名为 `bootstarp`，保持与现有工程一致）
    - `resources.xml` 必须以“页面当前实际功能”为准维护，不做历史兼容保留
    - 新增、修改、移除页面菜单/按钮/权限点时，必须同步更新 `resources.xml`，否则前端资源树与权限点不会完整注册
    - 当前后台资源模型已统一到 `sys_resource + sys_resource_permission`
    - 管理后台的资源心智模型统一为 `目录 -> 菜单 -> 功能 -> 按钮 -> 权限码`
        - 目录：仅用于导航分组与层级组织，不直接对应页面
        - 菜单：对应可访问的页面路由与页面级资源
        - 功能：对应菜单下的隐藏页面、子功能页或中间能力节点
        - 按钮：对应页面内可授权操作，且只有按钮允许绑定权限码
    - `resources.xml` 当前承载后台资源树定义，后续设计、页面命名、菜单管理交互与文档说明统一以 `目录 -> 菜单 -> 功能 -> 按钮` 为标准
    - 后端 API 与权限码的关系由控制器注解扫描和权限初始化流程维护，前端只消费返回的资源树控制菜单、功能和按钮可见性
    - 资源粒度以“业务能力”划分：菜单页面默认承载该页面基础查询能力（例如列表查询、搜索、重置、详情读取），这类基础能力不再额外拆分查询按钮资源
    - 对包含子功能页的场景（如 schemaforge），基础查询能力应下沉到子页面资源，不绑定到父级容器菜单资源

### 管理后台视觉规范（vben 基准）

- 管理后台视觉风格统一以 `vue-vben-admin` 为参考基准，后续后台页面、后台组件、后台布局、后台交互样式均需参考该项目的设计语言。
- 本项目不直接引入 `vue-vben-admin` 作为整套后台模板或 UI 框架，而是在现有 Vue 3 + Bz UI 体系内做“风格对齐 + 简化实现”。
- 管理后台页面入口与用户页面入口必须根级分离：
    - 用户页面：`/`
    - 管理后台页面：`/admin`
    - 用户登录页与后台登录页、首页与后台首页必须分开设计
- 管理后台布局以 vben 常见的“左侧导航 + 顶部工具栏 + 内容区”作为默认标准，视觉上保持统一的留白、圆角、层次、表格密度和工具栏组织方式。
- 每类后台基础组件只保留一套统一风格，不追求复刻 vben 的全部可选样式。
- 如果 vben 某类组件存在多种成熟风格方案，后续开发前必须先向用户确认选择哪一种，再进入实现；AI 不得自行在多种风格中拍板。
- API 约定
    - 后端统一返回 `ApiResponse<T>`，前端 `web/src/api/http.ts` 统一解析
    - 接口传参与响应中，涉及 UUID 含义的标识统一使用 `String` 进行存储与传递，不直接暴露 `UUID` 类型参数
    - API 元信息来源统一为“类级 `@ApiMeta` + 方法级 `@Permission`”：
        - `@ApiMeta` 仅用于 Controller 类，且必须提供 `apiSystem/service/module` 三个枚举值
        - `@Permission` 仅提供接口权限码（`permissionCode`）
        - 未标注 `@ApiMeta` 时，扫描回退到枚举默认值：`breezy/breezy/default`
- 登录与权限
    - JWT 令牌 + `@Permission` 注解 + 权限切面/拦截器
- 启动引导摘要（`sys_bootstrap_digest`）
    - 摘要表读写统一收敛到 `com.corwin.framework.config.BootstrapDigestStore`
    - 业务模块仅依赖框架抽象，不再在 `com.corwin` 业务包侧维护该表 SQL
    - 启动引导摘要由独立 bootstrap 初始化流程维护，不再依赖 `com.corwin.system.bootstrap` 包内能力
- 统一配置管理重构目标（`sys_config_value`）
    - framework 只提供配置定义、发现、启动加载抽象、不可变快照和运行时注册表，不持有系统表、SQL 或业务实体
    - system 配置域负责 `sys_config_value` 的启动期 JDBC 加载、运行期持久化和统一管理 API
    - 数据库只保存管理员覆盖值或重置状态标记；没有覆盖值时直接使用代码中的安全默认值
    - 用户个性化覆盖值继续由 `sys_user_config` 管理，其公共默认值由统一配置注册表提供
    - 详细方案与开发步骤见 `server/framework/config-tech.md`

### 管理后台页面区域命名约定

- 为保证后续 `/admin/*` 页面重构、评审与协作沟通一致，管理后台页面统一使用以下区域术语。
- 后续交流中统一使用这些名称，不再使用“上面那块”“右边按钮区”“表格那里”这类模糊描述。
- 后台公共壳体区域
    - `后台侧边导航区`
        - 指管理后台左侧导航区域
        - 包含：品牌区、一级菜单、二级菜单、侧边栏底部操作区
    - `后台顶部工具区`
        - 指管理后台顶部固定区域
        - 包含：面包屑、顶部右侧工具按钮、标签栏
    - `后台内容工作区`
        - 指管理后台用于承载具体页面内容的主工作区域
- 后台页面标准内容区域
    - `页面内容容器`
        - 指某个后台页面内部的总内容容器
    - `查询面板`
        - 指页面顶部用于筛选、搜索、条件收起展开的区域
    - `查询表单区`
        - 指查询面板内承载筛选字段的区域
    - `查询操作栏`
        - 指查询面板内承载“搜索 / 重置 / 展开收起”等按钮的区域
    - `列表卡片`
        - 指承载列表内容的主卡片区域
    - `列表头部`
        - 指列表卡片顶部区域
        - 包含：标题区、工具栏
    - `列表标题区`
        - 指列表头部中用于显示页面主标题的小区域
    - `列表工具栏`
        - 指列表头部右侧操作区
        - 包含：刷新、查询开关、批量操作、主操作按钮等
    - `表格承载区`
        - 指列表卡片中用于放置表格组件的区域
    - `数据表格区`
        - 指实际表格内容区域
    - `分页栏`
        - 指列表底部分页区域
    - `分页统计区`
        - 指分页栏中显示“共 xx 条记录”的区域
    - `分页控制区`
        - 指分页栏中包含页码、每页条数、翻页按钮的区域
- 前端分页默认约定
    - 管理后台列表页默认每页数量统一为 `10`
    - 若某页面没有特殊说明，分页初始化时必须使用 `10条/页`
    - 若页面提供分页大小切换，`10` 必须作为默认选项和默认值
    - `查询条件变更`（如搜索、筛选、重置）时，分页必须回到第一页
    - `列表刷新` 与 `行内操作后刷新` 时，必须优先保留当前页，不得默认跳回第一页
    - 若刷新后当前页已失效（例如总页数减少、当前页为空且存在前置页），必须自动回退到最后一个有效页
- 查询面板默认约定
    - 管理后台列表页的 `查询面板` 默认状态统一为 `收起`
    - 页面首次进入时不自动展开 `查询面板`
    - 后续新增后台列表页若包含 `查询面板`，默认行为也必须保持收起，仅在用户主动点击后展开
    - 后续 `/admin/*` 页面中的 `查询面板` 布局统一遵循 `/admin/apis` 当前实现，不允许各页自由发挥
    - `筛选条件` 标题只保留左侧标题，不再在标题栏右侧固定放置 `重置 / 搜索 / 展开/收起`
    - `重置 / 搜索 / 展开/收起` 必须作为 `查询表单区` 的最后一个 grid item，跟随所有查询条件一起参与布局
    - 查询按钮组整体宽度按一个普通查询条件单元处理，不额外悬浮、不绝对定位、不单独占据标题栏
    - 当所有查询条件加按钮组只占一行时，只显示 `重置 / 搜索`，不显示 `展开/收起`
    - 当查询条件超过一行时，默认折叠为只显示第一行，且第一行最后一个位置必须是 `重置 / 搜索 / 展开`
    - 点击 `展开` 后显示全部查询条件，按钮组跟在最后一个条件之后；点击 `收起` 后恢复默认折叠状态
    - 查询面板折叠高度计算时，必须把按钮组和查询条件一起纳入测量，窗口宽度变化后必须重新计算单行/多行与折叠高度
    - 移动端或窄屏场景下，若一行仅能容纳一个 grid cell，应取消“按钮强制占据第一行最后一格”的定位，避免第一行只剩按钮组
- 后台详情交互约定
    - 管理后台中，复杂实体的 `详情` 与 `编辑` 统一优先使用 `右侧抽屉`
    - 适用场景：实体字段较多、需要保留列表上下文、需要同时承载关联子列表或复杂编辑能力
    - `弹窗` 仅用于轻量确认或子对象的局部编辑，不作为复杂实体详情主交互
    - 后续新增后台页面若存在“列表 -> 详情/编辑”链路，默认按“列表页 + 右侧抽屉”实现
- 页面内部细分命名原则
    - 需要继续细分时，优先使用“区域名 + 功能名”的方式命名
    - 示例：`关键字筛选项`、`状态筛选项`、`主刷新按钮`、`查询面板开关按钮`、`操作列`、`详情弹窗`、`编辑弹窗`
- `/admin/apis` 页面可作为当前标准参考页，其页面结构默认按上述术语理解和沟通。
- `/admin/apis` 当前前端列表接口应直接调用分页接口 `/api/apis/page`，不再拉全量列表后做前端本地分页；`/api/apis` 可保留给其他非分页场景复用。
- `/admin/resources` 为平台管理下的资源管理页：
    - 用于维护目录、菜单、功能、按钮资源树
    - 支持资源基础信息、路由信息、状态配置、按钮权限码绑定维护
    - 核心校验规则统一在后端执行：父子类型约束、按钮权限码约束、编码唯一、系统内置资源删除保护
- 后台壳体级自助页面约定
    - 例如：`/admin/profile`、`/admin/profile/password`、`/admin/profile/preferences`、`/admin/help`
    - 这类页面属于 `user-panel` 下拉菜单入口，不属于后台侧边导航区菜单
    - 这类页面不写入 `resources.xml`
    - 这类页面不参与授权资源树，也不作为可分配权限点
    - 访问控制仅依赖静态后台路由与登录态/用户类型校验
    - 仅当页面后续演进为真正的业务菜单能力时，才进入 `resources.xml` 和授权体系

### 管理后台按钮设计约定

- 后续 `/admin/*` 页面中的按钮样式与文案必须收敛到已有按钮体系，不允许随意新增新的按钮形态。
- 当前管理后台允许使用的按钮类型如下：
    - `确认按钮`
        - 统一使用 `BzButton type="primary"`
        - 适用于通用提交、保存、确认类动作
        - 文案优先统一为 `确认`
    - `取消按钮`
        - 统一使用默认态 `BzButton`
        - 适用于取消编辑、关闭、返回、撤销当前临时修改
    - `查询主按钮`
        - 使用 `.admin-filter-primary`
        - 仅用于查询面板中的主查询动作，文案统一优先为 `搜索`
    - `查询次按钮`
        - 使用 `.admin-filter-secondary`
        - 仅用于查询面板中的次级动作，文案统一优先为 `重置`
    - `列表主操作按钮`
        - 使用 `.admin-toolbar-primary`
        - 仅用于列表头部主操作，如 `新增`
    - `圆形工具按钮`
        - 使用 `.admin-vben-circle-button`
        - 仅用于搜索框开关、刷新、轻量面板开关等顶部工具动作
    - `行内动作按钮`
        - 统一通过 `AdminActionBar` 承载
        - 文案与 tone 使用现有动作语义，如 `详情`、`编辑`、`启用`、`停用`、`删除`
    - `顶部下拉菜单按钮`
        - 统一用于壳体级轻量导航或状态动作，例如用户菜单、通知菜单中的入口项
- 对于通用语义，禁止继续创造新的业务按钮文案，例如 `保持昵称`、`修改密码`、`保存设置` 这类按钮文案不应作为默认设计继续扩散。
- 若业务上只是执行通用提交动作，应优先使用 `确认`；仅当业务语义必须明确区分时，才允许使用专用按钮文案。
- 若确实需要新增按钮类型，必须先补充文档约定并说明它与现有按钮体系的边界，不能直接在页面里临时发明新样式。

### 个性化配置与时间交互约定

- 获取个性化配置必须走白名单接口：
    - 已登录：返回“用户覆盖值 > 系统默认值”的合并结果
    - 游客（未登录）：返回同一白名单下的系统默认值（不返回 401）
- 前端登录态加载策略：
    - 首次登录成功后仅拉取一次个性化配置并缓存
    - 用户更新任一配置后，立即重新拉取一次并刷新本地缓存
- 时间交互协议（强约束）：
    - 前后端传输层仅使用 UTC 时间戳字符串（epoch millis），`Instant` 统一为字符串时间戳
    - 前端显示层按 `USER_TIME_ZONE + USER_DATE_TIME_FORMAT/USER_DATE_FORMAT` 渲染
    - 前端时间输入层按用户时区呈现，提交前必须转换为 UTC 时间戳字符串
    - `USER_TIME_ZONE`、`USER_DATE_TIME_FORMAT`、`USER_DATE_FORMAT`、`USER_DECIMAL_FORMAT` 统一存储语言无关的稳定编码，不直接存储时区 ID、日期格式串或小数格式串
    - 上述 4 个配置项的候选项文案仅用于展示，前后端各自根据同一份编码语义解析出本端使用的时区、日期格式和小数格式实现
    - 后续若新增或调整这 4 个配置项的候选项，必须保持“编码稳定、语义一致、前后端各自本地解析”的规则，禁止一端直接依赖另一端的显示文案或格式串
- 小数格式仅用于前端展示，传输层不携带“格式化后的数值字符串”
- 候选项来源约定：统一通过数据字典获取

### 数据建模约定

- 默认禁止数据库物理外键，关联统一使用逻辑外键字段维护（如 `todoId`、`eventId`）
- 关联关系由应用服务和仓储层维护，不依赖数据库 FK、JPA 级联删除或 `orphanRemoval`
- `@ElementCollection` 这类会隐式引入集合表关联的映射默认禁止使用，优先改为普通字段序列化或独立实体 + 逻辑外键
- 开发阶段保持 `spring.jpa.hibernate.ddl-auto=update`，继续通过运行自动更新表结构
- 默认不考虑历史数据、旧表结构、旧约束、旧数据迁移与兼容；仅在需求明确提出时处理
- 分页查询、动态筛选查询、后台列表检索查询默认优先使用 `XSQL` 实现，不优先使用 Spring Data JPA 方法名派生或 `@Query` 直接拼装复杂分页逻辑
- `XSQL` 使用约定：
    - `XTableQuery`：适用于单表、按实体属性路径构造条件、条件之间以 `AND` 组合的场景
    - `XNativeQuery`：适用于需要自定义 SQL、跨字段表达式、`OR` 组合、复杂统计、别名排序映射的场景
    - 选择原则：
        - 如果查询条件都能直接映射到实体属性，并且 where 逻辑是普通 `AND` 叠加，优先使用 `XTableQuery`
        - 如果关键字查询需要覆盖多个字段并通过 `OR` 命中，或需要 `concat/coalesce/case when/聚合` 这类表达式，改用 `XNativeQuery`
        - 不要为了强行使用 `XTableQuery` 而把本应是 `OR` 的语义错误改写成多个 `AND like`
    - `*If` 系列方法（如 `eqIf / likeIf / inIf / betweenIf`）用于动态条件拼装，条件不满足时自动忽略
    - `likeIf` 不会自动补 `%`，需要调用方显式传入，例如 `"%" + keyword + "%"`
    - 排序应在仓储实现层做字段白名单映射：
        - `XTableQuery` 使用 `orderBy(...)`
        - `XNativeQuery` 使用 `orderByAlias(...)`
        - 不要直接信任前端字段名
    - 分页统一在仓储层返回 `PageData<T>`，Controller 再转换为 `PageResult<T>`
    - 推荐模板：
        - `XTableQuery`：参考 `com.corwin.datasource.infrastructure.persistence.DatabaseTableRepositoryJpaAdapter#pageByQuery`
        - `XNativeQuery`：适用于 API 列表这类“模块 / 路径 / 处理类 / 处理方法 任一命中”的关键字检索场景
    - 简单主键查询、唯一键查询、固定条件存在性判断仍可继续使用普通 JPA Repository
- Bootstrap SQL 结构文件是引导程序建库建表的权威输入之一，必须与应用实体定义保持一致：
    - 系统域实体对应 `server/bootstrap/src/main/resources/bootstarp/sql/*/system_schema.sql`
    - 业务域实体对应 `server/bootstrap/src/main/resources/bootstarp/sql/*/business_schema.sql`
    - 后续新增、删除、修改实体字段、索引、唯一约束、表名、列名时，必须同步更新对应 schema SQL 文件，不能只改 JPA 实体

### 框架与业务模块依赖规则

**重要原则**：`com.corwin.framework` 与 `com.corwin` 业务包之间的依赖必须单向，**禁止反向依赖**。

- **依赖方向**：
    - `com.corwin.system`、`com.corwin.datasource`、`com.corwin.storage` 等业务包可以依赖 `com.corwin.framework`（业务层依赖框架层）
    - `com.corwin.framework` 严禁依赖任意业务包（框架层不能依赖业务层）

- **设计原则**：
    - 框架层定义抽象接口和基础能力，不包含具体业务实现
    - 业务层实现框架层定义的接口，提供具体业务逻辑
    - 框架层通过 Spring Boot Auto Configuration 机制强制要求业务层必须实现必需接口
    - 使用泛型避免框架层接口依赖业务层具体类型

- **示例**：
    - `TokenService<T>` 定义在 `com.corwin.framework.web.auth`，使用泛型避免对具体 `User` 类的依赖
    - `JwtTokenService` 在 `com.corwin.system.infrastructure.security` 中实现框架层的 `TokenService<User>`
    - `UserPermissionChecker` 定义在框架层，`DefaultUserPermissionChecker` 在业务层实现
    - `AuthFilter` 在框架层，只依赖框架层定义的接口，不依赖业务层具体实现

- **后续功能开发必须遵循此规则**：
    - 任何需要在框架层使用的抽象接口都应在 `com.corwin.framework` 中定义
    - 具体实现必须在 `com.corwin` 业务包中提供
    - 框架层不应包含任何业务逻辑或业务领域的具体类型
    - 如需在框架层添加新的抽象能力，请参考现有的 `TokenService`、`UserPermissionChecker` 等模式

## 目录结构

```
.
├─ dependencies/          # 依赖管理 BOM 模块
├─ server/                # 后端聚合模块
│  ├─ framework/          # 框架层模块
│  ├─ system/             # 系统域模块（含 MethodStat）
│  ├─ business/           # 业务域模块（datasource/storage/...）
│  └─ app/                # 应用装配与启动模块
├─ web/                   # 用户前端（过渡期仍含部分后台实现）
├─ web-admin/             # 账号后台独立前端（拆分进行中）
├─ pom.xml                # 多模块父工程
└─ README.md
```

## 后端模块说明

- `com.corwin.system`（系统域）
    - 用户、角色、资源、权限、接口、配置、通知、登录日志、任务调度等系统管理能力
    - 用户功能管理：支持维护用户默认可用功能，并按用户覆盖最终可用功能集合
    - 通用动态任务调度能力：基于 Spring 应用事件驱动创建/暂停/恢复/取消/删除动态任务，任务定义与运行态存储在 `sched_job`、`sched_job_runtime`、`sched_job_execution`
    - 动态任务运行时注册采用“事务内落库 + 提交后注册”模型，避免业务事务回滚后任务已注册的脏状态
    - 方法调用统计（MethodStat）已合并到系统域：`com.corwin.system.domain.model.MethodStat`、`com.corwin.system.application.service.MethodStatManageAppService`
    - Web Push 健康检查能力：提供订阅状态、最近投递状态查询与高优先级测试推送接口（`/api/push/health`、`/api/push/health/test`）
    - 系统配置增强：支持客户端 IP 获取方式实时预览、TIME_OFFSET 基于服务器时间的偏移预览与目标时间秒差辅助计算
    - 文件管理能力：系统业务文件上传、文件预览/下载、批量元数据查询、全局文件管理
    - 管理员文件查询增强：支持逻辑文件/目录统一检索、逻辑文件到物理文件详情映射、物理文件反向引用追踪
    - 应用内文件创建能力：支持系统在无外部上传流场景下按 `FilePurpose` 自动路由目录创建文件，按内容摘要执行物理文件去重与引用计数复用
    - 控制器集中在 `system/interfaces/web`，应用服务在 `system/application/service`
- `com.corwin.datasource`（数据源与元数据）
    - 数据源连接维护、连接测试、元数据刷新与查询
    - 数据源连接参数持久化：认证方式/连接类型/驱动/主机/端口/数据库/服务名等字段入库，复制连接信息直接读取字段，不再解析 JDBC URL
    - JDBC 元数据抽取、连接/表/列的域模型与仓储
- `com.corwin.storage` / `com.corwin.jsonfmt` / `com.corwin.schemaforge` / `com.corwin.reminder` / `com.corwin.web`（业务工具域）
    - 以上包位于 `server/business` 模块，按 DDD 分层组织
    - 各域控制器位于对应模块的 `interfaces/web`，应用服务位于对应模块的 `application/service`
- `com.corwin.framework`（框架与基础能力）
    - 统一响应/异常/权限注解、分页模型、JSON 序列化、过滤器、配置注册等
    - 启动摘要存储能力：`BootstrapDigestStore` 提供统一的 `sys_bootstrap_digest` 访问入口
    - 统一配置目标能力：提供 `ConfigSpec`、`ConfigSpecCatalog`、`ConfigRegistry` 和启动加载 SPI；具体配置表读写归 system 配置域
    - 通用缓存能力：`com.corwin.framework.cache` 提供 `CacheTemplate`、`LOCAL` 本地缓存实现，以及 `REDIS` / `LOCAL_REDIS` 扩展位；第一版支持 `STRING`、`OBJECT`、`HASH`、`LIST`、`SET`、`SORTED_SET`
    - 通用异步事件能力：统一 `AsyncEventPublisher/@AsyncEventListener` 编程模型，支持 `publish/publishAt/publishAfter`，支持 `IN_MEMORY` 与 `IN_MEMORY_DURABLE`（启动恢复）模式，并保留 Kafka/RabbitMQ 传输扩展位
    - 数据库方言通过 `AbstractDatabaseDialect` 提供公共 SQL 默认实现；启动期数据库配置由 `DbConfigEnvironmentPostProcessor` 预加载，运行期配置读写通过 `ConfigStore` 统一收口

## 前端模块说明

- 页面（`web/src/pages`）
    - 系统管理后台：接口、用户功能、账号、用户、用户直授权限、角色、系统配置、数据字典、登录日志、诊断工具、方法统计、系统文件
    - 全局通知菜单支持“推送健康检查面板”：可查看浏览器安全上下文、Service Worker Scope、订阅健康状态、最近投递状态，并支持一键发送高优先级测试推送
    - 系统配置页交互升级：列表改为只读展示 + 图标化编辑入口；按配置项弹出专用编辑界面（客户端 IP 实时测试、TIME_OFFSET 秒差辅助计算、日期/日期时间/小数格式样例预览）
- 系统管理后台：文件管理（只读查询逻辑文件/目录，支持物理文件详情映射与同物理引用反查，文本/图片预览带大小阈值，并提供清晰错误态提示）
    - 文件管理预览交互：目录切换、搜索、排序、刷新等会导致列表内容变化的操作会自动关闭当前预览，避免旧内容误导
    - 工具：数据源管理、数据库管理、元数据浏览、结构工厂（`/schemaforge`）、方法调用统计（`/method-stat`）、诊所管理（商品/库存/采购/销售/流水）
    - 工具：文件存储（平铺/详细列表双视图、名称模糊搜索、目录树选择目标的逻辑移动、按名称/大小/类型/最新修改排序）
    - 信息：个人资料、权限详情
- UI 约定
    - 工具类页面统一使用 `AppHeader` 顶部栏（已覆盖数据源管理与数据库管理）
    - 管理后台页面视觉风格统一参考 `vue-vben-admin`，包括导航层级、页面工具栏、卡片容器、表格页、表单页与弹窗页的结构与层次
    - 管理后台组件优先复用 Bz UI 并在其上做 vben 风格封装，不直接引入其他 UI 框架
    - 管理后台新组件若现有库中不存在，仍需按 vben 风格补齐，但每类组件仅保留一套统一风格实现
    - 工具类页面标题右侧提供快捷键提示图标，点击展示当前页面支持的组合键说明
    - 快捷键说明配置方式：工具页面在组件内注册（`web/src/registry/shortcuts.registry.ts`）
        - 使用 `setPageShortcuts([{ keys: "Ctrl/Cmd + S", action: "保存" }])` 注册（仅记录全局组合键）
        - 使用 `clearPageShortcuts()` 清理，未注册时弹层展示“暂无快捷键”
    - 常用主按钮规范：页面只有一种新增动作时按钮统一为「新增」，且使用统一的主按钮样式（形状/颜色/尺寸一致）
        - 仅当页面存在多种新增类型时，才使用「新增xx/新建xx」避免歧义
    - 按钮文本换行约束：后台所有按钮默认禁止文本换行，按钮文案必须单行展示（如「查询」「新增」「保存」）
        - 实现要求：按钮样式需保证 `white-space: nowrap`，并在窄宽度场景通过 `flex-shrink: 0`、最小宽度或布局调整避免折行
    - UI 组件标准化（Bz UI）
        - 基础 UI 组件统一使用 Bz UI（按钮/表格/表单/弹窗/分页/提示），禁止页面重复造轮子
        - 常用控件优先使用：`BzButton`、`BzTable`、`BzForm`、`BzInput`、`BzSelect`、`BzDialog`、`BzPagination`、`BzTooltip`、`BzEmpty`、`v-loading`
        - 图标统一通过 `BzIcon` 组织（优先文本/系统图标），避免私有图标实现导致风格割裂
        - 主题主色统一为 `#3B82F6`，相关颜色需与主题保持一致
        - 新增通用交互时优先补齐 `web/src/components/bz`，页面侧直接复用，不做一次性页面实现
    - 页面统一由固定顶部栏、内容区、固定底部状态栏构成，内容区独立滚动并与上下栏保持间距
    - 全局消息提示通过底部状态栏展示，左下角可查看最近 10 条记录
    - 功能页面不展示无实际功能的介绍文本，避免浪费空间
- 组件（`web/src/components`）
    - 各模块的表格、表单、弹窗、权限树、通知组件
- 注册表（`web/src/registry`）
    - 资源/权限/配置/通知/认证的本地状态与加载流程
- API 层（`web/src/api`）
    - 与后端接口对接的分层封装
- 类型与工具（`web/src/types` / `web/src/utils`）
    - 全量类型定义与工具函数

### 前端静态资源管理约定

- 静态资源统一分为两类目录：`web/public` 与 `web/src/assets`
- `web/public`
    - 用于必须通过固定 URL 访问的公开资源
    - 不参与 Vite 模块依赖分析与文件名 hash
    - 构建后按原路径原文件名输出到站点根目录
    - 典型场景：`favicon`、`manifest` 关联图标、`Service Worker` 脚本与其固定引用资源、浏览器通知图标
    - 页面或组件内若只是普通展示图片，禁止优先放到 `public`
- `web/src/assets`
    - 用于前端源码直接引用的资源
    - 必须通过 `import`、CSS `url(...)`、或组件模板资源引用方式接入
    - 参与 Vite 打包、缓存指纹和依赖图管理
    - 典型场景：后台品牌图、页面插图、组件内图片、局部背景图
- 命名约定
    - 不同目录中的资源若用途不同，禁止继续使用同名文件造成歧义
    - 固定 URL 图标优先命名为：`favicon.*`、`app-icon.*`、`notification-icon.*`
    - 组件内品牌图优先命名为：`brand-logo.*`、`admin-logo.*`、`home-hero.*`
- 使用约定
    - `public` 资源使用根路径访问，例如：`/favicon.png`
    - `src/assets` 资源使用模块导入，例如：`import brandLogo from "../assets/brand-logo.png"`
    - 同一个视觉素材若同时用于固定 URL 场景和源码引用场景，允许存在两份输出资源，但必须通过命名体现用途差异
- 当前项目约定
    - 后台左上角品牌图属于源码内展示资源，应放在 `web/src/assets`
    - 站点图标、通知图标、`Service Worker` 依赖图标属于固定 URL 资源，应放在 `web/public`
    - 后续新增图片前，先判断它属于“固定 URL 公开资源”还是“源码内引用资源”，再决定放置目录
    - 后台菜单/目录图标不再要求前端硬编码 SVG；统一使用后端系统文件中的 `STATIC_ASSET` 用途文件
    - `server/bootstrap/src/main/resources/bootstarp/resources.xml` 中 `directory/menu` 的 `icon` 属性统一填写静态资源 `code`
    - 静态资源 `code` 默认取 `STATIC_ASSET` 根目录下文件名主名，例如 `overview.svg` 的 `code` 为 `overview`
    - 前端通过后端公开接口 `/api/public/static-files/{fileId}` 获取菜单静态资源，且该接口只允许访问 `STATIC_ASSET` 用途下的图片资源文件
    - 用户工具应用图标与其他前端公开资源统一存放在 `server/business/app-resources/public/` 下，支持多级目录组织
    - `ProductApplication.icon` 不再存储 SVG 内容或静态资源 `code`，统一存储相对资源路径，例如 `icons/applications/jsonfmt.svg`
    - 前端通过公开接口 `/api/public/frontend-resources/{path}` 获取这类资源；该接口无权限控制，并加入默认认证白名单
    - `server/business` 打包后会生成 `target/app/` 部署目录，结构为 `app.jar + config/ + app-resources/ + logs/ + data/`
    - `application*.yml`、`log4j2-spring.xml` 与 `config/log4j2/json-template.json` 放在 `config/` 下；前端公开资源与模板文件放在 `app-resources/` 下，不再直接打进 `exec jar`

### 后台菜单图标静态资源清单

- 当前 `resources.xml` 中已使用的目录/菜单图标 code 如下，后续可按该清单上传到 `STATIC_ASSET` 逻辑目录
- 目录图标 code
    - `overview`
    - `platform`
    - `shield`
    - `customer`
- 菜单图标 code
    - `dashboard`
    - `settings`
    - `link`
    - `book`
    - `folder`
    - `monitor`
    - `chart`
    - `users`
    - `role`
    - `policy`
    - `history`
    - `audit`
    - `customer-user`
    - `customer-chart`
    - `spark`
- 推荐上传文件名
    - `overview.svg`
    - `platform.svg`
    - `shield.svg`
    - `customer.svg`
    - `dashboard.svg`
    - `settings.svg`
    - `link.svg`
    - `book.svg`
    - `folder.svg`
    - `monitor.svg`
    - `chart.svg`
    - `users.svg`
    - `role.svg`
    - `policy.svg`
    - `history.svg`
    - `audit.svg`
    - `customer-user.svg`
    - `customer-chart.svg`
    - `spark.svg`
- fallback 约定
    - 若某目录/菜单未设置 `icon`，或设置了 `icon` 但后端未解析到对应静态资源文件，则前端回退为代码内置 SVG 默认图标
    - 目录使用固定“目录默认图标”，菜单使用固定“菜单默认图标”，不再按 section 编码切换默认图标

### 前端架构与组件复用约定

- 分层建议
    - `pages` 仅负责页面编排（状态、接口调用、权限判断、路由上下文），避免把通用 UI/交互逻辑长期堆在页面内
    - `components` 承载可复用视图单元，优先按“通用能力 + 业务域”拆分目录
    - `api` 负责接口封装，页面不直接拼接后端 URL
    - `types` 统一管理 DTO/视图模型，跨页面复用同一类型定义

- 当前可复用的基础组件（优先复用）
    - 布局层：`web/src/layout/AppHeader.vue`
    - 通用展示：`web/src/components/SearchBox.vue`、`web/src/components/ResultsPanel.vue`
    - 全局反馈：`web/src/components/common/StatusBar.vue`（配合 `web/src/utils/message.ts`）
    - 通用状态面板：`web/src/components/common/StateHint.vue`（统一加载中/空态/错误态与重试按钮）
    - 通用目录选择弹窗：`web/src/components/common/FolderTargetPickerDialog.vue`（目录树目标选择、禁选项控制）
    - 认证与通知基础能力：`web/src/components/auth/AuthMenu.vue`、`web/src/components/auth/LoginDialog.vue`、`web/src/components/notifications/NotificationMenu.vue`

- 新功能开发约定
    - 同类交互（如搜索栏、表格操作列、选择弹窗、空态/错误态面板）在新增前先检索是否已有基础组件可复用
    - 当某能力在 2 个及以上页面重复出现时，需抽取为基础组件或组合式能力后再扩展
    - 新沉淀的通用能力优先放入 `web/src/components/common` 或 `web/src/layout`，并补充清晰的 `props/emit` 类型
     - 页面中的基础能力（如目录选择器、统一错误面板）应优先组件化沉淀，避免复制粘贴实现
     - 后续调整基础组件时，需同步评估并回归所有复用页面，确保交互一致性

### 前端管理后台现状地图（协作基线）

- 本节用于统一后续管理后台需求沟通语义，避免“页面在哪里、菜单怎么来、权限在哪判断”反复确认。
- 当前管理后台页面入口、路由分流、菜单构建、权限判定和页面-API 映射，均以本节描述为准。

- 入口与布局
    - 根组件 `web/src/App.vue` 通过路由 `meta.layout` 切换壳体：
        - `admin`：加载 `web/src/layout/AdminLayout.vue`
        - `blank`：登录等空白页
        - default：`AppHeader + 内容区 + StatusBar`
    - 管理后台入口根路径：`/admin`
    - 管理后台登录页：`/admin/login`

- 管理后台静态路由主干（当前已接入）
    - 概览：`/admin`
    - 平台管理：`/admin/configs`、`/admin/apis`、`/admin/resources`、`/admin/dicts`、`/admin/system-files`、`/admin/diagnostic`
    - 权限中心：`/admin/users`、`/admin/roles`、`/admin/login-logs`、`/admin/audit-logs`
    - 客户中心：`/admin/web-users`、`/admin/normal-features`
    - 隐藏页（不直接出现在侧边栏，通常由权限跳转进入）：`/admin/method-stat`、`/admin/user-permissions`
    - 路由定义位置：`web/src/router/index.ts`

- 后台菜单构建机制（侧边导航）
    - 管理后台左侧菜单不是手写常量，而是由 `AdminLayout` 根据 `router.getRoutes()` 动态构建。
    - 仅收集 `meta.layout === "admin"` 且非登录/404 路由项。
    - 菜单分组与排序由路由 `meta.adminNav.sectionOrder/order` 决定。
    - 菜单构建核心代码：`web/src/layout/AdminLayout.vue`（`buildMenuTree`）。

- 用户类型分流与路由守卫
    - 用户类型来源：`web/src/registry/auth.registry.ts`
        - `ADMIN`：账号
        - `USER`：用户
        - `GUEST`：游客
    - 路由守卫位置：`web/src/router/index.ts`
    - 当前分流规则：
        - 游客访问后台区域（`/admin/*`）跳转到后台登录页
        - 账号禁止进入首页与工具区，强制落到 `/admin`
        - 用户禁止访问后台区域，访问时回到首页

- 资源与权限驱动链路
    - 资源加载入口：`/registry`，前端加载逻辑位于 `web/src/registry/bootstrap.ts`
    - 资源状态存储：`web/src/registry/resources.registry.ts`
    - 权限判定入口：`web/src/registry/permissions.registry.ts`
    - 动态路由生成：`web/src/router/index.ts`（`buildDynamicRoutesFromResources`）
    - 资源组件解析：`web/src/utils/resourceLoader.ts`

- 管理后台页面与 API 对应（当前高频）
    - `web/src/pages/ConfigsAdminPage.vue` -> `web/src/api/configs.ts`、`web/src/api/dicts.ts`、`web/src/api/sse.ts`
    - `web/src/pages/ApisAdminPage.vue` -> `web/src/api/apis.ts`、`web/src/api/dicts.ts`
    - `web/src/pages/UsersAdminPage.vue` -> `web/src/api/users.ts`、`web/src/api/roles.ts`
    - `web/src/pages/RolesAdminPage.vue` -> `web/src/api/roles.ts`
    - `web/src/pages/UserPermissionsPage.vue` -> `web/src/api/permissions.ts`、`web/src/api/users.ts`
    - `web/src/pages/NormalFeatureAdminPage.vue` -> `web/src/api/normal-features.ts`、`web/src/api/external-users.ts`
    - `web/src/pages/SystemFilesPage.vue` -> `web/src/api/system-files.ts`
    - `web/src/pages/LoginLogsPage.vue` -> `web/src/api/login-logs.ts`
    - `web/src/pages/DictAdminPage.vue` -> `web/src/api/dicts.ts`
    - `web/src/pages/DiagnosticPage.vue` -> `web/src/api/diagnostic.ts`

- 管理后台复用组件分区（当前）
    - 用户域：`web/src/components/users-admin/*`
    - 角色域：`web/src/components/roles-admin/*`
    - 资源域：`web/src/components/resources-admin/*`
    - 权限域：`web/src/components/permissions-admin/*`
    - 接口域：`web/src/components/apis-admin/*`

- 后续协作沟通约定（按本节执行）
    - 讨论“菜单/路由”时，优先明确是“静态路由主干”还是“registry 动态生成路由”。
    - 讨论“看不见按钮/页面”时，按“资源是否存在 -> 权限是否授予 -> 路由守卫是否拦截”顺序排查。
    - 涉及后台页面菜单、按钮、权限点变更时，必须同步维护 `server/bootstrap/src/main/resources/bootstarp/resources.xml`。

## 工具模块与规划

- 设计思路
    - 工具模块独立、低耦合，可单独演进
    - 以“记录 + 查询”为核心，不做跨模块依赖
    - 保持功能最小闭环，避免过度扩展型设计
- 现有工具
- `com.corwin.datasource`


## 开发与运行

后端（仓库根目录执行）：

```bash
./mvnw spring-boot:run -pl server/business
```

Windows：

```powershell
./mvnw.cmd spring-boot:run -pl server/business
```

前端（`web/` 目录执行）：

```bash
cd web && npm install
cd web && npm run dev
```

Windows：

```powershell
cd web; npm install
cd web; npm run dev
```

构建与测试：

```bash
./mvnw clean package -pl server -am
./mvnw test -pl server
cd web && npm run build
```

Windows：

```powershell
./mvnw.cmd clean package -pl server -am
./mvnw.cmd test -pl server
cd web; npm run build
```

## 配置说明

- 服务端默认端口：`8910`（见 `server/src/main/resources/application.yml`）
- 默认数据库：H2 文件库（`./data/appdb`）
- 前端 API 基地址：`web/src/api/http.ts` 中的 `API_BASE_URL`
- 日志配置：`server/src/main/resources/log4j2-spring.xml`
- 文件上传默认限制：`50MB`（`spring.servlet.multipart.max-file-size` / `max-request-size`）
- JSON 工具内容转文件阈值：`jsonfmt.storage.policy.contentFileThresholdBytes`（字节，默认 61440）
- 系统文件预览最大字节数：`SYSTEM_FILE_PREVIEW_MAX_SIZE`（默认 5242880）
- 日志过滤器排除路径前缀：`LOGGING_FILTER_EXCLUDE_PREFIXES`（默认 `[/static/, /actuator, /favicon.ico]`）
- 日志过滤器流式路径前缀：`LOGGING_FILTER_STREAM_PREFIXES`（默认 `[/api/sse/]`，并且始终识别 `Accept: text/event-stream`）
- 缓存组件配置前缀：`framework.cache`
    - 默认启用 `LOCAL` 本地缓存实现
    - `framework.cache.key.allow-empty` 控制是否允许空 key，`framework.cache.key.max-length` 控制 key 最大长度
    - `framework.cache.local.eviction.enabled/maximum-size` 控制本地缓存容量淘汰
    - `framework.cache.local.cleanup.enabled/interval` 控制定时清理过期 key
- 已关闭 Spring Liquibase 自动迁移（`spring.liquibase.enabled=false` + 启动类排除 `LiquibaseAutoConfiguration`），Liquibase
  仅作为 SchemaForge 引擎能力使用，不依赖 `db/changelog/db.changelog-master.yaml`

## 数据源自动发现与绑定

- 应用启动时会自动发现 Spring 容器内的 `javax.sql.DataSource` Bean，并注册为 **应用数据源**
- 数据源表新增来源标记：`source_type`（APP/USER），APP 记录附带 `app_ds_key`（默认使用 BeanName）
- 判定同一数据源：`dbType + jdbcUrl + username`
    - 同一连接：只更新连接信息，保留名称
    - 不同连接：删除旧记录并新增新记录
- 应用数据源仅允许在 UI 编辑名称，其他属性不可编辑；连接时直接复用应用 DataSource
- 应用数据源在启动时执行连接测试，成功会更新状态与最近测试时间；失败将直接阻止应用启动
- 应用数据源不可在页面手动删除，启动时若对应 Bean 不存在会自动清理
- 删除数据源不再级联删除库/表/列，仅清空 ManagedDatabase 的 `dataSourceId`，前端会提示“未配置数据源”，可在数据库管理中重新绑定

## 代码风格与规范

- Java 21，优先 `record` 与模式匹配
- Req/Res/Command/View 采用后缀约定
- 禁止通配符导入，Controller 必须返回 `ApiResponse<T>`
- 权限控制通过 `@Permission` 注解
- API 扫描分组通过 `@ApiMeta` 注解与 `ApiSystemCode/ApiServiceCode/ApiModuleCode` 枚举统一约束，不再使用
  `com.corwin.framework.web.api` 包
- 文件域中的主键与关联标识（即使内容为 UUID）统一定义为 `String`，避免跨端序列化与参数绑定差异
- 新增系统/工具功能时，需同步维护 `server/bootstrap/src/main/resources/bootstarp/resources.xml` 中的资源定义，并确保定义与页面实际功能一致
- `*.infrastructure.persistence` 的 `*JpaAdapter` 中，所有 `delete/deleteById/delete*` 调用后必须执行 `repo.flush()`
  （避免唯一索引冲突）
- 时间获取统一规范：禁止在业务代码中直接使用 `Instant.now()`、`LocalDateTime.now()`、`LocalDate.now()`、
  `System.currentTimeMillis()` 获取当前时间
- 统一通过 `HighDate` 或 `LowDate` 获取时间；`java.time` 类型优先使用 `HighDate`，`Date/Timestamp` 场景使用 `LowDate`
- 默认使用 `mock*` 方法作为业务时间（受 `TIME_OFFSET` 影响）；仅在必须使用系统真实时间时使用 `real*` 方法
- 时间范围查询统一使用半开区间：开始时间用 `toStartOf*`，结束时间优先用“下一边界起点”（如 `toStartOfNextDay`、
  `toStartOfNextMonth`），查询条件使用 `<` 而不是 `<= toEndOf*`
- `LowDate.toEndOfDay` 仅保留为兼容语义方法，不再推荐作为查询上界；新代码优先使用 `HighDate`，`LowDate` 主要承担 `Date`/
  `Timestamp` 兼容转换
- Token 有效期规则：签发时间和过期校验都必须基于 mock 时间（当前由 `JwtTokenService` 使用 `HighDate.mockInstant()` 与
  parser clock 保证）
- 异步事件统一约定：所有业务异步事件必须使用 `com.corwin.framework.event` 基础能力实现（发布走 `AsyncEventPublisher`，消费走 `@AsyncEventListener`），禁止在业务模块重复自建队列/线程池事件机制
- 缓存统一约定：业务通过 `CacheTemplate` 显式选择 `CacheMode`；当前仅 `LOCAL` 可用，`REDIS` 与 `LOCAL_REDIS` 调用时会抛出带错误码的 `BizException`
- 缓存异常统一约定：缓存参数错误、模式未实现、类型不匹配等可预期失败统一走 `BizException + CacheError`，不再单独定义裸 `RuntimeException` 体系
- Vue 使用 `<script setup lang="ts">`，严禁 `any`
- 缩进与格式遵循现有文件风格

### 真实时间使用登记（real*）

- `server/system/src/main/java/com/corwin/system/config/application/service/ConfigCommandService.java`：配置覆盖值的修改时间属于系统运行元数据
- `server/system/src/main/java/com/corwin/system/application/service/ConfigAdminService.java`：`previewTimeOffset`
  需要以服务器真实当前时间作为偏移预览基准
- `server/src/main/java/com/corwin/framework/config/JdbcBootstrapDigestStore.java`：`save` 写入引导摘要更新时间，属于系统运行时元数据
- `server/framework/src/main/java/com/corwin/framework/web/filter/LoggingFilter.java`：请求耗时统计必须使用真实时间差
- `server/system/src/main/java/com/corwin/system/infrastructure/scheduling/TaskSchedulerManager.java`：任务执行耗时统计必须使用真实时间差
- `server/system/src/main/java/com/corwin/system/infrastructure/aop/MethodStatCollectMethodInterceptor.java`：方法调用耗时统计使用真实时间差
- `server/system/src/main/java/com/corwin/system/application/service/MethodStatQueryAppService.java`：滑动窗口查询按真实当前时间计算

## 前端双界面重构任务拆分

### 一、需求背景

当前系统前端以首页搜索为核心入口，用户和账号共用同一套页面组织方式。随着后端用户类型和权限体系逐步完善，账号与用户的使用目标已经明显不同：

- 用户的目标是搜索并使用可开放的业务工具
- 账号的目标是进入系统后台，执行资源、用户、角色、配置、日志、用户功能等管理工作

继续维持单一首页搜索入口，会让两类用户的交互边界混乱，也不利于后续系统管理能力扩展。

### 二、当前现状

- 首页仍是统一入口，搜索结果同时覆盖工具、设置、信息类菜单
- 前端登录后尚未真正基于用户类型分流
- 前端当前没有独立的管理后台布局，系统管理页面以平铺路由形式存在
- 账号和用户共享同一套主界面壳体
- 账号理论上还能通过首页搜索进入用户工具页面，这与当前目标不一致
- 后端已经具备用户类型、权限资源注册表、用户功能管理等基础能力，但前端尚未完成对应重构

### 三、目标

完成前端双界面重构，明确区分用户界面与账号管理后台：

- 首页继续作为系统公共入口保留
- 首页搜索仅面向用户功能，只搜索用户可用工具
- 用户登录后保持现有使用方式：搜索工具、进入工具、使用工具
- 账号登录后强制跳转到新的管理后台首页
- 账号不能使用用户工具，也不能通过首页搜索进入用户功能
- 新的管理后台采用左侧树形目录 + 右侧内容区的后台式布局，视觉风格以 `vue-vben-admin` 为标准
- 系统管理功能统一迁移到管理后台界面中承载

### 四、技术方案

前端方案：

- 保留首页公共入口，但重构首页搜索数据源，只保留用户工具资源
- 在认证状态中补齐 `userType` 解析，并在登录成功、刷新恢复时执行用户类型分流
- 新增独立后台布局，例如 `AdminLayout`，用于承载账号管理页面
- 将系统管理页面迁移到后台布局下，通过左侧树形菜单驱动路由跳转
- 左侧菜单优先基于当前 `/api/registry` 返回资源构树，不新增额外前端配置中心
- 管理后台路由与页面入口根路径统一收敛到 `/admin`，用户页面继续使用 `/`
- 路由分成三类：
  - 公共路由：首页、登录态通用页面、404 等
  - 用户工具路由：仅用户可访问
  - 账号后台路由：仅账号可访问
- 在路由守卫中加入用户类型约束，阻止账号进入用户工具页，也阻止用户进入后台管理页

后端方案：

- 优先复用现有 `/api/auth/login`、`/api/auth/me`、`/api/registry`
- 若前端对 `userType` 的字段解析存在缺口，则补齐前端 DTO 映射
- 若在后台菜单构建过程中发现现有 `registry` 数据无法满足后台树形导航需求，再评估是否补充轻量接口
- 当前阶段默认不新增额外后台专用菜单接口，先复用现有资源注册能力

边界约束：

- 账号强制跳转后台，不保留“继续使用用户功能”的兼容入口
- 账号不能使用用户工具
- 用户不进入后台管理页面

### 五、任务拆分

| 任务编号 | 任务内容 | 状态 |
| --- | --- | --- |
| T1 | 补齐前端认证用户类型解析与登录后分流逻辑，确保用户留在首页、账号强制跳转后台 | 已完成 |
| T2 | 重构首页搜索范围，只保留用户可用工具，并移除后台管理菜单进入首页搜索结果 | 已完成 |
| T3 | 新增账号后台基础布局，完成左侧树形目录、顶部栏、内容区三段式管理界面壳体 | 已完成 |
| T4 | 基于 `registry` 资源构建后台菜单树与后台路由体系，并增加用户类型路由守卫 | 已完成 |
| T5 | 将现有系统管理页面迁移到后台布局下，打通资源、用户、角色、配置、日志、用户功能管理等入口 | 已完成 |
| T6 | 完成前后端联调与行为修正，校验游客、用户、账号三种场景下的页面跳转、可见菜单与访问限制 | 已完成 |

### 六、执行规则

- 后续按任务模式推进，一次只执行一个任务
- 每个任务完成后暂停，等待用户确认是否继续下一个任务
- 每个任务都必须可独立编译和验证
- 若某一步发现必须补充后端接口或资源定义，将在对应任务内一并处理

## 前后端双项目拆分方案

### 一、拆分目标

- 将当前单仓内的两套前端形态在工程边界上明确拆开：
    - 用户前端：仅服务 `UserType.USER`，只承载用户工具页与用户侧账户页
    - 账号后台前端：仅服务 `UserType.ADMIN`，只承载后台菜单页与后台壳体级自助页
- `UserType.SYSTEM` 不属于可登录用户，只用于系统内部标记操作来源，不对应独立前端入口。
- 后续允许两套前端独立构建、独立部署、独立发布，不再共享同一前端应用入口。

### 二、页面边界

- 用户前端
    - 根路径建议保留 `/`
    - 只包含：
        - 首页搜索入口
        - 用户工具页，例如 `/jsonfmt`、`/storage`、`/schemaforge`、`/todo`
        - 用户登录/注册/个人资料等用户侧页面
    - 不包含：
        - 任意 `/admin/*` 页面
        - 后台菜单页
        - 后台壳体级自助页
- 账号后台前端
    - 根路径建议保留 `/admin`
    - 只包含：
        - 后台菜单页，例如 `/admin/users`、`/admin/roles`、`/admin/configs`
        - 后台壳体级自助页，例如 `/admin/profile`、`/admin/profile/password`、`/admin/profile/preferences`、`/admin/help`
    - 不包含：
        - 首页搜索
        - 用户工具页
        - 用户注册页与用户登录页

### 三、资源与权限边界

- 用户前端的工具页不纳入管理后台资源体系，不通过 `resources.xml` 管理，也不参与后台角色授权。
- 账号后台前端的后台菜单页继续使用 `resources.xml` + `/api/registry` 构建侧边菜单与页面级权限控制。
- 后台壳体级自助页不写入 `resources.xml`，不参与授权资源树，只通过静态后台路由与登录态校验控制访问。

### 四、认证边界

- 用户前端认证
    - 登录接口：`/api/auth/login`
    - 当前用户接口：`/api/auth/me`
    - 登出接口：`/api/auth/logout`
- 账号后台前端认证
    - 登录接口：`/api/admin/auth/login`
    - 当前用户接口：`/api/admin/auth/me`
    - 登出接口：`/api/admin/auth/logout`
- 两套前端后续应各自维护独立的登录态恢复、路由守卫与页面跳转策略，不再共享同一套前端分流逻辑。

### 五、路由与构建边界

- 后续拆分为两个独立前端工程：
    - `web/`：用户前端
    - `web-admin/`：账号后台前端
- 两个前端项目各自维护：
    - 自己的 `router`
    - 自己的 `layout`
    - 自己的 `api` 包装入口
    - 自己的静态资源与页面注册逻辑
- 共享能力优先下沉为独立公共包或共享目录，例如：
    - 通用 Bz UI 组件
    - 时间/格式化工具
    - 通用类型定义

### 六、接口分层建议

- 用户前端只消费用户侧接口与工具侧接口，不依赖后台菜单注册、后台角色授权、后台资源管理相关接口。
- 账号后台前端只消费后台管理接口，不直接依赖首页搜索、用户工具搜索、用户注册等接口。
- 当前 `/api/registry` 后续仅作为账号后台前端的后台菜单资源接口使用。
- 由于 `registry` 命名不能准确表达“后台菜单资源”的语义，后续应调整相关接口、属性、类和方法名称，统一向“后台菜单资源”语义收口。
- 建议的后续命名方向如下：
    - 接口：`/api/admin/menu-resources` 或 `/api/admin/navigation-resources`
    - Service：`AdminMenuResourceService`
    - Controller：`AdminMenuResourceController`
    - DTO / View：`AdminMenuResourceRes`、`AdminMenuResourceView`
    - 前端状态与方法：`menuResources.registry.ts`、`ensureAdminMenuResourcesLoaded()`
- 用户前端不再复用后台资源模型，后续应增加独立的“用户工具清单接口”。
- 建议的接口职责：
    - 只返回当前 `UserType.USER` 用户可用的工具页清单
    - 仅包含用户首页搜索和工具页跳转需要的最小字段
    - 不包含后台菜单树、后台按钮、后台权限资源节点
- 建议的接口与模型方向：
    - 接口：`/api/user/tools` 或 `/api/user/tool-pages`
    - 返回字段建议：`code`、`name`、`description`、`path`、`component`、`icon`、`sortNo`
    - 前端仅基于该接口构建首页搜索与用户工具导航，不再依赖后台菜单资源接口

### 七、拆分迁移步骤建议

- 第一步：先在当前仓库内完成页面边界与术语边界收口
- 第二步：把用户前端与账号后台前端的共享逻辑识别出来，抽成公共能力
- 第三步：以当前 `web/` 作为用户前端保留，并新增 `web-admin/` 作为账号后台前端入口
- 第四步：分别接入各自构建、部署、环境变量与发布流水线
- 第五步：将后台菜单资源接口从当前 `registry` 语义迁移为新的后台菜单资源接口命名
- 第六步：补充独立的用户工具清单接口，并将首页搜索完全切换到该接口
- 第七步：移除过渡期的单前端双体系分流逻辑

### 七点五、当前拆分落地进展（2026-05）

- `web-admin/` 已创建为独立前端工程，并具备以下最小闭环能力：
    - 独立 `package.json`、`vite.config.ts`、`tsconfig.json`
    - 独立后台登录页
    - 独立后台壳体布局
    - 独立后台路由守卫
    - 独立后台菜单资源加载逻辑
- 第一批后台高频页已接入 `web-admin/` 真实页面路由：
    - `/users`
    - `/roles`
    - `/configs`
    - `/apis`
- 上述四个页面已进入第二阶段迁移：
    - 页面本体已经内聚到 `web-admin/src/pages`
    - 用户域/角色域/接口域/配置域的直接依赖 API、types、专用组件已同步迁入 `web-admin/src`
    - 当前仍保留对部分通用共享能力的复用，例如：Bz UI、通用样式、`AdminActionBar`、`confirm/message/formatter`、权限注册表
    - 这意味着 `web-admin` 已不再依赖 `@shared/pages/*` 承载这四个高频后台页，而是开始拥有自己的后台页面实现层
- 第二批后台辅助页也已完成页面实现内聚：
    - `/login-logs`
    - `/audit-logs`
    - `/dicts`
    - `/system-files`
    - `/diagnostic`
- 上述五个页面的当前状态：
    - 页面本体已经内聚到 `web-admin/src/pages`
    - 直接依赖的领域 `api / types / 专用组件` 已按页面需要迁入 `web-admin/src`
    - 后台路由已由 `web-admin/src/router/index.ts` 显式接管，不再依赖 shared 页面包装
- 第三批剩余后台页也已完成接管与内聚：
    - `/method-stat`
    - `/web-users`
    - `/web-user-stats`
    - `/normal-features`
    - `/admin/profile`
    - `/admin/profile/password`
    - `/admin/profile/preferences`
    - `/admin/help`
- 上述页面的当前状态：
    - 页面路由已全部由 `web-admin/src/router/index.ts` 显式接管
    - 业务后台页（如方法统计、用户中心、功能配置）已迁入 `web-admin/src/pages` 并配套迁入本地 `api / types`
    - 壳体级自助页（个人中心、修改密码、偏好设置、帮助）已迁入 `web-admin/src/pages`，不再依赖旧 `web/` 页面入口
- 当前 `web-admin/` 仍处于第一阶段骨架态：
    - 管理后台主页面已基本完成迁移，剩余工作重点转为：减少 shared 依赖、清理旧 `web/` 后台实现、收敛后台专属 registry/api 命名
    - Bz UI 与部分基础样式仍通过共享源码目录复用，后续需继续抽离为稳定共享层
    - `web/` 中原有后台页面暂未删除，仍作为迁移期参考实现保留
- 后续迁移顺序建议：
    - 下一批优先处理 shared 依赖收口与旧后台实现下线
    - 最后清理 `web/` 中残留的后台实现与双体系分流逻辑

### 八、当前约束结论

- 用户前端和账号后台前端是两套独立系统，不应继续在产品设计上混用“同一前端内的两个区域”心智。
- 首页搜索只属于用户前端，只搜索用户工具页，不包含后台菜单页、后台壳体级自助页。
- 后台规范只覆盖账号后台前端，不覆盖用户工具页。

## web-admin Vue3 -> React + Next.js 重构方案（已完成）

### 一、需求背景

- 当前 `web-admin/` 已独立为账号后台前端工程，但技术栈仍为 `Vue 3 + Vite + TypeScript`。
- 本次需求要求在只处理 `web-admin/` 的前提下，将实现方案整体切换为 `React + Next.js`。
- 页面交互、样式、功能、路由体验需保持不变，只替换前端技术方案。
- 重构完成后，`web-admin/` 中不再保留任何 Vue 代码，Vue 相关依赖与构建配置一并移除。

### 二、当前现状

- `web-admin/` 当前是独立 SPA 工程，关键技术栈如下：
    - `vue@3.5.13`
    - `vue-router@4.5.1`
    - `vite@8.0.5`
    - `@vitejs/plugin-vue`、`vue-tsc`、`eslint-plugin-vue`
- 当前工程使用 `npm`，`packageManager` 与 `engines` 固定为：
    - `node 22.22.1`
    - `npm 11.11.1`
- Maven 根工程 `pom.xml` 也固定了前端工具链版本：
    - `<node.version>v22.22.1</node.version>`
    - `<npm.version>11.11.1</npm.version>`
- 当前页面与能力规模：
    - 22 个后台页面，位于 `web-admin/src/pages/*.vue`
    - 20 个 API 模块，位于 `web-admin/src/api/*.ts`
    - 9 个 registry 模块，位于 `web-admin/src/registry/*.ts`
    - 44 个自研 `Bz` Vue 组件，位于 `web-admin/src/components/bz/*.vue`
- 当前 `web-admin` 已基本摆脱对 `web/` 源码复用的直接依赖：
    - `vite.config.ts` 中仍保留 `@shared -> ../web/src` 别名
    - 但当前 `web-admin/src` 内已无 `@shared` 实际引用
- 当前部署方式为 `Vite SPA + dist 静态产物 + nginx`，`/admin/` 通过 `nginx.conf` 做单页回退。

### 三、目标

- 将 `web-admin/` 完整重构为 `React + Next.js + TypeScript`。
- 保持现有后台所有页面的视觉效果、DOM 结构语义、交互流程、接口行为、权限控制、动态菜单、通知、SSE、推送能力不变。
- 重构完成后删除 `web-admin/` 内全部 Vue 文件、Vue 构建配置、Vue ESLint/TS 配置与 Vue 依赖。
- 工具链版本切换为：
    - `node v24.16.0`
    - `npm 11.13.0`
- 默认继续使用 `npm`，仅在明确遇到安装或工程兼容问题时再评估切换到 `pnpm 11.7.0`。

### 四、技术方案

- 路由与工程形态
    - 采用 `Next.js` 作为新工程壳体，优先使用 `App Router`。
    - 由于当前后台路由依赖运行时资源树和动态权限，推荐 `web-admin` 迁移为“Next 壳体 + 客户端路由状态驱动”的实现，而不是继续使用纯静态导出模式。
    - 推荐以 `/admin` 及 `/admin/[[...slug]]` 作为统一后台入口，登录页保留 `/admin/login` 语义。
- 渲染策略
    - 现有后台高度依赖 `localStorage`、`EventSource`、`Notification`、`Service Worker`、运行时鉴权和动态菜单，因此后台主体页面按客户端组件实现更稳妥。
    - 不以 SSR 为目标，不主动引入服务端取数复杂度；Next 主要承担 React 工程组织、路由壳体、构建和部署。
- UI 迁移策略
    - 现有 `Bz UI` 是 Vue 组件体系，不能直接复用。
    - 需要在 `web-admin` 内重写一套 React 版 `Bz UI`，组件命名、视觉样式、交互行为与当前后台保持一致。
    - 现有全局样式文件 `theme.css`、`list-page.css`、`admin-page.css`、`bz-ui.css` 可尽量保留并迁移使用，减少样式回归风险。
- 状态与基础能力迁移
    - `api/*.ts` 大部分可保留 TypeScript 数据模型与 fetch 调用方式，仅改造为 React/Next 可直接消费的组织方式。
    - `registry/*.ts` 需从 Vue `ref/computed/watch` 改造为 React 可用的 store/context/hook 方案。
    - `router/index.ts` 中的静态路由、动态资源路由、权限判断、跳转守卫，需要重写为 Next 路由适配方案。
- 部署与构建迁移
    - 当前 `Dockerfile + nginx.conf` 面向静态 `dist/`。
    - 若采用标准 Next 运行时，Dockerfile、nginx、Maven 前端构建命令都需要同步改造。
    - 若后续强行要求纯静态部署，则需要额外评估 Next 对后台动态路由的兼容性，复杂度更高。

### 五、任务拆分

| 任务编号 | 任务内容 | 状态 |
| ---- | ---- | --- |
| T1 | 锁定重构边界：仅处理 `web-admin/`，不修改 `web/` 运行逻辑，并确认 Next 采用 App Router + 客户端壳体方案 | 已完成 |
| T2 | 调整工具链与构建入口：升级根 `pom.xml`、`web-admin/package.json` 的 Node/NPM 版本，移除 Vite/Vue 工程骨架，建立 Next 基础工程 | 已完成 |
| T3 | 重建后台应用壳体：迁移全局样式、`App.vue`、`AdminLayout.vue`、顶部工具区、侧边导航区、消息/确认宿主 | 已完成 |
| T4 | 重写 React 版 `Bz UI` 基础组件集，优先覆盖表格、表单、弹窗、下拉、分页、树、消息、确认、加载态等后台核心组件 | 已完成 |
| T5 | 迁移基础能力：`api`、鉴权存储、bootstrap 配置、格式化工具、消息/确认、SSE 协调、推送与通知能力 | 已完成 |
| T6 | 迁移 registry 体系：认证、资源树、权限、通知、快捷键、SSE 生命周期、待办提醒 | 已完成 |
| T7 | 重写路由与动态菜单机制，保证静态页、动态资源页、登录跳转、权限校验与面包屑行为一致 | 已完成 |
| T8 | 逐页迁移 22 个后台页面及其专属子组件，保持样式、交互、抽屉/弹窗/表格行为不变 | 已完成 |
| T9 | 清理 Vue 遗留：删除 `.vue` 文件、Vue ESLint/TS 配置、`vite.config.ts`、`index.html`、Vue 相关依赖与无效别名 | 已完成 |
| T10 | 改造 `Dockerfile`、`nginx.conf`、Maven 前端构建流程，并完成最终构建验证与文档回写 | 已完成 |

- 详细方案文档：`web-admin/docs/web-admin-react-next-refactor.md`

## 引导程序模块设计（草案）

- 当前 `server/bootstrap` 已经独立成模块，但初始化能力仍需继续从 `business` 收口到 `bootstrap`。
- 本次设计已明确：引导程序是后端控制台初始化工具，不新增前端页面，不新增 `/bootstrap` 页面路由。
- `business` 后续不再保留启动入口、初始化触发器和初始化定义文件副本，只保留底层 DAO、仓储、应用服务和领域能力。
- 本次补充的设计资料统一放在 `server/bootstrap/docs/`：
  - `bootstrap-module-current-status.md`：项目现状、迁移边界与模块归位结论
  - `bootstrap-init-inventory.md`：完整初始化资产清单，覆盖菜单、功能、按钮、权限、系统账号、账号、数据字典、配置项、应用数据源
  - `bootstrap-resources-design.xml`：`resources.xml` 迁移归属与资源根结构设计文档
  - `bootstrap-init-config.yaml`：控制台引导器初始化配置草案
- 设计原则：
  - `resources.xml`、`dictionaries.xml`、`bootstrap-init-config.yaml` 的权威副本统一归 `bootstrap`
  - 各领域 `ConfigSpec` 作为配置结构与代码默认值的唯一来源
  - 引导程序只创建配置表结构，不写入或覆盖配置值
  - 敏感信息通过环境变量注入，不写入仓库
## 后端打包与 Bootstrap 初始化边界

- `server/business` 负责生成正常后端服务可执行包，主类为 `com.corwin.App`。
- `server/bootstrap` 负责生成控制台引导程序可执行包，主类为 `com.corwin.Bootstrap`。
- `business` 默认打包不构建前端；需要将前端产物内置到后端包时，使用 `frontend-build` 逻辑并设置 `frontend.build.skip=false`。
- `business` 会保留普通依赖 jar，并额外挂出 `business-<version>-exec.jar` 作为可执行包，避免影响 `bootstrap` 对 `business` 的编译期依赖。
- PowerShell 下带点号的 Maven 属性需要加引号，例如：

```powershell
.\mvnw.cmd clean package -pl server/business,server/bootstrap -am "-Dmaven.test.skip=true" "-Dfrontend.build.skip=false"
```

- `bootstrap` 运行时只打包初始化定义所需的 `bootstarp/resources.xml`、`bootstarp/resources.xsd`、`bootstarp/dictionaries.xml`、`bootstarp/dictionaries.xsd`。
- `bootstrap-init-config.yaml` 不再作为运行时配置源；配置项默认值统一由各领域 `ConfigSpec` 提供。
- 固定系统账号与账号只以 `DefaultUser` 为权威定义源。首次创建 `admin` 时由控制台交互输入密码，并按当前密码策略默认值做基础校验。
- `bootstrap` 控制台不再提供配置种子或配置定义同步任务；统一配置表只由 schema 初始化创建，运行期缺失覆盖值时直接使用代码默认值。

## Bootstrap 控制台引导更新（2026-04）

- 应用初始化统一收口到 `server/bootstrap`，不再依赖 `business` 内部启动触发器。
- 引导程序只提供控制台交互，不增加前端页面。
- 配置默认值不再由 bootstrap 初始化：
  - 运行期配置默认值由各领域 `ConfigSpec` 直接提供
  - 固定用户仍直接来自 `DefaultUser`
- 新增“建库/建表检查”引导能力：根据数据源解析数据库类型，按 JPA 实体定义检查并创建缺失表，已存在表则输出列差异。
- `sys_bootstrap_digest` 及相关摘要跳过机制已移除，所有初始化动作都由引导程序控制台菜单显式触发。
- Bootstrap 中必须直接执行的 SQL 统一按数据库类型放在 `server/bootstrap/src/main/resources/bootstarp/sql/<database>/`；普通查询与更新优先使用 JPA。
## Bootstrap 当前初始化范围（2026-05）

- 当前引导顺序为：数据库与表结构初始化 -> 数据字典初始化。
- 数据字典定义文件为 `server/bootstrap/src/main/resources/bootstarp/dictionaries.xml`，格式由同目录 `dictionaries.xsd` 约束。
- 每个字典类型必须显式提供 `enumClass`，并且该类必须实现 `com.corwin.framework.dict.DictEnumDefinition`。
- 引导程序初始化数据字典时，会先清空 `sys_dict_item`、`sys_dict_type`，再根据 `dictionaries.xml` 解析出的枚举项整批重建。
- `DictUsageRef` 及其相关接口、服务、控制器、持久化实现已移除，不再参与系统初始化与运行期逻辑。
## Bootstrap API 与权限初始化（2026-05）
- 当前引导顺序扩展为：数据库与表结构初始化 -> 数据字典初始化 -> API 与权限码初始化。
- API 初始化不再依赖预生成 SQL，改为在 `bootstrap` 模块内扫描 `@RestController`。
- Controller 必须标注 `com.corwin.system.resource.published.ApiMeta`，且该注解现在只保留 `module` 属性。
- 接口路径与请求方法由 `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 解析得到。
- 接口访问控制规则由 `@PermitAll`、`@Authenticated`、`@Authorize` 解析得到；缺少权限声明的接口会导致引导程序直接失败。
- `sys_api` 表中的 `application_code` 已移除，改为 `module` 字段，对应 `com.corwin.system.resource.domain.model.Api`。
- API 初始化时会全量清空 `sys_api_permission` 与 `sys_api`，再按扫描结果重建。
- 权限码初始化会根据 `@Authorize.permissions` 收集并 upsert 到 `sys_permission`，不会清空已有权限表数据。
## Bootstrap 资源与用户功能初始化（2026-05）

- 当前引导顺序扩展为：数据库与表结构初始化 -> 数据字典初始化 -> API 与权限初始化 -> 资源初始化。
- 账号资源定义文件为 `server/bootstrap/src/main/resources/bootstarp/resources.xml`，结构由同目录 `resources.xsd` 约束。
- `resources.xml` 当前不直接声明 API 绑定，而是通过 `sys_resource + sys_resource_permission` 维护后台资源树与按钮权限码关系。
- 管理后台产品心智与后续菜单管理设计统一采用 `目录 -> 菜单 -> 功能 -> 按钮 -> 权限码`：
  - 目录对应导航分组
  - 菜单对应页面路由
  - 功能对应菜单下的子功能页或隐藏能力节点
  - 按钮对应页面内操作与权限码绑定点
  - `sys_function_permission` 保存按钮/功能到权限码的关联
- 每次执行资源初始化时，会先清空 `sys_role_function`、`sys_role_menu`、`sys_function_permission`、`sys_menu_function`、`sys_function`、`sys_menu`，再按 XML 全量重建。
- 用户功能定义文件为 `server/bootstrap/src/main/resources/bootstarp/normal-features.xml`，结构由同目录 `normal-features.xsd` 约束。
- `normal-features.xml` 只负责定义用户功能的基础状态与权限关联：
  - `sys_normal_feature` 保存功能定义与默认启用状态
  - `sys_normal_feature_permission` 保存功能到权限码的关联
- 每次执行用户功能初始化时，会先清空 `sys_normal_feature_user_override`、`sys_normal_feature_group_grant`、`sys_normal_feature_group_member`、`sys_normal_feature_group`、`sys_normal_feature_permission`、`sys_normal_feature`，再按 XML 全量重建。
- 用户运行期权限不再直接基于 `permission_id` 做默认授权，而是先根据 `sys_normal_feature.enabled` 与 `sys_normal_feature_user_override` 计算有效功能，再展开到权限码。
- `sys_normal_user_feature` 旧模型已移除，不再参与初始化和运行期鉴权。

## Bootstrap 默认用户初始化（2026-05）

- 当前引导顺序继续扩展为：数据库与表结构初始化 -> 数据字典初始化 -> API 与权限初始化 -> 资源初始化 -> 默认用户初始化 -> 系统文件逻辑目录初始化。
- 默认保留用户统一以 `com.corwin.system.user.domain.model.DefaultUser` 为权威定义源。
- 引导程序会按固定 ID 同步以下保留账号：
  - `1-5` 为 `SYSTEM` 类型保留账号
  - `1000` 为内部管理员账号 `admin`
- 默认用户初始化时只按保留 ID 检查是否存在：对应 ID 已存在则直接跳过，不更新也不覆盖；仅在不存在时执行新增。
- `admin` 的初始密码固定为 `123456`，并按应用统一密码哈希算法加密入库，首次初始化后可直接用于登录。
- 其他 `SYSTEM` 类型保留账号只会写入随机密码哈希，它们不能通过普通登录接口登录。
- `sys_user` 表的普通自增区间从 `1001` 开始，`1-1000` 预留给有固定语义的系统账号和管理员账号。

## Bootstrap 系统文件逻辑目录初始化（2026-05）

- 系统文件逻辑目录初始化已从运行期 `ApplicationRunner` 移动到 `bootstrap` 引导流程，不再由应用服务启动时自动执行。
- 引导程序会在默认用户初始化之后，按 `com.corwin.system.file.published.FilePurpose` 为 `APPLICATION/system` 创建顶层用途目录。
- 当前只补齐缺失目录：目录已存在则跳过，不删除、不重建、不覆盖现有数据。

## 统一配置管理重构

### 一、需求背景

当前系统以单字段作为配置最小单位，默认值、数据库种子、类型解析、校验和前端编辑逻辑分散。密码策略、审计策略、Web Push 等天然属于同一业务规则的字段无法作为完整资源原子查询、校验和更新，现有分类型缓存也难以继续承载结构化配置。

### 二、当前现状

- framework 同时承担配置抽象、具体配置定义、`sys_config` SQL 和静态分类型缓存。
- system 通过旧 `ConfigStore`、JPA 实体和多个按 code 校验器管理配置。
- business 配置集中在跨领域的 `com.corwin.config` 包中。
- 用户偏好默认值与用户覆盖值已经存在，但默认值仍使用旧字段配置模型。
- 配置事务提交和内存刷新边界、敏感字段脱敏及并发 revision 更新需要统一处理。

### 三、目标

- 将配置最小单位改为强类型业务配置资源。
- 代码提供配置定义和安全默认值，数据库只保存管理员覆盖值或重置状态标记。
- Bean 实例化前完成配置发现、覆盖值加载和不可变快照注册。
- 配置更新通过完整资源校验、乐观锁持久化和事务提交后原子刷新完成。
- 统一使用 `cfg.view`、`cfg.edit`，不增加配置资源级权限。
- 用户个性化配置继续独立保存用户覆盖值，公共注册表负责提供用户偏好默认值。
- framework 保持纯抽象，system 负责配置表与管理用例，business 配置回归各自领域。

### 四、技术方案

- framework 新增 `ConfigSpec<T>`、`ConfigSpecProvider`、`ConfigSpecCatalog`、`ConfigSnapshot<T>`、`ConfigRegistry`、`Configs` 和启动加载 SPI。
- system 新建 `sys_config_value` 实体、仓储、启动期 JDBC loader、查询服务、命令服务和管理 API。
- 数据库无覆盖记录时直接使用代码默认值；重置时保留单调递增的 revision 状态标记，并忽略记录内容、读取当前代码默认值。
- 配置策略拆分为编辑策略和生效策略，支持动态生效与重启生效。
- 敏感字段查询只返回是否已配置，更新时支持保留、替换和清除语义。
- 用户偏好默认值聚合为公共配置资源，`UserConfigAppService` 按“用户覆盖值 > 公共默认值”合并。
- 数字格式拆分两个维度：用户个性化只保留千分位与小数点组合（`COMMA_DOT`/`PLAIN_DOT`/`DOT_COMMA`），小数位数与舍入模式由系统资源 `system.format.decimal-policy` 统一管理。
- 完整技术模型、API、表结构、异常规则和验收标准见 `server/framework/config-tech.md`。

### 五、任务拆分

| 任务编号 | 任务内容 | 状态 |
| --- | --- | --- |
| T1 | 重构 framework 配置定义、Catalog、Codec、Snapshot、Registry 和强类型读取基础 | 已完成 |
| T2 | 实现启动加载 SPI、EnvironmentPostProcessor 与 system JDBC loader | 已完成 |
| T3 | 新建 `sys_config_value` 持久化模型并同步 bootstrap schema，移除旧配置种子同步 | 已完成 |
| T4 | 实现配置查询、更新、校验、乐观锁、提交后刷新、重置和敏感字段处理 | 已完成 |
| T5 | 重构后端配置管理 API，统一 `cfg.view` 与 `cfg.edit` 权限 | 已完成 |
| T6 | 迁移 framework/system 配置资源、消费方和用户偏好默认值 | 已完成 |
| T7 | 迁移 business 配置并删除旧分类型配置体系和 `com.corwin.config` | 已完成 |
| T8 | 完成后端编译、空配置表启动和关键配置流程验证 | 待验证 |

### 六、执行规则

- 一次只执行一个任务。
- 每个任务完成后必须恢复后端可编译状态并暂停，等待用户确认。
- 不兼容旧配置数据、旧表结构和旧字段级配置 API。
- 当前任务范围只包含后端，前端改造另行安排。
