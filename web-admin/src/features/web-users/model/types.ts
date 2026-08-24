export type ExternalUserStatus = "ACTIVE" | "DISABLED" | "CANCELLED";

export interface ExternalUserEntry {
  id: string;
  account: string;
  username: string;
  nickname: string;
  userType: "USER";
  status: ExternalUserStatus;
  lastLoginAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
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
