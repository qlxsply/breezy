// /src/types/command.ts

import type { ResourceEntry } from "./resource-admin";

export type SearchModeType = "setting" | "info" | "tool";

export interface SearchBadge {
  label: string;
  type: SearchModeType;
  className: string;
}

export type MenuResource = ResourceEntry & {
  type: "MENU";
};

export interface ConfigActionContext {
  toggleSearchMode: () => void;
  showAbout: () => void;
}

/**
 * 结果项：统一展示模型（附带 score 便于排序）
 * - score 由搜索算法计算（完全匹配/前缀/模糊等）
 */
export type ResultItem = { kind: string; resource: MenuResource; score: number };

/**
 * 分组展示：例如 “最近使用 / 全部工具 / 设置”
 * - HomePage 负责把分组渲染成多个 panel
 */
export interface ResultGroup {
  title: string;
  items: ResultItem[];
}
