import type { ResourceNodeType } from "@admin/types/resource-admin";

const defaultDirectoryIconUrl = "/admin-icons/default-directory.svg";
const defaultMenuIconUrl = "/admin-icons/default-menu.svg";

export function resolveResourceIconUrl(
  iconCode?: string | null,
  nodeType?: ResourceNodeType | null,
): string | null {
  const normalized = normalizeIconCode(iconCode);
  if (normalized) {
    return `/admin-icons/${normalized}.svg`;
  }

  if (nodeType === "DIRECTORY") {
    return defaultDirectoryIconUrl;
  }

  if (nodeType === "MENU") {
    return defaultMenuIconUrl;
  }

  return null;
}

function normalizeIconCode(iconCode?: string | null): string {
  return String(iconCode ?? "")
    .trim()
    .toLowerCase()
    .replace(/\.svg$/i, "")
    .replace(/[\s_:/\\]+/g, "-")
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}
