import type { RealtimeNotificationMessage } from "@admin/features/notifications/model/types";

export function parseSseMessage(
  raw: unknown,
  fallbackType = "SYSTEM_EVENT",
): RealtimeNotificationMessage | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  const notificationId = stringValue(record.notificationId);
  if (!notificationId) return null;
  const priority = stringValue(record.priority) || "LOW";
  return {
    eventId: stringValue(record.eventId),
    notificationId,
    msgType: stringValue(record.msgType) || fallbackType,
    title: stringValue(record.title),
    content: stringValue(record.content ?? record.body),
    route: stringValue(record.route),
    priority,
    panelAutoOpen: booleanValue(record.panelAutoOpen, priority !== "LOW"),
    osNotificationEnabled: booleanValue(record.osNotificationEnabled, priority === "HIGH"),
  };
}

function stringValue(value: unknown): string {
  return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
}

function booleanValue(value: unknown, fallback: boolean): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    if (value.toLowerCase() === "true") return true;
    if (value.toLowerCase() === "false") return false;
  }
  return fallback;
}
