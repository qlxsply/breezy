import type { InjectionKey } from "vue";

export interface BzDropdownContext {
  close: () => void;
}

export const bzDropdownContextKey: InjectionKey<BzDropdownContext> = Symbol("bzDropdownContext");
