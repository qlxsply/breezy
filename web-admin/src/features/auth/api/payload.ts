import type { UserConfigItem } from "@admin/shared/types/user-config";

import type { AuthUserType } from "../model/types";

export interface AuthUserPayload {
  id?: string | number | null;
  account?: string | null;
  userType?: AuthUserType | null;
  mustChangePassword?: boolean | null;
  configs?: UserConfigItem[] | null;
}

export interface AdminLoginPayload {
  sessionExpiresAt?: string | null;
  user?: AuthUserPayload | null;
}
