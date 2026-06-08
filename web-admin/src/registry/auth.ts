import type { AdminAuthUser } from "@admin/api/auth";
import { getMe, login as loginApi, logout as logoutApi } from "@admin/api/auth";
import {
  clearAuthToken,
  clearSharedAuth,
  getAuthToken,
  setAuthToken,
  syncSharedAuthScope,
  syncSharedAuthToken,
} from "@admin/utils/authStorage";
import { computed, ref } from "vue";

const currentUser = ref<AdminAuthUser | null>(null);

const authState = {
  loaded: false,
  loading: false,
  promise: null as Promise<void> | null,
};

export const isAuthenticated = computed(() => Boolean(currentUser.value?.id));

export function useAuthUser() {
  return currentUser;
}

export async function ensureAuthLoaded(force = false): Promise<void> {
  if (!force && authState.loaded) {
    return authState.promise ?? Promise.resolve();
  }
  if (!force && authState.loading) {
    return authState.promise ?? Promise.resolve();
  }
  authState.loading = true;
  authState.promise = (async () => {
    try {
      const token = getAuthToken();
      if (!token) {
        currentUser.value = null;
        return;
      }
      const me = await getMe();
      currentUser.value = me && me.userType === "INTERNAL" ? me : null;
      if (!currentUser.value) {
        clearAuthToken();
      }
    } catch {
      clearAuthToken();
      currentUser.value = null;
    } finally {
      authState.loaded = true;
      authState.loading = false;
    }
  })();
  return authState.promise;
}

export async function login(account: string, password: string): Promise<AdminAuthUser> {
  const response = await loginApi(account, password);
  if (response.user.userType !== "INTERNAL") {
    throw new Error("当前账号不是后台账号");
  }
  setAuthToken(response.token);
  syncSharedAuthToken(response.token);
  syncSharedAuthScope("internal");
  currentUser.value = response.user;
  authState.loaded = true;
  return response.user;
}

export async function logout(): Promise<void> {
  try {
    await logoutApi();
  } finally {
    clearAuthToken();
    clearSharedAuth();
    currentUser.value = null;
    authState.loaded = false;
  }
}
