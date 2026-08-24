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
