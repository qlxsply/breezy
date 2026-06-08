export type ExternalUserStatus = "ACTIVE" | "DISABLED" | "CANCELLED";

export interface ExternalUserEntry {
  id: string;
  account: string;
  username: string;
  nickname: string;
  userType: "EXTERNAL";
  status: ExternalUserStatus;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}
