// /src/registry/auth.registry.ts
import { computed, ref } from "vue";

import type { AuthSpace, AuthUser, AuthUserType } from "../api/auth";
import { getMe, login as loginApi, logout as logoutApi } from "../api/auth";
import type { UserConfigItem } from "../api/configs";
import {
  clearAuthToken,
  getAuthScope,
  getAuthToken,
  setAuthScope,
  setAuthToken,
} from "../utils/authStorage";
import { ensureUnreadLoaded } from "./notifications.registry";
import { refreshPermissions } from "./permissions.registry";
import {
  ensureWebPushSubscription,
  initTodoReminderPermission,
  removeWebPushSubscription,
} from "./todo-reminder.registry";

const currentUser = ref<AuthUser | null>(null);
const personalizedConfigs = ref<UserConfigItem[]>([]);

const authState = {
  loaded: false,
  loading: false,
  promise: null as Promise<void> | null,
};

export const INTERNAL_USER_LANDING_PATH = "/";
export const isAuthenticated = computed(() => Boolean(currentUser.value?.id));
export const currentUserType = computed<AuthUserType>(() => currentUser.value?.userType || "GUEST");
export const isInternalUser = computed(() => currentUserType.value === "INTERNAL");
export const isExternalUser = computed(() => currentUserType.value === "EXTERNAL");
export const isNormalUser = isExternalUser;

export function useAuthUser() {
  return currentUser;
}

export function getCurrentUserType(): AuthUserType {
  return currentUser.value?.userType || "GUEST";
}

export function resolveLandingPathForUser(userType: AuthUserType): string {
  return userType === "INTERNAL" ? INTERNAL_USER_LANDING_PATH : "/";
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
      if (!token) {
        currentUser.value = null;
        personalizedConfigs.value = [];
        await ensureUnreadLoaded(true);
        return;
      }

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
      clearAuthToken();
      currentUser.value = null;
      await ensureUnreadLoaded(true);
    } finally {
      authState.loaded = true;
      authState.loading = false;
    }
  })();

  return authState.promise;
}

export async function login(
  scope: AuthSpace,
  username: string,
  password: string,
): Promise<AuthUser> {
  const resp = await loginApi(scope, username, password);
  if (!resp || !resp.token || !resp.user) {
    throw new Error("登录响应无效");
  }
  setAuthToken(resp.token);
  setAuthScope(scope);

  await refreshPermissions();

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
    await logoutApi(getAuthScope());
  } finally {
    clearAuthToken();
    authState.loaded = false;
    await ensureAuthLoaded(true);
  }
}
