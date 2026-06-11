// /src/utils/bootstrapConfig.ts
import { readJson } from "./storage";

export interface BootstrapConfig {
  baseUrl?: string;
  bootstrapPath?: string;
  bootstrapUrl?: string;
}

const STORAGE_KEY = "breezy:bootstrap-config";

function readWindowConfig(): BootstrapConfig {
  if (typeof window === "undefined") return {};
  const win = window as typeof window & { __BREEZY_BOOTSTRAP__?: BootstrapConfig };
  return win.__BREEZY_BOOTSTRAP__ ?? {};
}

function readQueryConfig(): BootstrapConfig {
  if (typeof window === "undefined") return {};
  const params = new URLSearchParams(window.location.search);
  return {
    baseUrl: params.get("bootstrapBase") ?? undefined,
    bootstrapPath: params.get("bootstrapPath") ?? undefined,
    bootstrapUrl: params.get("bootstrapUrl") ?? undefined,
  };
}

function readEnvConfig(): BootstrapConfig {
  const env = import.meta.env;
  return {
    baseUrl: env.VITE_BOOTSTRAP_BASE_URL || undefined,
    bootstrapPath: env.VITE_BOOTSTRAP_PATH || undefined,
    bootstrapUrl: env.VITE_BOOTSTRAP_URL || undefined,
  };
}

function joinPath(base: string, path: string): string {
  const baseClean = base.endsWith("/") ? base.slice(0, -1) : base;
  const pathClean = path.startsWith("/") ? path : `/${path}`;
  return `${baseClean}${pathClean}`;
}

export function getBootstrapConfig(): BootstrapConfig {
  const stored = typeof window === "undefined" ? {} : readJson<BootstrapConfig>(STORAGE_KEY, {});
  return {
    ...readEnvConfig(),
    ...readWindowConfig(),
    ...stored,
    ...readQueryConfig(),
  };
}

export function resolveBootstrapUrl(defaultPath = "/api/admin/menu-resources"): string {
  const cfg = getBootstrapConfig();
  if (cfg.bootstrapUrl) return cfg.bootstrapUrl;

  const path = cfg.bootstrapPath || defaultPath;
  if (!cfg.baseUrl) return path;

  const baseUrl = cfg.baseUrl.trim();
  if (!baseUrl) return path;

  if (/^https?:\/\//i.test(baseUrl)) {
    try {
      return new URL(path, baseUrl).toString();
    } catch {
      return path;
    }
  }

  return joinPath(baseUrl, path);
}
