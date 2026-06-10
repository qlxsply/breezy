export type PermissionUserScope = "INTERNAL" | "EXTERNAL" | "COMMON";

export interface PermissionEntry {
  id: string;
  code: string;
  name: string;
  userScope: PermissionUserScope;
  description?: string;
  enabled: boolean;
}
