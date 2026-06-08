export interface NormalFeatureGroupFeatureEntry {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  enabled: boolean;
}

export interface NormalFeatureGroupEntry {
  id: string;
  code: string;
  name: string;
  groupType: string;
  description?: string | null;
  enabled: boolean;
  defaultGroup: boolean;
  featureIds: string[];
  features: NormalFeatureGroupFeatureEntry[];
}

export interface SaveNormalFeatureGroupRequest {
  code: string;
  name: string;
  groupType: string;
  description?: string | null;
  enabled: boolean;
  defaultGroup: boolean;
  featureIds: string[];
}
