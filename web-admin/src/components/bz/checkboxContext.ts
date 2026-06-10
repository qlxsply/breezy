import type { InjectionKey } from "vue";

export interface BzCheckboxGroupContext {
  isChecked: (value: unknown) => boolean;
  toggle: (value: unknown, checked: boolean) => void;
  disabled: boolean;
}

export const bzCheckboxGroupContextKey: InjectionKey<BzCheckboxGroupContext> =
  Symbol("bzCheckboxGroup");
