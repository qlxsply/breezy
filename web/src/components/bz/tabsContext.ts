import type { InjectionKey, Ref } from "vue";

export interface BzTabPaneItem {
  name: string;
  label: string;
}

export interface BzTabsContext {
  activeName: Ref<string>;
  registerPane: (pane: BzTabPaneItem) => void;
  unregisterPane: (name: string) => void;
}

export const bzTabsContextKey: InjectionKey<BzTabsContext> = Symbol("bzTabsContext");
