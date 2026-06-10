// /src/utils/authStorage.ts

const TOKEN_KEY = "breezy:auth:token";
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
  window.localStorage.removeItem(SCOPE_KEY);
}
