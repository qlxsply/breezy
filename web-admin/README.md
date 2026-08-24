# Breezy Web Admin

React 19 + Next.js 16 管理后台，采用静态导出、同源 Cookie 会话、真实 App Router 路由和 feature 垂直切片。

## 目录边界

- `src/app`：路由、布局、路由守卫与应用组合。
- `src/features`：页面、API、模型、权限和 feature UI。
- `src/shared`：Transport、通用 Hook、Bz UI 和后台公共组件。
- `src/runtime`：会话、SSE、Push、Service Worker 与运行时桥接。
- `src/styles`：仅主题 token 和基础 reset。

依赖方向为 `app -> runtime/features/shared`、`features -> shared`。跨 feature 只能通过 `public` 契约或明确的权限入口访问，规则由 `npm run check:architecture` 强制检查。

## 本地开发

```powershell
npm ci
npm run dev
```

开发代理通过 `ADMIN_API_UPSTREAM` 指向后端 HTTP/HTTPS Origin，默认值为 `http://localhost:8910`。

## 质量门禁

```powershell
npm audit --omit=dev
npm run quality
npm run build
npm run check:bundle
npx playwright install chromium
npm run test:e2e
```

- `quality` 包含格式、lint、类型、架构和 Vitest 检查。
- `build` 使用 webpack 严格 CSS 分块生成 `dist/` 静态产物。
- `check:bundle` 检查路由首屏、单 chunk 和跨路由 page chunk 隔离。
- Playwright 使用静态 preview 和浏览器侧 API mock，不依赖本地数据库。

## 部署

- `/runtime-config.json` 只接受同源路径配置。
- `/api/**` 由 Preview/Nginx 反向代理到可信后端 Origin。
- 认证使用 HttpOnly Cookie，写请求使用 CSRF Cookie/Header 双提交。
- Nginx 对 JS、CSS、JSON、SVG 和文本资源启用 gzip。
