import { get, post } from "@admin/shared/transport";

import type {
  DiagnosticCapability,
  DiagnosticConfigPayload,
  DiagnosticEvent,
  DiagnosticEventType,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "../types/diagnostic";

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
  return post<DiagnosticSnapshot[]>(`${BASE}/snapshots/history`, { limit });
}

export function getDiagnosticEvents(
  limit = 100,
  type?: DiagnosticEventType,
): Promise<DiagnosticEvent[]> {
  return post<DiagnosticEvent[]>(`${BASE}/events`, { limit, type });
}
