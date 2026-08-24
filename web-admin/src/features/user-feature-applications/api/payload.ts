import type { PageRule, SortRule } from "@admin/shared/types/pagination";

export interface UserFeatureItemPayload {
  id?: string | number | null;
  applicationId?: string | number | null;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  enabled?: boolean | null;
  permissionCodes?: string[] | null;
}

export interface UserFeatureApplicationPayload {
  id?: string | number | null;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  icon?: string | null;
  routePath?: string | null;
  componentPath?: string | null;
  enabled?: boolean | null;
  featureCount?: number | null;
  permissionBindingCount?: number | null;
  features?: UserFeatureItemPayload[] | null;
}

export interface UserFeatureApplicationPageRequest {
  keyword?: string;
  enabled?: boolean;
  page?: PageRule;
  sort?: SortRule;
}
