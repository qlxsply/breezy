export interface UserFeatureItemEntry {
  id: string;
  applicationId: string;
  code: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  permissionCodes: string[];
}

export interface UserFeatureApplicationEntry {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  icon?: string | null;
  routePath?: string | null;
  componentPath?: string | null;
  enabled: boolean;
  featureCount: number;
  permissionBindingCount: number;
  features: UserFeatureItemEntry[];
}

export type UserFeatureAccessScope = "FULL" | "PARTIAL";
export type UserFeatureOverrideType = "NONE" | "ENABLE" | "DISABLE";

export interface UserFeaturePackageOptionEntry {
  id: string;
  code: string;
  name: string;
  packageType: string;
  description?: string | null;
  enabled: boolean;
  defaultPackage: boolean;
}

export interface UserFeaturePackageApplicationAccessEntry {
  applicationId: string;
  applicationCode: string;
  applicationName: string;
  featureAccessScope: UserFeatureAccessScope;
  featureIds: string[];
  features: UserFeatureItemEntry[];
}

export interface UserFeaturePackageEntry {
  id: string;
  code: string;
  name: string;
  packageType: string;
  description?: string | null;
  enabled: boolean;
  defaultPackage: boolean;
  applicationAccesses: UserFeaturePackageApplicationAccessEntry[];
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

export interface UserFeatureUserFeatureEntry {
  id: string;
  applicationId: string;
  applicationCode: string;
  code: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  permissionCodes: string[];
  inheritedEnabled: boolean;
  effectiveEnabled: boolean;
  overrideType: UserFeatureOverrideType;
}

export interface UserFeatureUserApplicationEntry {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  icon?: string | null;
  routePath?: string | null;
  componentPath?: string | null;
  enabled: boolean;
  inheritedVisible: boolean;
  effectiveVisible: boolean;
  packageAccessScope: "NONE" | UserFeatureAccessScope;
  overrideType: UserFeatureOverrideType;
  overrideAccessScope?: UserFeatureAccessScope | null;
  features: UserFeatureUserFeatureEntry[];
}

export interface UserFeatureUserManagementEntry {
  userId: string;
  account: string;
  packageIds: string[];
  packages: UserFeaturePackageOptionEntry[];
  applications: UserFeatureUserApplicationEntry[];
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
