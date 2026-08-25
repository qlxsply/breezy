import type { PageRule } from "@admin/shared/types/pagination";

import type {
  UserFeatureAccessScope,
  UserFeatureOverrideType,
  WebUserStatus,
} from "../model/types";

export interface WebUserPayload {
  id?: string | number | null;
  account?: string | null;
  nickname?: string | null;
  status?: WebUserStatus | null;
  lastLoginAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface WebUserPageRequest {
  keyword?: string;
  status?: WebUserStatus;
  page?: PageRule;
}

export interface UserFeatureUserFeaturePayload {
  id?: string | number | null;
  applicationId?: string | number | null;
  applicationCode?: string | null;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  enabled?: boolean | null;
  permissionCodes?: string[] | null;
  inheritedEnabled?: boolean | null;
  effectiveEnabled?: boolean | null;
  overrideType?: UserFeatureOverrideType | null;
}

export interface UserFeatureUserApplicationPayload {
  id?: string | number | null;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  icon?: string | null;
  routePath?: string | null;
  componentPath?: string | null;
  enabled?: boolean | null;
  inheritedVisible?: boolean | null;
  effectiveVisible?: boolean | null;
  packageAccessScope?: "NONE" | UserFeatureAccessScope | null;
  overrideType?: UserFeatureOverrideType | null;
  overrideAccessScope?: UserFeatureAccessScope | null;
  features?: UserFeatureUserFeaturePayload[] | null;
}

export interface UserFeaturePackageOptionPayload {
  id?: string | number | null;
  code?: string | null;
  name?: string | null;
  packageType?: string | null;
  description?: string | null;
  enabled?: boolean | null;
  defaultPackage?: boolean | null;
}

export interface UserFeatureUserManagementPayload {
  userId?: string | number | null;
  account?: string | null;
  packageIds?: Array<string | number> | null;
  packages?: UserFeaturePackageOptionPayload[] | null;
  applications?: UserFeatureUserApplicationPayload[] | null;
}

export interface SaveUserFeatureUserManagementRequest {
  packageIds: string[];
  applicationOverrides: Array<{
    applicationId: string;
    overrideType: UserFeatureOverrideType;
    featureAccessScope?: UserFeatureAccessScope;
  }>;
  featureOverrides: Array<{
    applicationId: string;
    featureId: string;
    overrideType: UserFeatureOverrideType;
  }>;
}
