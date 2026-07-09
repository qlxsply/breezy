import type { ResourceNodeType } from "@admin/types/resource-admin";

const defaultDirectoryIconUrl = "/admin-icons/default-directory.svg";
const defaultMenuIconUrl = "/admin-icons/default-menu.svg";

const resourceIconUrlMap: Record<string, string> = {
  overview: "/admin-icons/overview.svg",
  dashboard: "/admin-icons/dashboard.svg",
  settings: "/admin-icons/settings.svg",
  setting: "/admin-icons/settings.svg",
  platform: "/admin-icons/settings.svg",
  config: "/admin-icons/settings.svg",
  link: "/admin-icons/link.svg",
  book: "/admin-icons/book.svg",
  folder: "/admin-icons/folder.svg",
  directory: "/admin-icons/folder.svg",
  monitor: "/admin-icons/monitor.svg",
  chart: "/admin-icons/chart.svg",
  analytics: "/admin-icons/chart.svg",
  report: "/admin-icons/chart.svg",
  shield: "/admin-icons/shield.svg",
  role: "/admin-icons/shield.svg",
  policy: "/admin-icons/shield.svg",
  permission: "/admin-icons/shield.svg",
  users: "/admin-icons/users.svg",
  user: "/admin-icons/users.svg",
  customer: "/admin-icons/users.svg",
  "customer-user": "/admin-icons/users.svg",
  history: "/admin-icons/history.svg",
  audit: "/admin-icons/audit.svg",
  log: "/admin-icons/audit.svg",
  spark: "/admin-icons/spark.svg",
  feature: "/admin-icons/spark.svg",
  "customer-chart": "/admin-icons/chart.svg",
};

export function resolveResourceIconUrl(
  iconCode?: string | null,
  nodeType?: ResourceNodeType | null,
): string | null {
  const normalized = normalizeIconCode(iconCode);
  if (normalized) {
    const mappedUrl = resourceIconUrlMap[normalized];
    if (mappedUrl) {
      return mappedUrl;
    }
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
    .replace(/[\s_:/\\-]+/g, "-");
}
