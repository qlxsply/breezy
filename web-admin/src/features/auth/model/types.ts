import type { UserConfigItem } from "@admin/shared/types/user-config";

export type AuthUserType = "ADMIN" | "USER" | "GUEST";
export type AuthSessionStatus = "idle" | "loading" | "authenticated" | "anonymous" | "error";

export interface AuthUser {
  id: string | null;
  username: string | null;
  account?: string | null;
  userType: AuthUserType;
  configs?: UserConfigItem[];
}

export interface AuthSession {
  status: AuthSessionStatus;
  user: AuthUser | null;
  error: string | null;
}
