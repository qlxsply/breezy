import { API_BASE_URL } from "@admin/core/env";
import type { ResourceNodeType } from "@admin/types/resource-admin";

const defaultDirectoryIconUrl = svgToDataUrl(iconDirectoryPlaceholder());
const defaultMenuIconUrl = svgToDataUrl(iconMenuPlaceholder());

export function resolveResourceIconUrl(iconCode?: string | null, nodeType?: ResourceNodeType | null): string | null {
  const normalized = normalizeIconCode(iconCode);
  if (normalized) {
    return `${API_BASE_URL}/public/static-files/code/${encodeURIComponent(normalized)}`;
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

function svgToDataUrl(svg: string): string {
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
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
