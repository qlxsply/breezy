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
const defaultDirectoryIconUrl = svgToDataUrl(iconDirectoryPlaceholder());
const defaultMenuIconUrl = svgToDataUrl(iconMenuPlaceholder());

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

  if (normalized === "directory") {
    return defaultDirectoryIconUrl;
  }

  if (normalized === "menu") {
    return defaultMenuIconUrl;
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
      <path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/>
  `);
}

function iconDashboard(): string {
  return baseIcon(`
      <rect width="7" height="9" x="3" y="3" rx="1"/>
  <rect width="7" height="5" x="14" y="3" rx="1"/>
  <rect width="7" height="9" x="14" y="12" rx="1"/>
  <rect width="7" height="5" x="3" y="16" rx="1"/>
  `);
}

function iconSetting(): string {
  return baseIcon(`
      <path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"/>
      <circle cx="12" cy="12" r="3"/>
  `);
}

function iconUser(): string {
  return baseIcon(`
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
  `);
}

function iconShield(): string {
  return baseIcon(`
      <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/>
      <path d="m9 12 2 2 4-4"/>
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
      <path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"/>
      <path d="M14 2v5a1 1 0 0 0 1 1h5"/>
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
  <path d="M4 5h16"/>
  <path d="M4 12h16"/>
  <path d="M4 19h16"/>
  `);
}

function iconDirectoryPlaceholder(): string {
  return baseIcon(`
  <path d="M16 5H3"/>
  <path d="M16 12H3"/>
  <path d="M16 19H3"/>
  <path d="M21 5h.01"/>
  <path d="M21 12h.01"/>
  <path d="M21 19h.01"/>
  `);
}

function iconMenuPlaceholder(): string {
  return baseIcon(`
  <path d="M4 5h16"/>
  <path d="M4 12h16"/>
  <path d="M4 19h16"/>
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
