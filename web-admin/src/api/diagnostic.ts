import type {
  DiagnosticCapability,
  DiagnosticConfigPayload,
  DiagnosticEvent,
  DiagnosticEventType,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "../types/diagnostic";
import { get, post } from "./http";

const BASE = "/admin/diagnostic";

export function getDiagnosticStatus(): Promise<DiagnosticSession> {
  return get<DiagnosticSession>(`${BASE}/status`);
}

export function getDiagnosticCapabilities(): Promise<DiagnosticCapability> {
  return get<DiagnosticCapability>(`${BASE}/capabilities`);
}

export function startDiagnostic(payload: DiagnosticConfigPayload): Promise<DiagnosticSession> {
  return post<DiagnosticSession>(`${BASE}/start`, payload);
}

export function updateDiagnosticConfig(
  payload: Partial<DiagnosticConfigPayload>,
): Promise<DiagnosticSession> {
  return post<DiagnosticSession>(`${BASE}/config`, payload);
}

export function stopDiagnostic(): Promise<boolean> {
  return post<boolean>(`${BASE}/stop`, {});
}

export function getLatestDiagnosticSnapshot(): Promise<DiagnosticSnapshot | null> {
  return get<DiagnosticSnapshot | null>(`${BASE}/snapshots/latest`);
}

export function getDiagnosticHistory(limit = 120): Promise<DiagnosticSnapshot[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  return get<DiagnosticSnapshot[]>(`${BASE}/snapshots/history?${params.toString()}`);
}

export function getDiagnosticEvents(
  limit = 100,
  type?: DiagnosticEventType,
): Promise<DiagnosticEvent[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  if (type) {
    params.set("type", type);
  }
  return get<DiagnosticEvent[]>(`${BASE}/events?${params.toString()}`);
}
