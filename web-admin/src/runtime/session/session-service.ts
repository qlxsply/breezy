import { resetAuthSession, revokeAuthSession } from "@admin/features/auth/service/auth-service";
import { resetNotifications } from "@admin/features/notifications/service/notification-service";
import { resetResources } from "@admin/features/resources/service/resource-service";
import { stopAdminRuntime } from "@admin/runtime/admin-runtime";

import { broadcastSessionEnded } from "./session-channel";

interface EndSessionOptions {
  redirectToLogin?: boolean;
  broadcast?: boolean;
}

let endingPromise: Promise<void> | null = null;

export function endSession(options: EndSessionOptions = {}): Promise<void> {
  if (endingPromise) return endingPromise;
  const { redirectToLogin = true, broadcast = true } = options;
  endingPromise = (async () => {
    await stopAdminRuntime({ unsubscribeLocalPush: true });
    resetNotifications();
    resetResources();
    resetAuthSession();
    if (broadcast && typeof window !== "undefined") broadcastSessionEnded();
    if (redirectToLogin && typeof window !== "undefined") redirectToLoginPage();
  })().finally(() => {
    endingPromise = null;
  });
  return endingPromise;
}

export async function logoutSession(): Promise<void> {
  try {
    await stopAdminRuntime({ removeServerPushSubscription: true, unsubscribeLocalPush: true });
    await revokeAuthSession();
  } finally {
    await endSession({ redirectToLogin: true, broadcast: true });
  }
}

function redirectToLoginPage(): void {
  const currentPath = `${window.location.pathname}${window.location.search}`;
  if (window.location.pathname.startsWith("/admin/login")) return;
  window.location.replace(`/admin/login?redirect=${encodeURIComponent(currentPath)}`);
}
