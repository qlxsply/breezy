import { get, post, type RequestOptions } from "@admin/shared/transport";

import type {
  DiagnosticCapability,
  DiagnosticEvent,
  DiagnosticEventType,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "../model/types";
import type { DiagnosticConfigPayload } from "./payload";

const BASE = "/admin/diagnostic";

type DiagnosticRequestOptions = Pick<RequestOptions, "signal">;

export function getDiagnosticStatus(
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticSession> {
  return get<DiagnosticSession>(`${BASE}/status`, options);
}

export function getDiagnosticCapabilities(
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticCapability> {
  return get<DiagnosticCapability>(`${BASE}/capabilities`, options);
}

export function startDiagnostic(
  payload: DiagnosticConfigPayload,
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticSession> {
  return post<DiagnosticSession>(`${BASE}/start`, payload, options);
}

export function updateDiagnosticConfig(
  payload: Partial<DiagnosticConfigPayload>,
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticSession> {
  return post<DiagnosticSession>(`${BASE}/config`, payload, options);
}

export function stopDiagnostic(options?: DiagnosticRequestOptions): Promise<boolean> {
  return post<boolean>(`${BASE}/stop`, {}, options);
}

export function getLatestDiagnosticSnapshot(
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticSnapshot | null> {
  return get<DiagnosticSnapshot | null>(`${BASE}/snapshots/latest`, options);
}

export function getDiagnosticHistory(
  limit = 120,
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticSnapshot[]> {
  return post<DiagnosticSnapshot[]>(`${BASE}/snapshots/history`, { limit }, options);
}

export function getDiagnosticEvents(
  limit = 100,
  type?: DiagnosticEventType,
  options?: DiagnosticRequestOptions,
): Promise<DiagnosticEvent[]> {
  return post<DiagnosticEvent[]>(`${BASE}/events`, { limit, type }, options);
}
