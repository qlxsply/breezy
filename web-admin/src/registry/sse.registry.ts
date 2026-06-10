import { computed, watch } from "vue";

import type { SseMessage } from "../utils/sse-coordinator";
import { useSseCoordinator } from "../utils/sse-coordinator";
import { isAuthenticated, useAuthUser } from "./auth.registry";
import { hasApiPermission, isPermissionsLoaded } from "./permissions.registry";

export const sseCoordinator = useSseCoordinator();

type SseMessageHandler = (msg: SseMessage) => void;

const globalHandlers = new Set<SseMessageHandler>();
const typedHandlers = new Map<string, Set<SseMessageHandler>>();

const recentEventIdSet = new Set<string>();
const recentEventIdQueue: string[] = [];
const RECENT_EVENT_ID_LIMIT = 1024;

function normalizeSseRoute(rawRoute: string | null | undefined): string {
  const trimmed = (rawRoute ?? "").trim();
  if (!trimmed) return "";
  if (trimmed === "/todo-all") return "/todo/all";
  if (!trimmed.startsWith("/")) return "";
  return trimmed;
}

function normalizeSseMessage(msg: SseMessage): SseMessage {
  return {
    ...msg,
    notificationId: (msg.notificationId || "").trim(),
    msgType: (msg.msgType || "").trim().toUpperCase(),
    priority: (msg.priority || "LOW").trim().toUpperCase(),
    route: normalizeSseRoute(msg.route),
  };
}

function markAndCheckDuplicate(eventId: string): boolean {
  const normalized = eventId.trim();
  if (!normalized) return false;
  if (recentEventIdSet.has(normalized)) {
    return true;
  }
  recentEventIdSet.add(normalized);
  recentEventIdQueue.push(normalized);
  if (recentEventIdQueue.length > RECENT_EVENT_ID_LIMIT) {
    const removed = recentEventIdQueue.shift();
    if (removed) {
      recentEventIdSet.delete(removed);
    }
  }
  return false;
}

function dispatchSseMessage(msg: SseMessage): void {
  const normalized = normalizeSseMessage(msg);
  if (normalized.eventId && markAndCheckDuplicate(normalized.eventId)) {
    return;
  }

  globalHandlers.forEach((handler) => {
    handler(normalized);
  });

  const handlers = typedHandlers.get(normalized.msgType);
  handlers?.forEach((handler) => {
    handler(normalized);
  });
}

sseCoordinator.subscribe((msg) => {
  dispatchSseMessage(msg);
});

export function onSseMessage(handler: SseMessageHandler): () => void {
  globalHandlers.add(handler);
  return () => {
    globalHandlers.delete(handler);
  };
}

export function onSseMessageType(msgType: string, handler: SseMessageHandler): () => void {
  const normalizedType = msgType.trim().toUpperCase();
  if (!typedHandlers.has(normalizedType)) {
    typedHandlers.set(normalizedType, new Set<SseMessageHandler>());
  }
  const handlers = typedHandlers.get(normalizedType);
  handlers?.add(handler);

  return () => {
    const currentHandlers = typedHandlers.get(normalizedType);
    if (!currentHandlers) {
      return;
    }
    currentHandlers.delete(handler);
    if (currentHandlers.size === 0) {
      typedHandlers.delete(normalizedType);
    }
  };
}

export function resetSseMessageCache(): void {
  recentEventIdSet.clear();
  recentEventIdQueue.length = 0;
}

/**
 * SSE 生命周期管理逻辑
 * 只有当登录状态和权限数据都准备就绪时，才进行权限检查并启动 SSE
 */
export function initSseLifecycle() {
  const authUser = useAuthUser();
  const readyToConnect = computed(() => isAuthenticated.value && isPermissionsLoaded.value);

  watch(
    readyToConnect,
    (ready) => {
      console.log("[sse] readyToConnect check:", {
        ready,
        authenticated: isAuthenticated.value,
        permissionsLoaded: isPermissionsLoaded.value,
      });
      if (ready) {
        // 此时登录状态和权限数据均已加载
        const hasPermission = hasApiPermission("sys.use");
        console.log("[sse] has sys.use permission:", hasPermission);
        const currentUserId = authUser.value?.id;
        if (hasPermission && currentUserId) {
          console.log("[sse] Permission verified, connecting for user:", currentUserId);
          sseCoordinator.init(currentUserId);
        } else {
          console.warn("[sse] No permission (sys.use) or missing user id to start SSE");
        }
      } else if (!isAuthenticated.value) {
        // 只要登录状态变为 false，立即清理
        sseCoordinator.cleanup();
        resetSseMessageCache();
      }
    },
    { immediate: true },
  );
}
