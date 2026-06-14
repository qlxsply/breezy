# 用户应用功能控制模型说明

## 1. 模型目标

本模型用于控制用户对产品应用及应用内功能的访问能力，支持以下场景：

- 按用户包统一开放应用能力。
- 按用户单独开放或关闭应用能力。
- 应用级支持完整功能和部分功能两种授权范围。
- 应用只负责前端入口控制。
- 功能可绑定权限码，用于后端 API 兜底鉴权。
- 不使用数据库外键，通过逻辑引用 ID 维护实体关系。

------

## 2. 核心概念

### 2.1 应用

应用表示一个完整的产品能力入口。

示例：

```text
JSON_FMT JSON工具
DATESOURCE 数据源管理 
```

应用主要控制：

- 前端菜单是否展示。
- 应用入口是否开放。
- 应用是否全局启用。

应用不绑定权限码。

对应实体：

```text
ProductApplication
```

------

### 2.2 功能

功能表示应用内部的细粒度能力。

示例：

```text
DATESOURCE.VIEW
DATESOURCE.EXPORT
DATESOURCE.BATCH_ANALYZE
DATESOURCE.ADVANCED_FILTER
```

功能主要控制：

- 页面入口。
- 子功能。
- 按钮。
- 操作。
- 后端 API 权限码绑定。

对应实体：

```text
ProductFeature
```

------

### 2.3 用户应用包

用户应用包表示一组用户共享的应用权益集合。

示例：

```text
FREE
VIP1
VIP2
ENTERPRISE
```

对应实体：

```text
UserApplicationPackage
UserApplicationPackageMember
```

------

## 3. 实体关系

### 3.1 应用与功能

```text
ProductApplication 1 ── N ProductFeature
```

关系说明：

- 一个应用可以包含多个功能。
- 功能通过 `applicationId` 逻辑引用所属应用。
- 应用关闭后，其下所有功能均不可用。

------

### 3.2 用户包与用户

```text
UserApplicationPackage 1 ── N UserApplicationPackageMember
```

关系说明：

- 一个用户包可以包含多个用户。
- 用户通过成员关系继承用户包的应用和功能授权。
- 用户可以属于多个用户包。

------

### 3.3 用户包与应用授权

```text
UserApplicationPackage 1 ── N UserPackageApplicationAccess
ProductApplication 1 ── N UserPackageApplicationAccess
```

关系说明：

- 用户包通过应用授权获得应用访问能力。
- 应用授权包含功能访问范围。

访问范围：

```text
FULL    完整功能
PARTIAL 部分功能
```

规则：

- `FULL`：拥有应用下全部已启用功能，后续新增功能自动可用。
- `PARTIAL`：仅拥有显式授权的功能，后续新增功能默认不可用。

------

### 3.4 用户包与功能授权

```text
UserApplicationPackage 1 ── N UserPackageFeatureAccess
ProductFeature 1 ── N UserPackageFeatureAccess
```

关系说明：

- 仅在应用授权范围为 `PARTIAL` 时需要维护功能授权。
- 如果用户包对某应用是 `FULL` 授权，则不需要配置该应用下的功能授权明细。

------

### 3.5 用户应用特例

```text
UserApplicationOverride
```

关系说明：

- 用于针对单个用户单独启用或禁用某个应用。
- 用户应用特例优先级高于用户包授权。
- 应用特例也支持 `FULL` 和 `PARTIAL` 功能访问范围。

特例类型：

```text
ENABLE  单独启用
DISABLE 单独禁用
NONE    无特例
```

------

### 3.6 用户功能特例

```text
UserFeatureOverride
```

关系说明：

- 用于针对单个用户单独启用或禁用某个功能。
- 功能特例用于处理用户级别的细粒度差异。
- 功能特例不能突破应用关闭和功能关闭。

------

### 3.7 功能与权限码

```text
ProductFeature 1 ── N ProductFeaturePermissionBinding
```

关系说明：

- 权限码只绑定到功能。
- 应用不绑定权限码。
- 没有绑定权限码的功能只参与前端控制或纯前端能力控制。
- 绑定权限码的功能可用于后端 API 鉴权。

------

## 4. 功能控制规则

### 4.1 应用是否可见

判断用户是否可以看到某个应用：

```text
1. ProductApplication.enabled 必须为 true
2. 如果存在 UserApplicationOverride.DISABLE，则不可见
3. 如果存在 UserApplicationOverride.ENABLE，则可见
4. 如果用户所属任一启用用户包拥有该应用授权，则可见
5. 否则不可见
```

------

### 4.2 功能是否可用

判断用户是否可以使用某个功能：

```text
1. ProductApplication.enabled 必须为 true
2. ProductFeature.enabled 必须为 true
3. 如果存在 UserApplicationOverride.DISABLE，则不可用
4. 如果存在 UserFeatureOverride.DISABLE，则不可用
5. 如果存在 UserApplicationOverride.ENABLE 且范围为 FULL，则可用
6. 如果存在 UserApplicationOverride.ENABLE 且范围为 PARTIAL，则需要 UserFeatureOverride.ENABLE
7. 如果用户包应用授权范围为 FULL，则可用
8. 如果用户包应用授权范围为 PARTIAL，则需要存在 UserPackageFeatureAccess
9. 否则不可用
```

------

## 5. 权限码生成规则

后端权限码只从功能维度生成：

```text
ProductFeaturePermissionBinding
```

生成逻辑：

```text
1. 先计算用户可用功能集合
2. 查询这些功能绑定的权限码
3. 汇总为用户最终权限码集合
```

示例：

```text
用户可用功能：
DATA_ANALYSIS.EXPORT
DATA_ANALYSIS.BATCH_ANALYZE

最终权限码：
data_analysis:export
data_analysis:batch_analyze
```

说明：

- 应用不生成权限码。
- 功能未绑定权限码时，不进入后端权限码集合。
- API 鉴权以权限码为准，作为绕过前端控制后的兜底安全机制。

------

## 6. 推荐裁决优先级

整体优先级如下：

```text
全局关闭 > 用户禁用特例 > 用户启用特例 > 用户包授权 > 默认不可用
```

细化为：

```text
1. 应用关闭，全部不可用
2. 功能关闭，该功能不可用
3. 用户应用禁用特例优先拒绝
4. 用户功能禁用特例优先拒绝
5. 用户应用启用特例优先生效
6. 用户功能启用特例补充生效
7. 用户包授权作为常规授权来源
8. 无授权则不可用
```

------

## 7. FULL 与 PARTIAL 的处理原则

### FULL

```text
拥有应用下全部已启用功能。
后续新增功能默认可用。
不需要维护功能授权明细。
```

适用场景：

```text
高级会员
企业版
内部管理员包
完整授权客户
```

### PARTIAL

```text
仅拥有显式配置的功能。
后续新增功能默认不可用。
必须维护功能授权明细。
```

适用场景：

```text
普通会员
试用用户
灰度开放
按功能售卖
运营定向开放
```

------

## 8. 数据一致性要求

由于模型不使用数据库外键，业务层需要保证以下一致性：

```text
1. ProductFeature.applicationId 必须指向有效应用
2. UserPackageApplicationAccess.applicationId 必须指向有效应用
3. UserPackageFeatureAccess.featureId 必须属于对应 applicationId
4. UserFeatureOverride.featureId 必须属于对应 applicationId
5. ProductFeaturePermissionBinding.featureId 必须属于对应 applicationId
6. FULL 授权下不应维护冗余功能授权明细
7. PARTIAL 授权下必须通过功能授权明细控制可用功能
```

------

## 9. 简化示例

### VIP1

```text
应用授权：
DATA_ANALYSIS = PARTIAL

功能授权：
DATA_ANALYSIS.VIEW
DATA_ANALYSIS.EXPORT
```

结果：

```text
可以看到数据分析应用
可以使用查看和导出功能
不能使用批量分析功能
新增功能默认不可用
```

### VIP2

```text
应用授权：
DATA_ANALYSIS = FULL
```

结果：

```text
可以看到数据分析应用
可以使用数据分析下全部已启用功能
新增功能默认可用
```

### 用户特例

```text
用户 A：
DATA_ANALYSIS = ENABLE + PARTIAL
DATA_ANALYSIS.BATCH_ANALYZE = ENABLE
```

结果：

```text
用户 A 可以单独使用批量分析功能
但不能突破应用关闭或功能关闭
```

------

## 10. 总结

该模型将访问控制拆分为三层：

```text
应用层：控制前端入口和应用全局开关
功能层：控制应用内细粒度能力
权限码层：控制后端 API 安全边界
```

授权来源包括两类：

```text
用户包授权：用于批量管理用户权益
用户特例：用于单个用户差异化开关
```

应用级 `FULL/PARTIAL` 机制用于控制功能扩展后的默认授权策略：

```text
FULL    面向完整权益，新增功能自动继承
PARTIAL 面向精确授权，新增功能默认关闭
```