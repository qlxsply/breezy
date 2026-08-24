import type { UserFeatureItemEntry } from "@admin/features/user-feature-applications/public/catalog";

export type UserFeatureAccessScope = "FULL" | "PARTIAL";

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

export interface UserFeaturePackageOptionEntry {
  id: string;
  code: string;
  name: string;
  packageType: string;
  description?: string | null;
  enabled: boolean;
  defaultPackage: boolean;
}
