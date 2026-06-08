import { ref } from "vue";

import { createSseTicket } from "../api/sse";
import { getAuthToken } from "./authStorage";

export interface SseMessage {
  eventId: string;
  notificationId: string;
  msgType: string;
  title: string;
  content: string;
  route: string;
  priority: string;
  panelAutoOpen: boolean;
  osNotificationEnabled: boolean;
}

export type SseState =
  | "idle"
  | "electing"
  | "leader-connecting"
  | "leader-open"
  | "leader-reconnecting"
  | "follower"
  | "closed";

interface SseLock {
  tabId: string;
  userId: string;
  expiresAt: number;
  version: number;
}

const LAST_EVENT_ID_KEY = "BREEZY_SSE_LAST_EVENT_ID";
const LOCK_TTL_MS = 10000;
const ELECTION_INTERVAL_MS = 3000;
const DEAD_TIMEOUT_MS = 40000;

export function useSseCoordinator() {
  const tabId = ref(Math.random().toString(36).substring(2, 15));
  const isMaster = ref(false);
  const sseState = ref<SseState>("idle");
  const eventSource = ref<EventSource | null>(null);
  const subscribers = new Set<(msg: SseMessage) => void>();

  let channel: BroadcastChannel | null = null;
  let electionTimer: ReturnType<typeof setInterval> | null = null;
  let deadCheckTimer: ReturnType<typeof setInterval> | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let lastMessageTime = 0;
  let beforeUnloadHandler: (() => void) | null = null;

  // 动态生成隔离 Key
  let currentUserId = "";
  let masterKey = "";
  let syncChannelName = "";

  function getLock(): SseLock | null {
    try {
      const val = localStorage.getItem(masterKey);
      return val ? JSON.parse(val) : null;
    } catch {
      return null;
    }
  }

  function setLock(lock: SseLock) {
    localStorage.setItem(masterKey, JSON.stringify(lock));
  }

  function checkMaster() {
    if (!currentUserId || !getAuthToken()) {
      cleanup();
      return;
    }

    const now = Date.now();
    const lock = getLock();

    const isLockExpired = lock && now > lock.expiresAt;
    const isLockOwnedByMe = lock && lock.tabId === tabId.value && lock.userId === currentUserId;

    if (!lock || isLockExpired || isLockOwnedByMe) {
      takeMaster(lock ? lock.version : 0);
    } else {
      becomeFollower();
    }
  }

  function takeMaster(currentVersion: number) {
    if (
      sseState.value !== "leader-open" &&
      sseState.value !== "leader-connecting" &&
      sseState.value !== "leader-reconnecting"
    ) {
      sseState.value = "electing";
    }

    const now = Date.now();
    const newLock: SseLock = {
      tabId: tabId.value,
      userId: currentUserId,
      expiresAt: now + LOCK_TTL_MS,
      version: currentVersion + 1,
    };

    // 写前竞争逻辑
    setLock(newLock);

    // 二次校验，防止脑裂（极小概率在同一毫秒写入）
    setTimeout(() => {
      const checkLock = getLock();
      if (checkLock && checkLock.tabId === tabId.value && checkLock.version === newLock.version) {
        becomeLeader();
      } else {
        becomeFollower();
      }
    }, 50);
  }

  function becomeLeader() {
    if (!isMaster.value) {
      console.log("[sse] I am the leader now:", tabId.value);
      isMaster.value = true;
    }

    if (channel) {
      channel.postMessage({ type: "MASTER_HEARTBEAT", source: tabId.value });
    }

    if (!eventSource.value && sseState.value !== "leader-connecting") {
      void connectSse();
    }
  }

  function becomeFollower() {
    if (isMaster.value) {
      console.log("[sse] Stepping down to follower:", tabId.value);
      isMaster.value = false;
      cleanupConnection();
    }
    sseState.value = "follower";
  }

  async function connectSse() {
    const token = getAuthToken();
    if (!token) return;

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    sseState.value = "leader-connecting";
    cleanupConnection(false);

    let ticket = "";
    try {
      const ticketRes = await createSseTicket();
      ticket = ticketRes.ticket;
    } catch (err) {
      console.error("[sse] Failed to create ticket", err);
      sseState.value = "leader-reconnecting";
      return;
    }

    const lastEventId = localStorage.getItem(LAST_EVENT_ID_KEY);
    const url = new URL("/api/sse/stream", window.location.origin);
    url.searchParams.set("sseTicket", ticket);
    if (lastEventId) {
      url.searchParams.set("lastEventId", lastEventId);
    }

    console.log("[sse] Connecting to SSE...");
    const es = new EventSource(url.toString());
    eventSource.value = es;

    es.onopen = () => {
      console.log("[sse] Connected");
      sseState.value = "leader-open";
      lastMessageTime = Date.now();
      startDeadCheck();
    };

    const handleSseMessage = (e: MessageEvent) => {
      lastMessageTime = Date.now();
      try {
        // 处理服务器的心跳事件，仅用于保活
        if (e.type === "HEARTBEAT") return;

        const data = JSON.parse(e.data);
        const eventId = data.eventId || e.lastEventId;
        const msg: SseMessage = {
          eventId,
          notificationId: data.notificationId || "",
          msgType: data.msgType || e.type,
          title: data.title || "",
          content: data.content || data.body || "",
          route: data.route || "",
          priority: data.priority || "LOW",
          panelAutoOpen: resolveBoolean(data.panelAutoOpen, (data.priority || "LOW") !== "LOW"),
          osNotificationEnabled: resolveBoolean(
            data.osNotificationEnabled,
            (data.priority || "LOW") === "HIGH",
          ),
        };

        if (eventId) {
          localStorage.setItem(LAST_EVENT_ID_KEY, eventId);
        }

        dispatchToSubscribers(msg);

        if (channel) {
          channel.postMessage({
            source: tabId.value,
            payload: msg,
          });
        }
      } catch (err) {
        console.error("[sse] Parse failed", err);
      }
    };

    es.addEventListener("TODO_REMINDER", handleSseMessage);
    es.addEventListener("SYSTEM_EVENT", handleSseMessage);
    es.addEventListener("BUSINESS_EVENT", handleSseMessage);
    es.addEventListener("HEARTBEAT", handleSseMessage);

    es.onerror = () => {
      console.error("[sse] Connection error");
      if (sseState.value === "leader-open" || sseState.value === "leader-connecting") {
        sseState.value = "leader-reconnecting";
      }

      // SSE ticket 为一次性消费，连接断开后必须重新申请 ticket 再重连。
      // 主动关闭当前 EventSource，避免浏览器反复用旧 ticket 自动重试。
      if (eventSource.value === es) {
        cleanupConnection(false);
      }
      scheduleReconnect();
    };
  }

  function scheduleReconnect(delayMs = 1200) {
    if (reconnectTimer) {
      return;
    }
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      if (!isMaster.value) {
        return;
      }
      if (!currentUserId || !getAuthToken()) {
        return;
      }
      void connectSse();
    }, delayMs);
  }

  function startDeadCheck() {
    if (deadCheckTimer) clearInterval(deadCheckTimer);
    deadCheckTimer = setInterval(() => {
      if (sseState.value === "leader-open" && eventSource.value) {
        const now = Date.now();
        if (now - lastMessageTime > DEAD_TIMEOUT_MS) {
          console.warn("[sse] Dead connection detected, reconnecting...");
          sseState.value = "leader-reconnecting";
          cleanupConnection(false);
          void connectSse();
        }
      }
    }, 10000);
  }

  function cleanupConnection(changeState = true) {
    if (eventSource.value) {
      eventSource.value.close();
      eventSource.value = null;
    }
    if (deadCheckTimer) {
      clearInterval(deadCheckTimer);
      deadCheckTimer = null;
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (changeState) {
      sseState.value = "closed";
    }
  }

  function dispatchToSubscribers(msg: SseMessage) {
    subscribers.forEach((fn) => fn(msg));
  }

  function subscribe(fn: (msg: SseMessage) => void) {
    subscribers.add(fn);
  }

  function unsubscribe(fn: (msg: SseMessage) => void) {
    subscribers.delete(fn);
  }

  function handleStorageChange(e: StorageEvent) {
    if (e.key === masterKey) {
      // 如果是被别人抢走了，立刻退居 follower
      const newLock = getLock();
      if (newLock && newLock.tabId !== tabId.value && isMaster.value) {
        becomeFollower();
      } else if (!newLock && !isMaster.value) {
        // 如果锁被清空（例如 leader 正常退出），立即尝试竞选
        checkMaster();
      }
    }
  }

  function cleanup() {
    if (electionTimer) {
      clearInterval(electionTimer);
      electionTimer = null;
    }
    if (isMaster.value) {
      const lock = getLock();
      if (lock && lock.tabId === tabId.value) {
        localStorage.removeItem(masterKey);
      }
      if (channel) channel.postMessage({ type: "MASTER_LEAVING", source: tabId.value });
    }
    cleanupConnection();
    isMaster.value = false;
    sseState.value = "closed";

    window.removeEventListener("storage", handleStorageChange);
    if (beforeUnloadHandler) {
      window.removeEventListener("beforeunload", beforeUnloadHandler);
      beforeUnloadHandler = null;
    }
    if (channel) {
      channel.close();
      channel = null;
    }

    currentUserId = "";
    masterKey = "";
    syncChannelName = "";
  }

  function init(userId: string) {
    if (!userId) return;

    // 身份发生切换时，清理掉旧的环境
    if (currentUserId && currentUserId !== userId) {
      cleanup();
    }

    if (sseState.value !== "idle" && sseState.value !== "closed") {
      return;
    }

    currentUserId = userId;
    masterKey = `BREEZY_SSE_MASTER_${userId}`;
    syncChannelName = `BREEZY_SSE_SYNC_${userId}`;

    if (!channel) {
      channel = new BroadcastChannel(syncChannelName);
      channel.onmessage = (e) => {
        if (e.data.type === "MASTER_HEARTBEAT") {
          if (e.data.source !== tabId.value && isMaster.value) {
            // 收到别人心跳，说明发生了脑裂，比拼 tabId 决定去留
            if (tabId.value > e.data.source) {
              becomeFollower();
            }
          }
        } else if (e.data.type === "MASTER_LEAVING") {
          if (!isMaster.value) checkMaster();
        } else if (e.data.source !== tabId.value && e.data.payload) {
          dispatchToSubscribers(e.data.payload);
        }
      };
    }

    window.removeEventListener("storage", handleStorageChange);
    window.addEventListener("storage", handleStorageChange);

    if (beforeUnloadHandler) {
      window.removeEventListener("beforeunload", beforeUnloadHandler);
    }
    beforeUnloadHandler = () => {
      cleanup();
    };
    window.addEventListener("beforeunload", beforeUnloadHandler);

    checkMaster();

    if (electionTimer) clearInterval(electionTimer);
    electionTimer = setInterval(checkMaster, ELECTION_INTERVAL_MS);
  }

  return {
    subscribe,
    unsubscribe,
    isMaster,
    sseState,
    init,
    cleanup,
  };
}

function resolveBoolean(raw: unknown, fallback: boolean): boolean {
  if (typeof raw === "boolean") {
    return raw;
  }
  if (typeof raw === "string") {
    const normalized = raw.trim().toLowerCase();
    if (normalized === "true") {
      return true;
    }
    if (normalized === "false") {
      return false;
    }
  }
  return fallback;
}
