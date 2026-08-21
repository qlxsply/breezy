export interface RuntimeConfig {
  apiBaseUrl: string;
  bootstrapPath: string;
}

const RUNTIME_CONFIG_URL = "/runtime-config.json";
const DEFAULT_CONFIG: RuntimeConfig = {
  apiBaseUrl: "/api",
  bootstrapPath: "/admin/menu-resources",
};
const CONFIG_KEYS = new Set<keyof RuntimeConfig>(["apiBaseUrl", "bootstrapPath"]);

let runtimeConfig: RuntimeConfig = DEFAULT_CONFIG;
let loadingPromise: Promise<RuntimeConfig> | null = null;

function normalizePath(value: unknown, field: keyof RuntimeConfig, fallback: string): string {
  const path = value === undefined ? fallback : String(value).trim();
  if (!path || !path.startsWith("/") || path.startsWith("//")) {
    throw new Error(`${field} 必须是以单个 / 开头的同源路径`);
  }
  if (path.includes("\\") || path.includes("?") || path.includes("#")) {
    throw new Error(`${field} 不能包含反斜杠、查询参数或哈希`);
  }
  return path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
}

export function parseRuntimeConfig(payload: unknown): RuntimeConfig {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("运行时配置必须是 JSON 对象");
  }

  const record = payload as Record<string, unknown>;
  const unknownKey = Object.keys(record).find(
    (key) => !CONFIG_KEYS.has(key as keyof RuntimeConfig),
  );
  if (unknownKey) {
    throw new Error(`运行时配置包含未知字段：${unknownKey}`);
  }

  return Object.freeze({
    apiBaseUrl: normalizePath(record.apiBaseUrl, "apiBaseUrl", DEFAULT_CONFIG.apiBaseUrl),
    bootstrapPath: normalizePath(
      record.bootstrapPath,
      "bootstrapPath",
      DEFAULT_CONFIG.bootstrapPath,
    ),
  });
}

export async function loadRuntimeConfig(fetcher: typeof fetch = fetch): Promise<RuntimeConfig> {
  const response = await fetcher(RUNTIME_CONFIG_URL, {
    cache: "no-store",
    credentials: "same-origin",
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`运行时配置加载失败：HTTP ${response.status}`);
  }

  runtimeConfig = parseRuntimeConfig(await response.json());
  return runtimeConfig;
}

export function ensureRuntimeConfigLoaded(): Promise<RuntimeConfig> {
  if (!loadingPromise) {
    loadingPromise = loadRuntimeConfig().catch((error) => {
      loadingPromise = null;
      throw error;
    });
  }
  return loadingPromise;
}

export function reloadRuntimeConfig(): Promise<RuntimeConfig> {
  loadingPromise = null;
  return ensureRuntimeConfigLoaded();
}

export function getRuntimeConfig(): RuntimeConfig {
  return runtimeConfig;
}

export function resolveApiUrl(endpoint: string): string {
  const normalizedEndpoint = normalizePath(endpoint, "apiBaseUrl", "/");
  const { apiBaseUrl } = runtimeConfig;
  return apiBaseUrl === "/" ? normalizedEndpoint : `${apiBaseUrl}${normalizedEndpoint}`;
}

export function resolveBootstrapUrl(): string {
  return resolveApiUrl(runtimeConfig.bootstrapPath);
}
