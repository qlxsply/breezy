import type { PageRule, SortRule } from "@admin/shared/types/pagination";

import type { UserFeatureAccessScope } from "../model/types";

export interface UserFeaturePackageAccessPayload {
  applicationId?: string | number | null;
  applicationCode?: string | null;
  applicationName?: string | null;
  featureAccessScope?: UserFeatureAccessScope | null;
  featureIds?: Array<string | number> | null;
  features?: UserFeaturePackageItemPayload[] | null;
}

export interface UserFeaturePackagePayload {
  id?: string | number | null;
  code?: string | null;
  name?: string | null;
  packageType?: string | null;
  description?: string | null;
  enabled?: boolean | null;
  defaultPackage?: boolean | null;
  applicationAccesses?: UserFeaturePackageAccessPayload[] | null;
}

export interface UserFeaturePackagePageRequest {
  keyword?: string;
  enabled?: boolean;
  page?: PageRule;
  sort?: SortRule;
}

export interface SaveUserFeaturePackageRequest {
  code: string;
  name: string;
  packageType: string;
  description?: string | null;
  enabled: boolean;
  defaultPackage: boolean;
  applicationAccesses: Array<{
    applicationId: string;
    featureAccessScope: UserFeatureAccessScope;
    featureIds: string[];
  }>;
}
export interface UserFeaturePackageItemPayload {
  id?: string | number | null;
  applicationId?: string | number | null;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  enabled?: boolean | null;
  permissionCodes?: string[] | null;
}
