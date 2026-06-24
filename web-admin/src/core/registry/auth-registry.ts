"use client";

import {
  clearAuthToken,
  getAuthScope,
  getAuthToken,
  setAuthScope,
  setAuthToken,
} from "@admin/core/auth-storage";
import { createStore, useStoreValue } from "@admin/core/client-store";
import { clearFormatterConfigs, setFormatterConfigs } from "@admin/core/formatter";
import { get, post, put } from "@admin/core/http";
import { refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { ensureUnreadLoaded } from "@admin/core/registry/notifications-registry";
import {
  ensureWebPushSubscription,
  initTodoReminderPermission,
  removeWebPushSubscription,
} from "@admin/core/registry/todo-reminder-registry";
import type { UserConfigItem } from "@admin/core/types";

export type AuthUserType = "INTERNAL" | "EXTERNAL" | "GUEST";
export type AuthSpace = "internal" | "external";

export interface AuthUser {
  id: string | null;
  username: string | null;
  account?: string | null;
  userType: AuthUserType;
  configs?: UserConfigItem[];
}

interface AuthUserPayload {
  id: string | null;
  account: string | null;
  userType?: AuthUserType | null;
  configs?: UserConfigItem[];
}

interface LoginResponsePayload {
  token: string;
  user: AuthUserPayload;
}

interface AuthRegistryState {
  currentUser: AuthUser | null;
  personalizedConfigs: UserConfigItem[];
  loaded: boolean;
  loading: boolean;
}

const authStore = createStore<AuthRegistryState>({
  currentUser: null,
  personalizedConfigs: [],
  loaded: false,
  loading: false,
});

let authPromise: Promise<void> | null = null;

export const INTERNAL_USER_LANDING_PATH = "/admin";

export function useAuthUser(): AuthUser | null {
  return useStoreValue(authStore, (state) => state.currentUser);
}

export function usePersonalizedConfigs(): UserConfigItem[] {
  return useStoreValue(authStore, (state) => state.personalizedConfigs);
}

export function useIsAuthenticated(): boolean {
  return useStoreValue(authStore, (state) => Boolean(state.currentUser?.id));
}

export function useAuthLoaded(): boolean {
  return useStoreValue(authStore, (state) => state.loaded);
}

export function useAuthLoading(): boolean {
  return useStoreValue(authStore, (state) => state.loading);
}

export function useCurrentUserType(): AuthUserType {
  return useStoreValue(authStore, (state) => state.currentUser?.userType || "GUEST");
}

export function isAuthenticated(): boolean {
  return Boolean(authStore.getState().currentUser?.id);
}

export function isAuthLoaded(): boolean {
  return authStore.getState().loaded;
}

export function getCurrentUser(): AuthUser | null {
  return authStore.getState().currentUser;
}

export function getCurrentUserType(): AuthUserType {
  return authStore.getState().currentUser?.userType || "GUEST";
}

export function resolveLandingPathForUser(userType: AuthUserType): string {
  return userType === "INTERNAL" ? INTERNAL_USER_LANDING_PATH : "/";
}

export function applyPersonalizedConfigs(configs: UserConfigItem[]) {
  authStore.setState((state) => ({
    ...state,
    personalizedConfigs: configs,
    currentUser: state.currentUser ? { ...state.currentUser, configs } : state.currentUser,
  }));
  setFormatterConfigs(configs);
}

export async function ensureAuthLoaded(force = false): Promise<void> {
  const state = authStore.getState();
  if (!force && state.loaded) return authPromise ?? Promise.resolve();
  if (!force && state.loading && authPromise) return authPromise;

  authStore.setState((current) => ({ ...current, loading: true }));
  authPromise = (async () => {
    try {
      const token = getAuthToken();
      if (!token) {
        setLoggedOutState();
        await ensureUnreadLoaded(true);
        return;
      }

      const me = await getMe(getAuthScope());
      if (me?.id) {
        authStore.setState({
          currentUser: me,
          personalizedConfigs: me.configs ?? [],
          loaded: true,
          loading: false,
        });
        setFormatterConfigs(me.configs ?? []);
        await ensureUnreadLoaded(true);
        initTodoReminderPermission();
        await ensureWebPushSubscription();
        return;
      }

      setLoggedOutState();
      await ensureUnreadLoaded(true);
    } catch (error) {
      console.warn("[auth] load failed", error);
      clearAuthToken();
      setLoggedOutState();
      await ensureUnreadLoaded(true);
    } finally {
      authStore.setState((current) => ({ ...current, loaded: true, loading: false }));
    }
  })();

  return authPromise;
}

export async function login(
  scope: AuthSpace,
  username: string,
  password: string,
): Promise<AuthUser> {
  const payload = await post<LoginResponsePayload>(`${authBase(scope)}/login`, {
    account: username,
    password,
  });
  if (!payload?.token || !payload.user) {
    throw new Error("登录响应无效");
  }

  const user = toAuthUser(payload.user);
  setAuthToken(payload.token);
  setAuthScope(scope);
  await refreshRegistryLoaded();

  authStore.setState({
    currentUser: user,
    personalizedConfigs: user.configs ?? [],
    loaded: true,
    loading: false,
  });
  setFormatterConfigs(user.configs ?? []);
  await ensureUnreadLoaded(true);
  initTodoReminderPermission();
  await ensureWebPushSubscription();
  return user;
}

export async function logout(): Promise<void> {
  try {
    await removeWebPushSubscription();
    await post<boolean>(`${authBase(getAuthScope())}/logout`, {});
  } finally {
    clearAuthToken();
    setLoggedOutState();
    await ensureAuthLoaded(true);
  }
}

export async function changePassword(
  space: AuthSpace,
  oldPassword: string,
  newPassword: string,
): Promise<boolean> {
  return put<boolean>(`${authBase(space)}/password`, { oldPassword, newPassword });
}

async function getMe(space: AuthSpace): Promise<AuthUser | null> {
  const payload = await get<AuthUserPayload | null>(`${authBase(space)}/me`);
  return payload ? toAuthUser(payload) : null;
}

function authBase(space: AuthSpace): string {
  return space === "internal" ? "/admin/auth" : "/auth";
}

function toAuthUser(payload: AuthUserPayload): AuthUser {
  const account = payload.account || "";
  return {
    id: payload.id,
    username: account,
    account,
    userType: payload.userType || "GUEST",
    configs: payload.configs,
  };
}

function setLoggedOutState() {
  authStore.setState({ currentUser: null, personalizedConfigs: [], loaded: false, loading: false });
  clearFormatterConfigs();
}
