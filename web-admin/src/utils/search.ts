// /src/utils/search.ts

/**
 * 轻量搜索打分：
 * - 不引入第三方库
 * - 规则足够可控、可解释、易调参
 */

export interface Scored<T> {
  item: T;
  score: number;
}

/**
 * 子序列匹配（fuzzy）：
 * - pattern="jsf" 可以匹配 "jsonformat"（j-s-f 按顺序出现即可）
 * - 返回一个“紧凑度分数”，越紧凑越好
 * - 若不匹配返回 0
 */
export function fuzzySubsequenceScore(textRaw: string, patternRaw: string): number {
  const text = textRaw.toLowerCase();
  const pattern = patternRaw.toLowerCase();
  if (!pattern) return 0;

  let ti = 0;
  let pi = 0;

  // 记录匹配的首尾位置，用于评估“跨度”
  let first = -1;
  let last = -1;

  while (ti < text.length && pi < pattern.length) {
    if (text[ti] === pattern[pi]) {
      if (first < 0) first = ti;
      last = ti;
      pi++;
    }
    ti++;
  }

  if (pi !== pattern.length) return 0;

  // 完全匹配到：计算跨度越小分越高
  const span = last - first + 1; // 覆盖的长度
  const tightness = Math.max(1, pattern.length * 2 - (span - pattern.length)); // 简单紧凑度
  // 给一个上限，避免分数过大
  return Math.min(200, 80 + tightness);
}

export interface ScoreOptions {
  /**
   * 是否启用 fuzzy（子序列匹配）
   * - 建议默认开启，但你也可以在设置里加开关
   */
  enableFuzzy: boolean;
}

export const DEFAULT_SCORE_OPTIONS: ScoreOptions = {
  enableFuzzy: true,
};

export function scoreText(textRaw: string, queryRaw: string, opt: ScoreOptions): number {
  const text = textRaw.toLowerCase();
  const query = queryRaw.toLowerCase();

  if (!query) return 0;

  if (text === query) return 1000;
  if (text.startsWith(query)) return 600;
  if (text.includes(query)) return 300;

  if (opt.enableFuzzy) {
    const fuzzy = fuzzySubsequenceScore(text, query);
    if (fuzzy > 0) return fuzzy;
  }

  return 0;
}

/**
 * 通用排序：score desc -> weight desc -> name asc
 */
export function sortByScoreThenWeight<T extends { weight?: number; name?: string }>(
  a: { score: number; item: T },
  b: { score: number; item: T },
) {
  if (b.score !== a.score) return b.score - a.score;

  const aw = a.item.weight ?? 0;
  const bw = b.item.weight ?? 0;
  if (bw !== aw) return bw - aw;

  const an = (a.item.name ?? "").toLowerCase();
  const bn = (b.item.name ?? "").toLowerCase();
  return an.localeCompare(bn);
}
