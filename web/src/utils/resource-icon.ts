import { API_BASE_URL } from "../api/http";

export function resolveResourceIconUrl(icon?: string | null): string | null {
  const normalized = normalizeIcon(icon);
  if (!normalized) {
    return null;
  }
  return `${API_BASE_URL}/public/frontend-resources/${encodeResourcePath(normalized)}`;
}

function normalizeIcon(icon?: string | null): string | null {
  if (typeof icon !== "string") {
    return null;
  }
  const normalized = icon.trim().replace(/\\/g, "/").replace(/^\/+/, "");
  return normalized ? normalized : null;
}

function encodeResourcePath(resourcePath: string): string {
  return resourcePath
    .split("/")
    .filter((segment) => segment.length > 0)
    .map((segment) => encodeURIComponent(segment))
    .join("/");
}
