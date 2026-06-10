// /src/registry/configs.registry.ts
import type { ConfigActionContext } from "../types/command";

export interface ConfigActionDef {
  code: string; // 对应资源 code，例如 md
  name: string;
  action: (ctx: ConfigActionContext) => void;
}

export const CONFIG_ACTIONS: ConfigActionDef[] = [
  {
    code: "md",
    name: "切换搜索模式 (指令/名称)",
    action: (ctx) => ctx.toggleSearchMode(),
  },
  {
    code: "abt",
    name: "关于工具箱",
    action: (ctx) => ctx.showAbout(),
  },
];

export function getConfigAction(code: string): ConfigActionDef | undefined {
  const trimmed = code.trim();
  const normalized = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  return CONFIG_ACTIONS.find((s) => s.code === trimmed || s.code === normalized);
}
