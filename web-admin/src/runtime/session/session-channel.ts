const CHANNEL_NAME = "breezy-admin-session";
const STORAGE_KEY = "BREEZY_ADMIN_SESSION_EVENT";

interface SessionEvent {
  id: string;
  type: "SESSION_ENDED";
  timestamp: number;
}

let channel: BroadcastChannel | null = null;
let storageHandler: ((event: StorageEvent) => void) | null = null;

export function startSessionChannel(onSessionEnded: () => void): () => void {
  stopSessionChannel();
  if (typeof BroadcastChannel !== "undefined") {
    channel = new BroadcastChannel(CHANNEL_NAME);
    channel.onmessage = (event: MessageEvent<unknown>) => {
      if (isSessionEndedEvent(event.data)) onSessionEnded();
    };
  }
  storageHandler = (event) => {
    if (event.key !== STORAGE_KEY || !event.newValue) return;
    try {
      if (isSessionEndedEvent(JSON.parse(event.newValue) as unknown)) onSessionEnded();
    } catch {
      // Ignore malformed cross-tab events.
    }
  };
  window.addEventListener("storage", storageHandler);
  return stopSessionChannel;
}

export function broadcastSessionEnded(): void {
  const event: SessionEvent = {
    id:
      typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    type: "SESSION_ENDED",
    timestamp: Date.now(),
  };
  channel?.postMessage(event);
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(event));
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // BroadcastChannel remains the primary transport when storage is unavailable.
  }
}

function stopSessionChannel(): void {
  channel?.close();
  channel = null;
  if (storageHandler) window.removeEventListener("storage", storageHandler);
  storageHandler = null;
}

function isSessionEndedEvent(value: unknown): value is SessionEvent {
  if (!value || typeof value !== "object") return false;
  const record = value as Record<string, unknown>;
  return record.type === "SESSION_ENDED" && typeof record.id === "string";
}
