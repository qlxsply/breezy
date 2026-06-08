import { fileURLToPath, URL } from "node:url";

import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

export default defineConfig({
  base: "/admin/",
  plugins: [vue()],
  appType: "spa",
  server: {
    port: 5174,
    proxy: {
      "/api": {
        target: "http://localhost:8910",
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
});
