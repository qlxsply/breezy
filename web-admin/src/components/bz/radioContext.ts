import type { InjectionKey } from "vue";

export interface BzRadioGroupContext {
  isChecked: (value: unknown) => boolean;
  disabled: boolean;
  update: (value: unknown) => void;
  isButton: boolean;
}

export const bzRadioGroupContextKey: InjectionKey<BzRadioGroupContext> = Symbol("bzRadioGroup");
