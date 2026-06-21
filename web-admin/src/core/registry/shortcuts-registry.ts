"use client";

import { createStore, useStoreValue } from "@admin/core/client-store";

export interface ShortcutItem {
  keys: string;
  action: string;
}

const shortcutsStore = createStore<ShortcutItem[]>([]);

export function setPageShortcuts(shortcuts: ShortcutItem[]): void {
  shortcutsStore.setState(Array.isArray(shortcuts) ? shortcuts : []);
}

export function clearPageShortcuts(): void {
  shortcutsStore.setState([]);
}

export function getPageShortcuts(): ShortcutItem[] {
  return shortcutsStore.getState();
}

export function usePageShortcuts(): ShortcutItem[] {
  return useStoreValue(shortcutsStore, (state) => state);
}
