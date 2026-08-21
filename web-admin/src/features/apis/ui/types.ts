import type { ApiEntry } from "@admin/features/apis/model/types";

export interface ApiTableRow extends ApiEntry {
  protocolLabel: string;
  httpMethodLabel: string;
  accessTypeLabel: string;
  userTypeLabel: string;
  auditTooltip: string;
}
