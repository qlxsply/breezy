import type { DiagnosticItem } from "../model/types";

export interface DiagnosticConfigPayload {
  intervalMs: number;
  historyCapacity: number;
  eventCapacity: number;
  items: DiagnosticItem[];
  deepMode: boolean;
  slowRequestThresholdMs: number;
  slowSqlThresholdMs: number;
  ttlSeconds: number;
}
