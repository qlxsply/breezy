import { readText, removeKey, writeText } from "./storage";

const TOKEN_KEY = "breezy:auth:token";
const SCOPE_KEY = "breezy:auth:scope";

export type AuthScope = "internal" | "external";

export function getAuthToken(): string {
  return readText(TOKEN_KEY, "");
}

export function setAuthToken(token: string): void {
  writeText(TOKEN_KEY, token);
}

export function getAuthScope(): AuthScope {
  return readText(SCOPE_KEY, "external") === "internal" ? "internal" : "external";
}

export function setAuthScope(scope: AuthScope): void {
  writeText(SCOPE_KEY, scope);
}

export function clearAuthToken(): void {
  removeKey(TOKEN_KEY);
  removeKey(SCOPE_KEY);
}
