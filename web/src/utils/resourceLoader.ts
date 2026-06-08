// /src/utils/resourceLoader.ts
import { type Component, defineAsyncComponent } from "vue";

const pageComponentMap = import.meta.glob("../pages/**/*.vue");

const modalComponentMap: Record<string, () => Promise<{ default: Component }>> = {
  "../components/MyInfoBox.vue": () => import("../components/MyInfoBox.vue"),
};

function resolveLoader(target: string): (() => Promise<{ default: Component }>) | undefined {
  const normalized = target.replace(/^\/+/, "").replace(/^\.\//, "");
  const key = `../${normalized}`;
  if (key.startsWith("../pages/")) {
    return pageComponentMap[key] as (() => Promise<{ default: Component }>) | undefined;
  }
  if (key.startsWith("../components/")) {
    return modalComponentMap[key];
  }
  return undefined;
}

export function resolveRouteComponent(
  target?: string,
): (() => Promise<{ default: Component }>) | undefined {
  if (!target) return undefined;
  const loader = resolveLoader(target);
  if (!loader) return undefined;
  const normalized = target.replace(/^\/+/, "").replace(/^\.\//, "");
  return normalized.startsWith("pages/") ? loader : undefined;
}

export function resolveResourceComponent(target?: string): Component | undefined {
  if (!target) return undefined;
  const loader = resolveLoader(target);
  if (!loader) return undefined;
  return defineAsyncComponent(loader);
}
