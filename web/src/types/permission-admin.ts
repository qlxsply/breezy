export type PermissionUserScope = "INTERNAL" | "EXTERNAL";

export interface PermissionEntry {
  id: string;
  code: string;
  name: string;
  userScope: PermissionUserScope;
}
