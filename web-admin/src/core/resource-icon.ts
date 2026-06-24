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
  return baseIcon(`
    <path d="M3.75 7.9c0-1.02.83-1.85 1.85-1.85h4.05c.5 0 .97.2 1.32.55l1.03 1.03c.35.35.82.55 1.32.55h5.08c1.02 0 1.85.83 1.85 1.85v7.02c0 1.02-.83 1.85-1.85 1.85H5.6a1.85 1.85 0 0 1-1.85-1.85z" />
    <path d="M4 10h16" />
  `);
}

function iconDashboard(): string {
  return baseIcon(`
    <path d="M4.75 5.9c0-.64.51-1.15 1.15-1.15h4.2c.64 0 1.15.51 1.15 1.15v5.2c0 .64-.51 1.15-1.15 1.15H5.9c-.64 0-1.15-.51-1.15-1.15z" />
    <path d="M12.75 5.9c0-.64.51-1.15 1.15-1.15h4.2c.64 0 1.15.51 1.15 1.15v2.7c0 .64-.51 1.15-1.15 1.15h-4.2c-.64 0-1.15-.51-1.15-1.15z" />
    <path d="M12.75 12.9c0-.64.51-1.15 1.15-1.15h4.2c.64 0 1.15.51 1.15 1.15v5.2c0 .64-.51 1.15-1.15 1.15h-4.2c-.64 0-1.15-.51-1.15-1.15z" />
    <path d="M4.75 15.4c0-.64.51-1.15 1.15-1.15h4.2c.64 0 1.15.51 1.15 1.15v2.7c0 .64-.51 1.15-1.15 1.15H5.9c-.64 0-1.15-.51-1.15-1.15z" />
  `);
}

function iconSetting(): string {
  return baseIcon(`
    <path d="M12 14.75a2.75 2.75 0 1 0 0-5.5 2.75 2.75 0 0 0 0 5.5z" />
    <path d="M18.35 13.05c.05-.34.08-.69.08-1.05s-.03-.71-.08-1.05l1.48-1.16c.2-.16.25-.45.12-.67l-1.4-2.42a.53.53 0 0 0-.64-.23l-1.74.7a7.46 7.46 0 0 0-1.82-1.05l-.27-1.86A.52.52 0 0 0 13.56 3h-3.12c-.26 0-.48.19-.52.45l-.27 1.86a7.46 7.46 0 0 0-1.82 1.05l-1.74-.7a.53.53 0 0 0-.64.23l-1.4 2.42c-.13.22-.08.51.12.67l1.48 1.16c-.05.34-.08.69-.08 1.05s.03.71.08 1.05l-1.48 1.16a.52.52 0 0 0-.12.67l1.4 2.42c.13.22.4.32.64.23l1.74-.7c.55.44 1.16.79 1.82 1.05l.27 1.86c.04.26.26.45.52.45h3.12c.26 0 .48-.19.52-.45l.27-1.86a7.46 7.46 0 0 0 1.82-1.05l1.74.7c.24.09.51-.01.64-.23l1.4-2.42a.52.52 0 0 0-.12-.67z" />
  `);
}

function iconUser(): string {
  return baseIcon(`
    <path d="M12 11.35a3.55 3.55 0 1 0 0-7.1 3.55 3.55 0 0 0 0 7.1z" />
    <path d="M4.95 19.25c.6-3.35 3.42-5.65 7.05-5.65s6.45 2.3 7.05 5.65" />
  `);
}

function iconShield(): string {
  return baseIcon(`
    <path d="M12 3.9 5.85 6.25v4.58c0 3.85 2.5 7.28 6.15 8.67 3.65-1.39 6.15-4.82 6.15-8.67V6.25z" />
    <path d="m9.25 12.15 1.85 1.85 3.9-4.1" />
  `);
}

function iconGrid(): string {
  return baseIcon(`
    <path d="M5.8 4.75h4.05c.58 0 1.05.47 1.05 1.05v4.05c0 .58-.47 1.05-1.05 1.05H5.8c-.58 0-1.05-.47-1.05-1.05V5.8c0-.58.47-1.05 1.05-1.05z" />
    <path d="M14.15 4.75h4.05c.58 0 1.05.47 1.05 1.05v4.05c0 .58-.47 1.05-1.05 1.05h-4.05c-.58 0-1.05-.47-1.05-1.05V5.8c0-.58.47-1.05 1.05-1.05z" />
    <path d="M5.8 13.1h4.05c.58 0 1.05.47 1.05 1.05v4.05c0 .58-.47 1.05-1.05 1.05H5.8c-.58 0-1.05-.47-1.05-1.05v-4.05c0-.58.47-1.05 1.05-1.05z" />
    <path d="M14.15 13.1h4.05c.58 0 1.05.47 1.05 1.05v4.05c0 .58-.47 1.05-1.05 1.05h-4.05c-.58 0-1.05-.47-1.05-1.05v-4.05c0-.58.47-1.05 1.05-1.05z" />
  `);
}

function iconFile(): string {
  return baseIcon(`
    <path d="M7.15 3.95h6.05c.44 0 .86.18 1.17.49l3.19 3.19c.31.31.49.73.49 1.17v9.05c0 .91-.74 1.65-1.65 1.65H7.15c-.91 0-1.65-.74-1.65-1.65V5.6c0-.91.74-1.65 1.65-1.65z" />
    <path d="M13.25 4.25v3.4c0 .69.56 1.25 1.25 1.25h3.25" />
    <path d="M8.7 12.8h6.6" />
    <path d="M8.7 15.75h4.4" />
  `);
}

function iconTool(): string {
  return baseIcon(`
    <path d="M16.85 4.55 14.3 7.1l.5 2.2 2.2.5 2.55-2.55a4.8 4.8 0 0 1-6.35 6.35l-5.35 5.35a2.05 2.05 0 0 1-2.9-2.9l5.35-5.35a4.8 4.8 0 0 1 6.55-6.15z" />
    <path d="M6.9 17.1h.01" />
  `);
}

function iconPlaceholder(): string {
  return baseIcon(`
    <path d="M5.85 6.15h12.3c1.02 0 1.85.83 1.85 1.85v8c0 1.02-.83 1.85-1.85 1.85H5.85A1.85 1.85 0 0 1 4 16V8c0-1.02.83-1.85 1.85-1.85z" />
    <path d="M7.6 9.65h8.8" />
    <path d="M7.6 12.15h8.8" />
    <path d="M7.6 14.65h5.2" />
  `);
}

function baseIcon(pathMarkup: string): string {
  return `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      width="1em"
      height="1em"
      fill="none"
      color="currentColor"
      aria-hidden="true"
      focusable="false"
    >
      <g
        stroke="currentColor"
        stroke-width="1.75"
        stroke-linecap="round"
        stroke-linejoin="round"
        vector-effect="non-scaling-stroke"
      >
        ${pathMarkup}
      </g>
    </svg>
  `;
}
