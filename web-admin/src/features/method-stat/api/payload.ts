import type { PageQuery } from "@admin/shared/types/pagination";

export interface MethodStatStatsPageRequest extends PageQuery {
  methodName?: string;
  collectEnabled?: boolean;
}
