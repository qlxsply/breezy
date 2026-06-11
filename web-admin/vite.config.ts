import { fileURLToPath, URL } from "node:url";

import vue from "@vitejs/plugin-vue";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");

  return {
    base: env.VITE_APP_BASE || "/admin/",
    plugins: [vue()],
    appType: "spa",
    server: {
      port: Number(env.VITE_DEV_PORT || 5174),
      proxy: {
        "/api": {
          target: env.VITE_DEV_PROXY_TARGET || "http://localhost:8910",
          changeOrigin: true,
        },
      },
    },
    resolve: {
      alias: {
        "@admin": fileURLToPath(new URL("./src", import.meta.url)),
        "@shared": fileURLToPath(new URL("../web/src", import.meta.url)),
      },
    },
  };
});
