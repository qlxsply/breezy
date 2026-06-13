// /src/registry/auth.registry.ts
import { computed, ref } from "vue";

import type { AuthUser, AuthUserType } from "../api/auth";
import { getMe, login as loginApi, logout as logoutApi } from "../api/auth";
import type { UserConfigItem } from "../api/configs";
import { getAuthScope, getAuthToken, getRefreshToken } from "../utils/authStorage";
import {
  applyExternalAuthTokens,
  clearAuthSession,
  ensureValidAccessToken,
  handleAuthSessionExpired,
} from "./auth-token.registry";
import { ensureUnreadLoaded } from "./notifications.registry";
import {
  ensureWebPushSubscription,
  initTodoReminderPermission,
  removeWebPushSubscription,
} from "./todo-reminder.registry";
import { refreshUserToolPermissions } from "./user-tool-permissions.registry";

const currentUser = ref<AuthUser | null>(null);
const personalizedConfigs = ref<UserConfigItem[]>([]);

const authState = {
  loaded: false,
  loading: false,
  promise: null as Promise<void> | null,
};

export const isAuthenticated = computed(() => Boolean(currentUser.value?.id));
export const currentUserType = computed<AuthUserType>(() => currentUser.value?.userType || "GUEST");
export const isExternalUser = computed(() => currentUserType.value === "EXTERNAL");
export const isNormalUser = isExternalUser;

export function useAuthUser() {
  return currentUser;
}

export function resetAuthPresentationState(): void {
  currentUser.value = null;
  personalizedConfigs.value = [];
  authState.loaded = false;
}

export function getCurrentUserType(): AuthUserType {
  return currentUser.value?.userType || "GUEST";
}

export function resolveLandingPathForUser(_userType: AuthUserType): string {
  return "/";
}

export function usePersonalizedConfigs() {
  return personalizedConfigs;
}

export function applyPersonalizedConfigs(configs: UserConfigItem[]) {
  personalizedConfigs.value = configs;
  if (currentUser.value) {
    currentUser.value = { ...currentUser.value, configs };
  }
}

export async function ensureAuthLoaded(force = false): Promise<void> {
  if (!force && authState.loaded) return authState.promise ?? Promise.resolve();
  if (!force && authState.loading) return authState.promise ?? Promise.resolve();

  authState.loading = true;
  authState.promise = (async () => {
    try {
      const token = getAuthToken();
      const refreshToken = getRefreshToken();
      if (!token && !refreshToken) {
        currentUser.value = null;
        personalizedConfigs.value = [];
        await ensureUnreadLoaded(true);
        return;
      }

      await ensureValidAccessToken();
      const me = await getMe(getAuthScope());
      if (me) {
        if (me.id) {
          currentUser.value = me;
          if (me.userType !== "INTERNAL") {
            await ensureUnreadLoaded(true);
            initTodoReminderPermission();
            await ensureWebPushSubscription();
          }
        } else {
          currentUser.value = null;
          await ensureUnreadLoaded(true);
        }
        if (me.configs) {
          personalizedConfigs.value = me.configs;
        }
      } else {
        currentUser.value = null;
        personalizedConfigs.value = [];
        await ensureUnreadLoaded(true);
      }
    } catch (err) {
      console.warn("[auth] load failed", err);
      handleAuthSessionExpired("登录已过期，请重新登录");
      await ensureUnreadLoaded(true);
    } finally {
      authState.loaded = true;
      authState.loading = false;
    }
  })();

  return authState.promise;
}

export async function login(username: string, password: string): Promise<AuthUser> {
  const resp = await loginApi("external", username, password);
  if (!resp || !resp.token || !resp.user) {
    throw new Error("登录响应无效");
  }
  applyExternalAuthTokens({
    accessToken: resp.token,
    refreshToken: resp.refreshToken,
    accessTokenExpiresAt: resp.accessTokenExpiresAt,
    refreshTokenExpiresAt: resp.refreshTokenExpiresAt,
  });

  await refreshUserToolPermissions();

  currentUser.value = resp.user;
  if (resp.user.configs) {
    personalizedConfigs.value = resp.user.configs;
  }
  authState.loaded = true;
  if (resp.user.userType !== "INTERNAL") {
    await ensureUnreadLoaded(true);
    initTodoReminderPermission();
    await ensureWebPushSubscription();
  }
  return resp.user;
}

export async function logout(): Promise<void> {
  try {
    await removeWebPushSubscription();
    await logoutApi("external");
  } finally {
    clearAuthSession();
    authState.loaded = false;
    await ensureAuthLoaded(true);
  }
}
