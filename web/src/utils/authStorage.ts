// /src/utils/authStorage.ts

const TOKEN_KEY = "breezy:auth:token";
const REFRESH_TOKEN_KEY = "breezy:auth:refresh-token";
const ACCESS_TOKEN_EXPIRES_AT_KEY = "breezy:auth:access-token-expires-at";
const REFRESH_TOKEN_EXPIRES_AT_KEY = "breezy:auth:refresh-token-expires-at";
const SCOPE_KEY = "breezy:auth:scope";

export type AuthScope = "internal" | "external";

export function getAuthToken(): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(TOKEN_KEY) || "";
}

export function setAuthToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function getRefreshToken(): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(REFRESH_TOKEN_KEY) || "";
}

export function setRefreshToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

export function getAccessTokenExpiresAt(): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY) || "";
}

export function setAccessTokenExpiresAt(expiresAt: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(ACCESS_TOKEN_EXPIRES_AT_KEY, expiresAt);
}

export function getRefreshTokenExpiresAt(): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(REFRESH_TOKEN_EXPIRES_AT_KEY) || "";
}

export function setRefreshTokenExpiresAt(expiresAt: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(REFRESH_TOKEN_EXPIRES_AT_KEY, expiresAt);
}

export function getAuthScope(): AuthScope {
  if (typeof window === "undefined") return "external";
  const value = window.localStorage.getItem(SCOPE_KEY);
  return value === "internal" ? "internal" : "external";
}

export function setAuthScope(scope: AuthScope): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(SCOPE_KEY, scope);
}

export function clearAuthToken(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_EXPIRES_AT_KEY);
  window.localStorage.removeItem(SCOPE_KEY);
}
