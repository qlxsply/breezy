"use client";

import { createSseTicket } from "@admin/api/sse";
import { getAuthToken } from "@admin/core/auth-storage";
import { createStore, useStoreValue } from "@admin/core/client-store";
import type { AuthUser } from "@admin/core/registry/auth-registry";
import { hasApiPermission, isPermissionsLoaded } from "@admin/core/registry/permissions-registry";

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
  | "leader-open"
  | "leader-connecting"
  | "leader-reconnecting"
  | "follower"
  | "closed";

interface SseRegistryState {
  state: SseState;
  connected: boolean;
}

type SseMessageHandler = (msg: SseMessage) => void;

interface SseLock {
  tabId: string;
  userId: string;
  expiresAt: number;
  version: number;
}

const LAST_EVENT_ID_KEY = "BREEZY_SSE_LAST_EVENT_ID";
const LOCK_TTL_MS = 10000;
const ELECTION_INTERVAL_MS = 3000;

const sseRegistryStore = createStore<SseRegistryState>({ state: "idle", connected: false });
const globalHandlers = new Set<SseMessageHandler>();
const typedHandlers = new Map<string, Set<SseMessageHandler>>();
const recentEventIdSet = new Set<string>();
const recentEventIdQueue: string[] = [];
const RECENT_EVENT_ID_LIMIT = 1024;

const tabId = Math.random().toString(36).substring(2, 15);
let isMaster = false;
let eventSource: EventSource | null = null;
let channel: BroadcastChannel | null = null;
let electionTimer: ReturnType<typeof setInterval> | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let currentUserId = "";
let masterKey = "";
let syncChannelName = "";
let lifecycleBound = false;

export function useSseState(): SseRegistryState {
  return useStoreValue(sseRegistryStore, (state) => state);
}

export function onSseMessage(handler: SseMessageHandler): () => void {
  globalHandlers.add(handler);
  return () => globalHandlers.delete(handler);
}

export function onSseMessageType(msgType: string, handler: SseMessageHandler): () => void {
  const normalizedType = msgType.trim().toUpperCase();
  if (!typedHandlers.has(normalizedType)) {
    typedHandlers.set(normalizedType, new Set<SseMessageHandler>());
  }
  const handlers = typedHandlers.get(normalizedType);
  handlers?.add(handler);
  return () => {
    const current = typedHandlers.get(normalizedType);
    current?.delete(handler);
    if (current && current.size === 0) {
      typedHandlers.delete(normalizedType);
    }
  };
}

export function resetSseMessageCache(): void {
  recentEventIdSet.clear();
  recentEventIdQueue.length = 0;
}

export function initSseLifecycle(getCurrentUser: () => AuthUser | null): void {
  if (lifecycleBound) {
    return;
  }
  lifecycleBound = true;

  const sync = () => {
    const authUser = getCurrentUser();
    const authenticated = Boolean(authUser?.id) && Boolean(getAuthToken());
    const permissionsReady = isPermissionsLoaded();
    if (authenticated && permissionsReady && hasApiPermission("sys.use") && authUser?.id) {
      init(authUser.id);
      return;
    }
    cleanup();
    resetSseMessageCache();
  };

  window.addEventListener("focus", sync);
  sync();
}

export function init(userId: string): void {
  currentUserId = userId.trim();
  if (!currentUserId || !getAuthToken()) {
    cleanup();
    return;
  }

  masterKey = `BREEZY_SSE_MASTER_${currentUserId}`;
  syncChannelName = `breezy-sse-sync-${currentUserId}`;
  setupChannel();
  startElection();
  checkMaster();
}

export function cleanup(): void {
  cleanupConnection(false);
  if (electionTimer) {
    clearInterval(electionTimer);
    electionTimer = null;
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (channel) {
    channel.close();
    channel = null;
  }
  isMaster = false;
  currentUserId = "";
  masterKey = "";
  syncChannelName = "";
  sseRegistryStore.setState({ state: "closed", connected: false });
}

function setupChannel() {
  if (!syncChannelName || typeof BroadcastChannel === "undefined") {
    return;
  }
  if (channel) {
    channel.close();
  }
  channel = new BroadcastChannel(syncChannelName);
  channel.onmessage = (event) => {
    const data = event.data as { source?: string; payload?: SseMessage; type?: string };
    if (!data || data.source === tabId) {
      return;
    }
    if (data.payload) {
      dispatchSseMessage(data.payload);
    }
  };
}

function startElection() {
  if (electionTimer) {
    clearInterval(electionTimer);
  }
  electionTimer = setInterval(() => {
    checkMaster();
  }, ELECTION_INTERVAL_MS);
}

function getLock(): SseLock | null {
  if (!masterKey || typeof window === "undefined") return null;
  try {
    const val = window.localStorage.getItem(masterKey);
    return val ? (JSON.parse(val) as SseLock) : null;
  } catch {
    return null;
  }
}

function setLock(lock: SseLock) {
  window.localStorage.setItem(masterKey, JSON.stringify(lock));
}

function checkMaster() {
  if (!currentUserId || !getAuthToken()) {
    cleanup();
    return;
  }
  const now = Date.now();
  const lock = getLock();
  const isLockExpired = lock && now > lock.expiresAt;
  const isLockOwnedByMe = lock && lock.tabId === tabId && lock.userId === currentUserId;
  if (!lock || isLockExpired || isLockOwnedByMe) {
    takeMaster(lock ? lock.version : 0);
    return;
  }
  becomeFollower();
}

function takeMaster(currentVersion: number) {
  sseRegistryStore.setState({ state: "leader-connecting", connected: false });
  const now = Date.now();
  const newLock: SseLock = {
    tabId,
    userId: currentUserId,
    expiresAt: now + LOCK_TTL_MS,
    version: currentVersion + 1,
  };
  setLock(newLock);
  window.setTimeout(() => {
    const checkLockValue = getLock();
    if (
      checkLockValue &&
      checkLockValue.tabId === tabId &&
      checkLockValue.version === newLock.version
    ) {
      becomeLeader();
      return;
    }
    becomeFollower();
  }, 50);
}

function becomeLeader() {
  isMaster = true;
  if (channel) {
    channel.postMessage({ type: "MASTER_HEARTBEAT", source: tabId });
  }
  if (!eventSource && sseRegistryStore.getState().state !== "leader-connecting") {
    void connectSse();
  }
}

function becomeFollower() {
  if (isMaster) {
    isMaster = false;
    cleanupConnection(false);
  }
  sseRegistryStore.setState({ state: "follower", connected: false });
}

async function connectSse() {
  const token = getAuthToken();
  if (!token) return;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  sseRegistryStore.setState({ state: "leader-connecting", connected: false });
  cleanupConnection(false);

  let ticket = "";
  try {
    const ticketRes = await createSseTicket();
    ticket = ticketRes.ticket;
  } catch (error) {
    console.error("[sse] Failed to create ticket", error);
    sseRegistryStore.setState({ state: "leader-reconnecting", connected: false });
    scheduleReconnect();
    return;
  }

  const lastEventId = window.localStorage.getItem(LAST_EVENT_ID_KEY);
  const url = new URL("/api/sse/stream", window.location.origin);
  url.searchParams.set("sseTicket", ticket);
  if (lastEventId) {
    url.searchParams.set("lastEventId", lastEventId);
  }

  const es = new EventSource(url.toString());
  eventSource = es;

  es.onopen = () => {
    sseRegistryStore.setState({ state: "leader-open", connected: true });
  };

  const handleSseMessage = (event: MessageEvent) => {
    try {
      if (event.type === "HEARTBEAT") return;
      const data = JSON.parse(event.data);
      const eventId = data.eventId || event.lastEventId;
      const msg: SseMessage = {
        eventId,
        notificationId: data.notificationId || "",
        msgType: data.msgType || event.type,
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
        window.localStorage.setItem(LAST_EVENT_ID_KEY, eventId);
      }
      dispatchSseMessage(msg);
      channel?.postMessage({ source: tabId, payload: msg });
    } catch (error) {
      console.error("[sse] Parse failed", error);
    }
  };

  es.addEventListener("TODO_REMINDER", handleSseMessage);
  es.addEventListener("SYSTEM_EVENT", handleSseMessage);
  es.addEventListener("BUSINESS_EVENT", handleSseMessage);
  es.addEventListener("HEARTBEAT", handleSseMessage);

  es.onerror = () => {
    sseRegistryStore.setState({ state: "leader-reconnecting", connected: false });
    if (eventSource === es) {
      cleanupConnection(false);
    }
    scheduleReconnect();
  };
}

function cleanupConnection(resetState = true) {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  if (resetState) {
    sseRegistryStore.setState({ state: "idle", connected: false });
  }
}

function scheduleReconnect(delayMs = 1200) {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    if (!isMaster || !currentUserId || !getAuthToken()) return;
    void connectSse();
  }, delayMs);
}

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
  if (recentEventIdSet.has(normalized)) return true;
  recentEventIdSet.add(normalized);
  recentEventIdQueue.push(normalized);
  if (recentEventIdQueue.length > RECENT_EVENT_ID_LIMIT) {
    const removed = recentEventIdQueue.shift();
    if (removed) recentEventIdSet.delete(removed);
  }
  return false;
}

function dispatchSseMessage(msg: SseMessage): void {
  const normalized = normalizeSseMessage(msg);
  if (normalized.eventId && markAndCheckDuplicate(normalized.eventId)) {
    return;
  }
  globalHandlers.forEach((handler) => handler(normalized));
  const handlers = typedHandlers.get(normalized.msgType);
  handlers?.forEach((handler) => handler(normalized));
}

function resolveBoolean(value: unknown, fallback: boolean): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") return value.trim().toLowerCase() === "true";
  return fallback;
}
