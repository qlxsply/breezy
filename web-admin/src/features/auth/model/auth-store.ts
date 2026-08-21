"use client";

import { useStoreValue } from "@admin/shared/hooks/useStoreValue";
import { createStore } from "@admin/shared/lib/store";
import type { UserConfigItem } from "@admin/shared/types/user-config";

import type { AuthSession, AuthSessionStatus, AuthUser, AuthUserType } from "./types";

const INITIAL_SESSION: AuthSession = {
  status: "idle",
  user: null,
  error: null,
};

const authStore = createStore<AuthSession>(INITIAL_SESSION);
const EMPTY_CONFIGS: UserConfigItem[] = [];

export function getAuthSessionSnapshot(): AuthSession {
  return authStore.getState();
}

export function subscribeAuthSession(listener: () => void): () => void {
  return authStore.subscribe(listener);
}

export function setAuthLoading(): void {
  authStore.setState((state) => ({ ...state, status: "loading", error: null }));
}

export function setAuthenticated(user: AuthUser): void {
  authStore.setState({ status: "authenticated", user, error: null });
}

export function setAuthAnonymous(): void {
  authStore.setState({ status: "anonymous", user: null, error: null });
}

export function setAuthError(error: string): void {
  authStore.setState({ status: "error", user: null, error });
}

export function updateAuthUser(user: AuthUser): void {
  authStore.setState({ status: "authenticated", user, error: null });
}

export function useAuthSession(): AuthSession {
  return useStoreValue(authStore, (state) => state);
}

export function useAuthStatus(): AuthSessionStatus {
  return useStoreValue(authStore, (state) => state.status);
}

export function useAuthUser(): AuthUser | null {
  return useStoreValue(authStore, (state) => state.user);
}

export function usePersonalizedConfigs(): UserConfigItem[] {
  return useStoreValue(authStore, (state) => state.user?.configs ?? EMPTY_CONFIGS);
}

export function useIsAuthenticated(): boolean {
  return useStoreValue(authStore, (state) => state.status === "authenticated");
}

export function useCurrentUserType(): AuthUserType {
  return useStoreValue(authStore, (state) => state.user?.userType ?? "GUEST");
}
