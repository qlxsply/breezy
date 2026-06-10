import { refreshExternalToken } from "../api/auth";
import {
  clearAuthToken,
  getAccessTokenExpiresAt,
  getAuthScope,
  getAuthToken,
  getRefreshToken,
  getRefreshTokenExpiresAt,
  setAccessTokenExpiresAt,
  setAuthScope,
  setAuthToken,
  setRefreshToken,
  setRefreshTokenExpiresAt,
} from "../utils/authStorage";
import { resetAuthPresentationState } from "./auth.registry";
import { openLoginDialog } from "./auth-dialog.registry";

let refreshPromise: Promise<string> | null = null;

export function hasAuthSession(): boolean {
  return Boolean(getAuthToken() || getRefreshToken());
}

export function clearAuthSession(): void {
  clearAuthToken();
  resetAuthPresentationState();
}

export function applyExternalAuthTokens(payload: {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
}): void {
  setAuthToken(payload.accessToken);
  setRefreshToken(payload.refreshToken);
  setAccessTokenExpiresAt(payload.accessTokenExpiresAt);
  setRefreshTokenExpiresAt(payload.refreshTokenExpiresAt);
  setAuthScope("external");
}

export async function getValidAuthToken(): Promise<string> {
  await ensureValidAccessToken();
  return getAuthToken();
}

export async function ensureValidAccessToken(): Promise<void> {
  if (getAuthScope() !== "external") {
    return;
  }

  const accessToken = getAuthToken();
  const refreshToken = getRefreshToken();
  if (!accessToken && !refreshToken) {
    return;
  }

  const accessTokenExpiresAt = toEpochMillis(getAccessTokenExpiresAt());
  if (accessToken && accessTokenExpiresAt && Date.now() < accessTokenExpiresAt) {
    return;
  }

  if (refreshPromise) {
    await refreshPromise;
    return;
  }

  const refreshTokenExpiresAt = toEpochMillis(getRefreshTokenExpiresAt());
  if (!refreshToken || !refreshTokenExpiresAt || Date.now() >= refreshTokenExpiresAt) {
    handleAuthSessionExpired();
    throw new Error("refresh token expired");
  }

  refreshPromise = (async () => {
    const response = await refreshExternalToken(refreshToken);
    if (!response.token || !response.refreshToken) {
      throw new Error("refresh response invalid");
    }
    applyExternalAuthTokens({
      accessToken: response.token,
      refreshToken: response.refreshToken,
      accessTokenExpiresAt: response.accessTokenExpiresAt,
      refreshTokenExpiresAt: response.refreshTokenExpiresAt,
    });
    return response.token;
  })();

  try {
    await refreshPromise;
  } catch (error) {
    handleAuthSessionExpired();
    throw error;
  } finally {
    refreshPromise = null;
  }
}

export function handleAuthSessionExpired(error = "登录已失效，请重新登录"): void {
  clearAuthSession();
  openLoginDialog(error);
}

export function handleUnauthorizedResponse(): void {
  handleAuthSessionExpired("登录状态无效，请重新登录");
}

function toEpochMillis(raw: string): number {
  if (!raw) return 0;
  const value = Number(raw);
  return Number.isFinite(value) ? value : 0;
}
