import type { InjectionKey } from "vue";

export interface BzMenuContext {
  activeKey: string;
  select: (key: string) => void;
}

export const bzMenuContextKey: InjectionKey<BzMenuContext> = Symbol("bzMenuContext");
