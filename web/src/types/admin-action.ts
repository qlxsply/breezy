export type AdminActionTone =
  | "detail"
  | "edit"
  | "enable"
  | "disable"
  | "delete"
  | "pause"
  | "copy"
  | "more"
  | "neutral";

export interface AdminActionItem {
  key: string;
  label: string;
  tone?: AdminActionTone;
  disabled?: boolean;
  handler: () => void;
}
