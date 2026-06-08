// /vite.config.ts
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

/**
 * Vite config:
 * - Vue SFC support
 * - SPA history fallback in dev/preview
 */
export default defineConfig({
  plugins: [vue()],
  appType: "spa",
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8910",
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
});
