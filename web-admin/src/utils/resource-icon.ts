import { API_BASE_URL } from "../api/http";

export function resolveResourceIconUrl(icon?: string | null): string | null {
  const normalized = normalizeIcon(icon);
  if (!normalized) {
    return null;
  }
  if (isFrontendResourcePath(normalized)) {
    return `${API_BASE_URL}/public/frontend-resources/${encodeResourcePath(normalized)}`;
  }
  return `${API_BASE_URL}/public/static-files/${encodeURIComponent(normalized)}`;
}

function normalizeIcon(icon?: string | null): string | null {
  if (typeof icon !== "string") {
    return null;
  }
  const normalized = icon.trim().replace(/\\/g, "/").replace(/^\/+/, "");
  return normalized ? normalized : null;
}

function isFrontendResourcePath(icon: string): boolean {
  return icon.includes("/") || /\.(svg|png|jpe?g|gif|webp|bmp|ico)$/i.test(icon);
}

function encodeResourcePath(resourcePath: string): string {
  return resourcePath
    .split("/")
    .filter((segment) => segment.length > 0)
    .map((segment) => encodeURIComponent(segment))
    .join("/");
}
