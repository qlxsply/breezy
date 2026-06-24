const iconSvgMap = new Map<string, string>([
  [["folder", "directory", "catalog", "menu-group"].join("|"), iconFolder()],
  [["dashboard", "workbench", "home", "desktop"].join("|"), iconDashboard()],
  [["setting", "settings", "config", "configs", "system", "gear"].join("|"), iconSetting()],
  [["user", "users", "account", "accounts", "profile"].join("|"), iconUser()],
  [["role", "shield", "permission", "security", "auth"].join("|"), iconShield()],
  [["api", "app", "application", "grid", "module", "feature"].join("|"), iconGrid()],
  [["file", "files", "doc", "document", "log"].join("|"), iconFile()],
  [["tool", "tools", "diagnostic", "wrench", "build"].join("|"), iconTool()],
]);

const defaultIconUrl = svgToDataUrl(iconPlaceholder());

export function resolveResourceIconUrl(iconCode?: string | null): string {
  const normalized = normalizeIconCode(iconCode);
  if (!normalized) {
    return defaultIconUrl;
  }

  for (const [aliasGroup, svg] of iconSvgMap.entries()) {
    const aliases = aliasGroup.split("|");
    if (aliases.some((alias) => normalized.includes(alias))) {
      return svgToDataUrl(svg);
    }
  }

  return defaultIconUrl;
}

function normalizeIconCode(iconCode?: string | null): string {
  return String(iconCode ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s_:/\\-]+/g, "-");
}

function svgToDataUrl(svg: string): string {
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

function iconFolder(): string {
  return baseIcon('<path d="M4 7.75A1.75 1.75 0 0 1 5.75 6h4.19c.47 0 .92.19 1.25.52l1.04 1.04c.33.33.78.52 1.25.52h4.77A1.75 1.75 0 0 1 20 9.83v7.42A1.75 1.75 0 0 1 18.25 19H5.75A1.75 1.75 0 0 1 4 17.25z" />');
}

function iconDashboard(): string {
  return baseIcon('<path d="M5.75 5h4.5A.75.75 0 0 1 11 5.75v4.5a.75.75 0 0 1-.75.75h-4.5A.75.75 0 0 1 5 10.25v-4.5A.75.75 0 0 1 5.75 5m8 0h4.5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-.75.75h-4.5a.75.75 0 0 1-.75-.75v-4.5a.75.75 0 0 1 .75-.75m-8 8h4.5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-.75.75h-4.5A.75.75 0 0 1 5 18.25v-4.5a.75.75 0 0 1 .75-.75m8 0h4.5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-.75.75h-4.5a.75.75 0 0 1-.75-.75v-4.5a.75.75 0 0 1 .75-.75" />');
}

function iconSetting(): string {
  return baseIcon('<path d="M9.78 4.72a2.5 2.5 0 0 1 4.44 0l.3.58a1 1 0 0 0 1.13.5l.64-.15a2.5 2.5 0 0 1 3.15 3.14l-.15.65a1 1 0 0 0 .5 1.12l.58.3a2.5 2.5 0 0 1 0 4.44l-.58.3a1 1 0 0 0-.5 1.13l.15.64a2.5 2.5 0 0 1-3.14 3.15l-.65-.15a1 1 0 0 0-1.12.5l-.3.58a2.5 2.5 0 0 1-4.44 0l-.3-.58a1 1 0 0 0-1.13-.5l-.64.15a2.5 2.5 0 0 1-3.15-3.14l.15-.65a1 1 0 0 0-.5-1.12l-.58-.3a2.5 2.5 0 0 1 0-4.44l.58-.3a1 1 0 0 0 .5-1.13l-.15-.64a2.5 2.5 0 0 1 3.14-3.15l.65.15a1 1 0 0 0 1.12-.5zM12 9.25A2.75 2.75 0 1 0 12 14.75 2.75 2.75 0 0 0 12 9.25" />');
}

function iconUser(): string {
  return baseIcon('<path d="M12 12a3.5 3.5 0 1 0-3.5-3.5A3.5 3.5 0 0 0 12 12m0 1.5c-3.32 0-6 1.9-6 4.25 0 .41.34.75.75.75h10.5c.41 0 .75-.34.75-.75 0-2.35-2.68-4.25-6-4.25" />');
}

function iconShield(): string {
  return baseIcon('<path d="M12 4.5 6.5 6.6v4.28c0 3.47 2.31 6.71 5.5 7.62 3.19-.91 5.5-4.15 5.5-7.62V6.6zm0 4.12a2.38 2.38 0 1 1 0 4.76 2.38 2.38 0 0 1 0-4.76" />');
}

function iconGrid(): string {
  return baseIcon('<path d="M6.75 5h3.5c.97 0 1.75.78 1.75 1.75v3.5c0 .97-.78 1.75-1.75 1.75h-3.5A1.75 1.75 0 0 1 5 10.25v-3.5C5 5.78 5.78 5 6.75 5m7 0h3.5c.97 0 1.75.78 1.75 1.75v3.5c0 .97-.78 1.75-1.75 1.75h-3.5A1.75 1.75 0 0 1 12 10.25v-3.5C12 5.78 12.78 5 13.75 5m-7 7h3.5c.97 0 1.75.78 1.75 1.75v3.5c0 .97-.78 1.75-1.75 1.75h-3.5A1.75 1.75 0 0 1 5 17.25v-3.5c0-.97.78-1.75 1.75-1.75m7 0h3.5c.97 0 1.75.78 1.75 1.75v3.5c0 .97-.78 1.75-1.75 1.75h-3.5A1.75 1.75 0 0 1 12 17.25v-3.5c0-.97.78-1.75 1.75-1.75" />');
}

function iconFile(): string {
  return baseIcon('<path d="M7.75 4h5.69c.46 0 .9.18 1.22.5l2.84 2.84c.32.32.5.76.5 1.22v9.69A1.75 1.75 0 0 1 16.25 20h-8.5A1.75 1.75 0 0 1 6 18.25v-12.5C6 4.78 6.78 4 7.75 4m5.5 1.5v2.75c0 .41.34.75.75.75h2.75" />');
}

function iconTool(): string {
  return baseIcon('<path d="M14.78 5.1a4.2 4.2 0 0 0-4.56 5.64L5.3 15.66a1.9 1.9 0 1 0 2.69 2.69l4.92-4.92a4.2 4.2 0 0 0 5.64-4.56l-2.38 2.38-2.04-.34-.34-2.04z" />');
}

function iconPlaceholder(): string {
  return baseIcon('<path d="M5.75 6h12.5c.97 0 1.75.78 1.75 1.75v8.5c0 .97-.78 1.75-1.75 1.75H5.75A1.75 1.75 0 0 1 4 16.25v-8.5C4 6.78 4.78 6 5.75 6m2.5 3.5h7.5M8.25 12h7.5M8.25 14.5h4.5" />');
}

function baseIcon(pathMarkup: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"><g stroke="#475569" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${pathMarkup}</g></svg>`;
}
