import type { InjectionKey } from "vue";

export interface JsonTreeContext {
  isCollapsed: (path: string) => boolean;
  toggle: (path: string) => void;
  isKeyMatch: (path: string) => boolean;
  isValueMatch: (path: string) => boolean;
  parseValue: (path: string, value: unknown) => void;
  updateValue: (path: string, value: string) => void;
}

export const JsonTreeContextKey: InjectionKey<JsonTreeContext> = Symbol("JsonTreeContext");
