// /src/main.ts
import "./styles/theme.css";
import "./styles/list-page.css";
import "./styles/admin-page.css";
import "./styles/bz-ui.css";

import { createApp } from "vue";

import App from "./App.vue";
import { BzUi } from "./components/bz";
import { ensureAuthLoaded, getCurrentUserType } from "./registry/auth.registry";
import { ensureRegistryLoaded } from "./registry/bootstrap";
import { ensurePermissionsLoaded } from "./registry/permissions.registry";
import { initSseLifecycle } from "./registry/sse.registry";
import router, { initDynamicRoutes } from "./router";

async function bootstrap(): Promise<void> {
  await ensureAuthLoaded();

  if (getCurrentUserType() === "INTERNAL") {
    await initDynamicRoutes();
    await ensureRegistryLoaded();
    await ensurePermissionsLoaded();
  }

  // 初始化 SSE 生命周期
  initSseLifecycle();

  // 挂载应用
  createApp(App).use(router).use(BzUi).mount("#app");
}

bootstrap().catch((error) => {
  console.error("应用启动失败:", error);
});
