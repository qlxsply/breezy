import type { RealtimeNotificationMessage } from "@admin/features/notifications/model/types";
import { resolveApiUrl } from "@admin/shared/transport";

import { createSseTicket } from "./sse-client";
import { parseSseMessage } from "./sse-message-parser";
import { setSseState } from "./sse-store";

interface SseLock {
  tabId: string;
  userId: string;
  expiresAt: number;
  version: number;
}

interface ActiveRuntime {
  generation: number;
  userId: string;
  onMessage: (message: RealtimeNotificationMessage) => void;
  controller: AbortController;
}

const LOCK_TTL_MS = 7_000;
const ELECTION_INTERVAL_MS = 2_500;
const RECONNECT_DELAY_MS = 1_500;
const tabId =
  typeof crypto !== "undefined" && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random()}`;

let runtimeGeneration = 0;
let activeRuntime: ActiveRuntime | null = null;
let eventSource: EventSource | null = null;
let channel: BroadcastChannel | null = null;
let electionTimer: number | null = null;
let reconnectTimer: number | null = null;
let connecting = false;
let leader = false;
let focusHandler: (() => void) | null = null;
let storageHandler: ((event: StorageEvent) => void) | null = null;
let connectionAttempt = 0;

export function startSseRuntime(
  userId: string,
  onMessage: (message: RealtimeNotificationMessage) => void,
): void {
  const normalizedUserId = userId.trim();
  if (!normalizedUserId || typeof window === "undefined") return;
  if (activeRuntime?.userId === normalizedUserId) {
    activeRuntime.onMessage = onMessage;
    return;
  }

  stopSseRuntime();
  const generation = ++runtimeGeneration;
  activeRuntime = {
    generation,
    userId: normalizedUserId,
    onMessage,
    controller: new AbortController(),
  };
  setSseState("electing");
  setupChannel(activeRuntime);
  focusHandler = () => checkLeadership(generation);
  storageHandler = (event) => {
    if (event.key === lockKey(normalizedUserId)) checkLeadership(generation);
  };
  window.addEventListener("focus", focusHandler);
  window.addEventListener("storage", storageHandler);
  electionTimer = window.setInterval(() => checkLeadership(generation), ELECTION_INTERVAL_MS);
  checkLeadership(generation);
}

export function stopSseRuntime(): void {
  runtimeGeneration += 1;
  activeRuntime?.controller.abort();
  releaseOwnedLock();
  activeRuntime = null;
  leader = false;
  connecting = false;
  connectionAttempt += 1;
  closeConnection();
  if (electionTimer) window.clearInterval(electionTimer);
  if (reconnectTimer) window.clearTimeout(reconnectTimer);
  electionTimer = null;
  reconnectTimer = null;
  channel?.close();
  channel = null;
  if (focusHandler) window.removeEventListener("focus", focusHandler);
  focusHandler = null;
  if (storageHandler) window.removeEventListener("storage", storageHandler);
  storageHandler = null;
  setSseState("stopped");
}

function setupChannel(runtime: ActiveRuntime): void {
  if (typeof BroadcastChannel === "undefined") return;
  channel = new BroadcastChannel(channelName(runtime.userId));
  channel.onmessage = (event: MessageEvent<unknown>) => {
    if (!isCurrent(runtime.generation) || !event.data || typeof event.data !== "object") return;
    const record = event.data as Record<string, unknown>;
    if (record.source === tabId || record.type !== "SSE_MESSAGE") return;
    const message = parseSseMessage(record.payload);
    if (message) activeRuntime?.onMessage(message);
  };
}

function checkLeadership(generation: number): void {
  const runtime = activeRuntime;
  if (!runtime || runtime.generation !== generation) return;
  const now = Date.now();
  const current = readLock(runtime.userId);
  if (!current || current.expiresAt <= now || current.tabId === tabId) {
    const candidate: SseLock = {
      tabId,
      userId: runtime.userId,
      expiresAt: now + LOCK_TTL_MS,
      version: (current?.version ?? 0) + 1,
    };
    writeLock(runtime.userId, candidate);
    window.setTimeout(() => {
      if (!isCurrent(generation)) return;
      const verified = readLock(runtime.userId);
      if (verified?.tabId === tabId && verified.version === candidate.version)
        becomeLeader(generation);
      else becomeFollower();
    }, 50);
    return;
  }
  becomeFollower();
}

function becomeLeader(generation: number): void {
  const runtime = activeRuntime;
  if (!runtime || runtime.generation !== generation) return;
  leader = true;
  const lock = readLock(runtime.userId);
  writeLock(runtime.userId, {
    tabId,
    userId: runtime.userId,
    expiresAt: Date.now() + LOCK_TTL_MS,
    version: lock?.version ?? 1,
  });
  clearReconnectTimer();
  if (!eventSource && !connecting) void connect(generation);
}

function becomeFollower(): void {
  leader = false;
  connecting = false;
  connectionAttempt += 1;
  clearReconnectTimer();
  closeConnection();
  setSseState("follower");
}

async function connect(generation: number): Promise<void> {
  const runtime = activeRuntime;
  if (!runtime || runtime.generation !== generation || !leader) return;
  const attempt = ++connectionAttempt;
  connecting = true;
  setSseState(eventSource ? "reconnecting" : "connecting");
  try {
    const ticket = await createSseTicket({ signal: runtime.controller.signal });
    if (attempt !== connectionAttempt || !isCurrentLeader(generation, runtime.userId)) return;

    const url = new URL(resolveApiUrl("/sse/stream"), window.location.origin);
    url.searchParams.set("sseTicket", ticket.ticket);
    const lastEventId = window.localStorage.getItem(cursorKey(runtime.userId));
    if (lastEventId) url.searchParams.set("lastEventId", lastEventId);

    const source = new EventSource(url.toString());
    eventSource = source;
    source.onopen = () => {
      if (source === eventSource) setSseState("connected", true);
    };
    const receive = (event: MessageEvent<string>) =>
      receiveMessage(event, generation, runtime.userId);
    source.addEventListener("TODO_REMINDER", receive);
    source.addEventListener("SYSTEM_EVENT", receive);
    source.addEventListener("BUSINESS_EVENT", receive);
    source.onerror = () => {
      if (source !== eventSource) return;
      closeConnection();
      scheduleReconnect(generation);
    };
  } catch (error) {
    if (!(error instanceof DOMException && error.name === "AbortError")) {
      console.warn("[sse] connection failed", error);
      scheduleReconnect(generation);
    }
  } finally {
    if (attempt === connectionAttempt) connecting = false;
  }
}

function receiveMessage(event: MessageEvent<string>, generation: number, userId: string): void {
  if (!isCurrentLeader(generation, userId)) return;
  try {
    const raw = JSON.parse(event.data) as unknown;
    const message = parseSseMessage(raw, event.type);
    if (!message) return;
    const eventId = message.eventId || event.lastEventId;
    const normalized = { ...message, eventId };
    if (eventId) window.localStorage.setItem(cursorKey(userId), eventId);
    activeRuntime?.onMessage(normalized);
    channel?.postMessage({ type: "SSE_MESSAGE", source: tabId, payload: normalized });
  } catch (error) {
    console.warn("[sse] invalid message", error);
  }
}

function scheduleReconnect(generation: number): void {
  if (!isCurrent(generation) || !leader || reconnectTimer) return;
  setSseState("reconnecting");
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null;
    if (
      activeRuntime &&
      !eventSource &&
      !connecting &&
      isCurrentLeader(generation, activeRuntime.userId)
    )
      void connect(generation);
  }, RECONNECT_DELAY_MS);
}

function closeConnection(): void {
  eventSource?.close();
  eventSource = null;
}

function clearReconnectTimer(): void {
  if (reconnectTimer) window.clearTimeout(reconnectTimer);
  reconnectTimer = null;
}

function isCurrent(generation: number): boolean {
  return activeRuntime?.generation === generation && runtimeGeneration === generation;
}

function isCurrentLeader(generation: number, userId: string): boolean {
  return isCurrent(generation) && leader && readLock(userId)?.tabId === tabId;
}

function readLock(userId: string): SseLock | null {
  try {
    const raw = window.localStorage.getItem(lockKey(userId));
    if (!raw) return null;
    const value = JSON.parse(raw) as unknown;
    if (!value || typeof value !== "object") return null;
    const record = value as Record<string, unknown>;
    if (
      typeof record.tabId !== "string" ||
      record.userId !== userId ||
      typeof record.expiresAt !== "number" ||
      typeof record.version !== "number"
    ) {
      return null;
    }
    return record as unknown as SseLock;
  } catch {
    return null;
  }
}

function writeLock(userId: string, lock: SseLock): void {
  window.localStorage.setItem(lockKey(userId), JSON.stringify(lock));
}

function releaseOwnedLock(): void {
  const runtime = activeRuntime;
  if (!runtime) return;
  if (readLock(runtime.userId)?.tabId === tabId)
    window.localStorage.removeItem(lockKey(runtime.userId));
}

const lockKey = (userId: string) => `BREEZY_SSE_MASTER_${userId}`;
const channelName = (userId: string) => `breezy-sse-sync-${userId}`;
const cursorKey = (userId: string) => `BREEZY_SSE_LAST_EVENT_ID_${userId}`;
