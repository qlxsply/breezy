export type PermissionUserScope = "ADMIN" | "USER";

export interface PermissionEntry {
  id: string;
  code: string;
  name: string;
  userScope: PermissionUserScope;
}
