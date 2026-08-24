import type { PageRule, SortRule } from "@admin/shared/types/pagination";

export interface LoginLogPageRequest {
  userAccount?: string;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}
