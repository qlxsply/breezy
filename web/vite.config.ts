// /vite.config.ts
import vue from "@vitejs/plugin-vue";
import { defineConfig, loadEnv } from "vite";

/**
 * Vite config:
 * - Vue SFC support
 * - SPA history fallback in dev/preview
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");

  return {
    base: env.VITE_APP_BASE || "/",
    plugins: [vue()],
    appType: "spa",
    server: {
      port: Number(env.VITE_DEV_PORT || 5173),
      proxy: {
        "/api": {
          target: env.VITE_DEV_PROXY_TARGET || "http://localhost:8910",
          changeOrigin: true,
        },
      },
    },
    resolve: {
      dedupe: [
        "@codemirror/state",
        "@codemirror/view",
        "@codemirror/language",
        "@codemirror/commands",
        "@codemirror/lint",
        "@codemirror/search",
        "@codemirror/lang-json",
        "@lezer/highlight",
        "@lezer/common",
        "@lezer/lr",
      ],
    },
  };
});
