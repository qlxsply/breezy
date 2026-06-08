export interface NormalFeatureEntry {
  id: string;
  code: string;
  name: string;
  description?: string;
  enabled: boolean;
  permissionCodes: string[];
}

export interface NormalFeatureSelection {
  featureIds: string[];
  resourceIds?: string[];
}

export type NormalFeatureOverrideType = "NONE" | "ENABLE" | "DISABLE";

export interface NormalFeatureUserFeatureEntry extends NormalFeatureEntry {
  groupEnabled: boolean;
  effectiveEnabled: boolean;
  overrideType: NormalFeatureOverrideType;
}

export interface NormalFeatureUserManagementEntry {
  userId: string;
  account: string;
  groupIds: string[];
  features: NormalFeatureUserFeatureEntry[];
}

export interface SaveNormalFeatureUserManagementRequest {
  groupIds: string[];
  overrides: Array<{
    featureId: string;
    overrideType: NormalFeatureOverrideType;
  }>;
}
