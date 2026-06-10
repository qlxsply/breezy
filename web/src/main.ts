// /src/main.ts
import "./styles/theme.css";
import "./styles/list-page.css";
import "./styles/bz-ui.css";

import { createApp } from "vue";

import App from "./App.vue";
import { BzUi } from "./components/bz";
import { ensureAuthLoaded } from "./registry/auth.registry";
import { initSseLifecycle } from "./registry/sse.registry";
import { ensureUserToolPermissionsLoaded } from "./registry/user-tool-permissions.registry";
import { ensureUserToolsLoaded } from "./registry/user-tools.registry";
import router, { initDynamicRoutes } from "./router";

async function bootstrap(): Promise<void> {
  // 初始化动态路由
  await initDynamicRoutes();
  await ensureUserToolsLoaded();
  await ensureAuthLoaded();
  await ensureUserToolPermissionsLoaded();

  // 初始化 SSE 生命周期
  initSseLifecycle();

  // 挂载应用
  createApp(App).use(router).use(BzUi).mount("#app");
}

bootstrap().catch((error) => {
  console.error("应用启动失败:", error);
});
