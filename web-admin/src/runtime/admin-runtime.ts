import { getAuthSessionSnapshot } from "@admin/features/auth/model/auth-store";
import { loadAuthSession } from "@admin/features/auth/service/auth-service";
import {
  acceptRealtimeNotification,
  loadUnreadNotifications,
} from "@admin/features/notifications/service/notification-service";
import { loadResources } from "@admin/features/resources/service/resource-service";

import { startWebPushRuntime, stopWebPushRuntime } from "./push/push-runtime";
import { startSseRuntime, stopSseRuntime } from "./sse/sse-runtime";

let leaseCount = 0;
let runtimeGeneration = 0;
let runtimeController: AbortController | null = null;
let initializationPromise: Promise<void> | null = null;
let delayedStop: ReturnType<typeof setTimeout> | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;

const RETRY_DELAY_MS = 3_000;

export function acquireAdminRuntime(): () => void {
  leaseCount += 1;
  if (delayedStop) clearTimeout(delayedStop);
  delayedStop = null;
  void initializeAdminRuntime();

  let released = false;
  return () => {
    if (released) return;
    released = true;
    leaseCount = Math.max(0, leaseCount - 1);
    if (leaseCount === 0) {
      delayedStop = setTimeout(() => {
        delayedStop = null;
        if (leaseCount === 0) void stopAdminRuntime();
      }, 0);
    }
  };
}

export async function stopAdminRuntime(
  options: { removeServerPushSubscription?: boolean; unsubscribeLocalPush?: boolean } = {},
): Promise<void> {
  runtimeGeneration += 1;
  runtimeController?.abort();
  runtimeController = null;
  initializationPromise = null;
  if (retryTimer) clearTimeout(retryTimer);
  retryTimer = null;
  stopSseRuntime();
  await stopWebPushRuntime({
    removeServerSubscription: options.removeServerPushSubscription,
    unsubscribeLocal: options.unsubscribeLocalPush,
  });
}

async function initializeAdminRuntime(): Promise<void> {
  if (initializationPromise) return initializationPromise;
  const generation = ++runtimeGeneration;
  const controller = new AbortController();
  runtimeController?.abort();
  runtimeController = controller;

  initializationPromise = (async () => {
    const session = await loadAuthSession({ signal: controller.signal });
    if (
      !isCurrent(generation) ||
      session.status !== "authenticated" ||
      session.user?.userType !== "ADMIN"
    ) {
      return;
    }

    await loadResources({ signal: controller.signal });
    if (!isCurrentSession(generation, session.user.id)) return;

    await loadUnreadNotifications({ signal: controller.signal });
    if (!isCurrentSession(generation, session.user.id)) return;

    startSseRuntime(session.user.id ?? "", acceptRealtimeNotification);
    await startWebPushRuntime(session.user.id ?? "", acceptRealtimeNotification);
  })()
    .catch((error: unknown) => {
      if (!(error instanceof DOMException && error.name === "AbortError")) {
        console.warn("[runtime] admin initialization failed", error);
        scheduleInitializationRetry(generation);
      }
    })
    .finally(() => {
      if (generation === runtimeGeneration) initializationPromise = null;
    });
  return initializationPromise;
}

function scheduleInitializationRetry(generation: number): void {
  if (retryTimer || generation !== runtimeGeneration || leaseCount === 0) return;
  retryTimer = setTimeout(() => {
    retryTimer = null;
    if (generation === runtimeGeneration && leaseCount > 0) void initializeAdminRuntime();
  }, RETRY_DELAY_MS);
}

function isCurrent(generation: number): boolean {
  return generation === runtimeGeneration && !runtimeController?.signal.aborted;
}

function isCurrentSession(generation: number, userId: string | null): boolean {
  const session = getAuthSessionSnapshot();
  return (
    isCurrent(generation) &&
    session.status === "authenticated" &&
    Boolean(userId) &&
    session.user?.id === userId
  );
}
