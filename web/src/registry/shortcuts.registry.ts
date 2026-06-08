// /src/registry/shortcuts.registry.ts
import { ref } from "vue";

export interface ShortcutItem {
  keys: string;
  action: string;
}

const pageShortcuts = ref<ShortcutItem[]>([]);

export function setPageShortcuts(shortcuts: ShortcutItem[]): void {
  pageShortcuts.value = Array.isArray(shortcuts) ? shortcuts : [];
}

export function clearPageShortcuts(): void {
  pageShortcuts.value = [];
}

export function usePageShortcuts() {
  return pageShortcuts;
}
