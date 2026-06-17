import { fileURLToPath, URL } from "node:url";

import vue from "@vitejs/plugin-vue";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  const appBase = env.VITE_APP_BASE || "/admin/";

  const baseRedirectPlugin = {
    name: "admin-base-redirect",
    configureServer(server: { middlewares: { use: (handler: (req: { url?: string }, res: { statusCode?: number; setHeader: (name: string, value: string) => void; end: () => void }, next: () => void) => void) => void } }) {
      server.middlewares.use((req, res, next) => {
        if (req.url === appBase.slice(0, -1)) {
          res.statusCode = 302;
          res.setHeader("Location", appBase);
          res.end();
          return;
        }
        next();
      });
    },
    configurePreviewServer(server: { middlewares: { use: (handler: (req: { url?: string }, res: { statusCode?: number; setHeader: (name: string, value: string) => void; end: () => void }, next: () => void) => void) => void } }) {
      server.middlewares.use((req, res, next) => {
        if (req.url === appBase.slice(0, -1)) {
          res.statusCode = 302;
          res.setHeader("Location", appBase);
          res.end();
          return;
        }
        next();
      });
    },
  };

  return {
    base: appBase,
    plugins: [vue(), baseRedirectPlugin],
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
