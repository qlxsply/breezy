export interface PublicRuntimeEnv {
  apiBaseUrl: string;
  bootstrapBaseUrl?: string;
  bootstrapPath?: string;
  bootstrapUrl?: string;
}

function readEnv(name: string): string | undefined {
  const value = process.env[name];
  if (typeof value !== "string") {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed || undefined;
}

export function getPublicRuntimeEnv(): PublicRuntimeEnv {
  return {
    apiBaseUrl: readEnv("NEXT_PUBLIC_API_BASE_URL") ?? "http://localhost:8910/api",
    bootstrapBaseUrl: readEnv("NEXT_PUBLIC_BOOTSTRAP_BASE_URL"),
    bootstrapPath: readEnv("NEXT_PUBLIC_BOOTSTRAP_PATH"),
    bootstrapUrl: readEnv("NEXT_PUBLIC_BOOTSTRAP_URL"),
  };
}

export const API_BASE_URL = getPublicRuntimeEnv().apiBaseUrl;
