import {
  changeAdminPassword,
  getCurrentAdmin,
  loginAdmin,
  logoutAdmin,
} from "@admin/features/auth/api/auth-client";
import {
  getAuthSessionSnapshot,
  setAuthAnonymous,
  setAuthenticated,
  setAuthError,
  setAuthLoading,
  updateAuthUser,
} from "@admin/features/auth/model/auth-store";
import type { AuthSession, AuthUser, AuthUserType } from "@admin/features/auth/model/types";
import { clearFormatterConfigs, setFormatterConfigs } from "@admin/shared/lib/formatter";
import type { UserConfigItem } from "@admin/shared/types/user-config";

export const INTERNAL_USER_LANDING_PATH = "/admin";

let loadPromise: Promise<AuthSession> | null = null;
let authGeneration = 0;

export function loadAuthSession(
  options: { force?: boolean; signal?: AbortSignal } = {},
): Promise<AuthSession> {
  const current = getAuthSessionSnapshot();
  if (!options.force && (current.status === "authenticated" || current.status === "anonymous")) {
    return Promise.resolve(current);
  }
  if (!options.force && loadPromise) return loadPromise;

  const generation = ++authGeneration;
  setAuthLoading();
  loadPromise = getCurrentAdmin({ signal: options.signal })
    .then((user) => {
      if (generation !== authGeneration) return getAuthSessionSnapshot();
      if (!user?.id) {
        clearFormatterConfigs();
        setAuthAnonymous();
      } else {
        setFormatterConfigs(user.configs ?? []);
        setAuthenticated(user);
      }
      return getAuthSessionSnapshot();
    })
    .catch((error: unknown) => {
      if (generation !== authGeneration) return getAuthSessionSnapshot();
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      setAuthError(error instanceof Error ? error.message : "后台会话恢复失败");
      throw error;
    })
    .finally(() => {
      if (generation === authGeneration) loadPromise = null;
    });
  return loadPromise;
}

export async function login(account: string, password: string): Promise<AuthUser> {
  const generation = ++authGeneration;
  const user = await loginAdmin(account, password);
  if (generation !== authGeneration) throw new DOMException("登录流程已取消", "AbortError");
  setFormatterConfigs(user.configs ?? []);
  setAuthenticated(user);
  return user;
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<boolean> {
  const changed = await changeAdminPassword(oldPassword, newPassword);
  const current = getAuthSessionSnapshot().user;
  if (changed && current?.mustChangePassword) {
    updateAuthUser({ ...current, mustChangePassword: false });
  }
  return changed;
}

export function revokeAuthSession(): Promise<boolean> {
  return logoutAdmin();
}

export function applyPersonalizedConfigs(configs: UserConfigItem[]): void {
  const current = getAuthSessionSnapshot().user;
  if (!current) return;
  const user = { ...current, configs };
  setFormatterConfigs(configs);
  updateAuthUser(user);
}

export function resetAuthSession(): void {
  authGeneration += 1;
  loadPromise = null;
  clearFormatterConfigs();
  setAuthAnonymous();
}

export function resolveLandingPathForUser(userType: AuthUserType): string {
  return userType === "ADMIN" ? INTERNAL_USER_LANDING_PATH : "/";
}
