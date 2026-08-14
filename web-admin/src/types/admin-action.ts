export type AdminActionLevel = "default" | "primary" | "success" | "warning" | "danger";

export interface AdminActionItem {
  key: string;
  label: string;
  level?: AdminActionLevel;
  disabled?: boolean;
  onClick: () => void;
}
