export type LoginEvent = "LOGIN_SUCCESS" | "LOGIN_FAILURE" | "LOGOUT";

export interface LoginLogEntry {
  id: string;
  userId?: string | null;
  username?: string | null;
  eventType: LoginEvent;
  success: boolean;
  loginIp?: string | null;
  failureReason?: string | null;
  sessionId?: string | null;
  operatorId?: string | null;
  occurredAt?: string | null;
  remark?: string | null;
}
