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

interface RuntimeConfigParseOptions {
  requireComplete?: boolean;
}

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

export function parseRuntimeConfig(
  payload: unknown,
  options: RuntimeConfigParseOptions = {},
): RuntimeConfig {
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
  if (
    options.requireComplete &&
    (!Object.hasOwn(record, "apiBaseUrl") || !Object.hasOwn(record, "bootstrapPath"))
  ) {
    throw new Error("运行时配置必须显式声明 apiBaseUrl 和 bootstrapPath");
  }

  const apiBaseUrl = normalizePath(record.apiBaseUrl, "apiBaseUrl", DEFAULT_CONFIG.apiBaseUrl);
  const bootstrapPath = normalizePath(
    record.bootstrapPath,
    "bootstrapPath",
    DEFAULT_CONFIG.bootstrapPath,
  );
  if (apiBaseUrl === "/" || bootstrapPath === "/") {
    throw new Error("apiBaseUrl 和 bootstrapPath 不能是根路径");
  }
  if (!/^\/[A-Za-z][A-Za-z0-9_-]*$/.test(apiBaseUrl)) {
    throw new Error("apiBaseUrl 必须是单个安全路径段");
  }
  if (apiBaseUrl === "/admin" || apiBaseUrl === "/healthz") {
    throw new Error("apiBaseUrl 与前端保留路径冲突");
  }
  return Object.freeze({ apiBaseUrl, bootstrapPath });
}

export async function loadRuntimeConfig(
  fetcher: typeof fetch = fetch,
  options: RuntimeConfigParseOptions = {},
): Promise<RuntimeConfig> {
  const response = await fetcher(RUNTIME_CONFIG_URL, {
    cache: "no-store",
    credentials: "same-origin",
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`运行时配置加载失败：HTTP ${response.status}`);
  }

  runtimeConfig = parseRuntimeConfig(await response.json(), options);
  return runtimeConfig;
}

export function ensureRuntimeConfigLoaded(
  options: RuntimeConfigParseOptions = {},
): Promise<RuntimeConfig> {
  if (!loadingPromise) {
    loadingPromise = loadRuntimeConfig(fetch, options).catch((error) => {
      loadingPromise = null;
      throw error;
    });
  }
  return loadingPromise;
}

export function reloadRuntimeConfig(
  options: RuntimeConfigParseOptions = {},
): Promise<RuntimeConfig> {
  loadingPromise = null;
  return ensureRuntimeConfigLoaded(options);
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
