const TOKEN_KEY = "breezy:admin:auth:token";
const SHARED_TOKEN_KEY = "breezy:auth:token";
const SHARED_SCOPE_KEY = "breezy:auth:scope";

export function getAuthToken(): string {
  if (typeof window === "undefined") return "";
  return window.localStorage.getItem(TOKEN_KEY) || "";
}

export function setAuthToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuthToken(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
}

export function syncSharedAuthToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(SHARED_TOKEN_KEY, token);
}

export function syncSharedAuthScope(scope: "internal" | "external"): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(SHARED_SCOPE_KEY, scope);
}

export function clearSharedAuth(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(SHARED_TOKEN_KEY);
  window.localStorage.removeItem(SHARED_SCOPE_KEY);
}
