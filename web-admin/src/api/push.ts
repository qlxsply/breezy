import { del, get, post } from "./http";

const PUSH_BASE = "/push";

export interface PushPublicKeyRes {
  publicKey: string;
}

export interface SavePushSubscriptionReq {
  deviceId: string;
  endpoint: string;
  p256dh: string;
  auth: string;
}

export interface PushHealthSubscriptionRes {
  deviceId: string;
  active: boolean;
  endpointHost: string;
  updatedAt: string | null;
  lastPushAt: string | null;
  lastError: string;
}

export interface PushHealthDeliveryRes {
  id: string;
  msgType: string;
  priority: string;
  status: string;
  route: string;
  createdAt: string | null;
  sentAt: string | null;
  ackedAt: string | null;
}

export interface PushHealthRes {
  vapidReady: boolean;
  vapidSubject: string;
  activeSubscriptionCount: number;
  inactiveSubscriptionCount: number;
  latestSubscriptionUpdatedAt: string | null;
  latestSubscriptionPushAt: string | null;
  latestSubscriptionError: string;
  subscriptions: PushHealthSubscriptionRes[];
  latestDelivery: PushHealthDeliveryRes | null;
}

export interface PushHealthTestRes {
  message: string;
  delivery: PushHealthDeliveryRes | null;
}

export function getPushPublicKey(): Promise<PushPublicKeyRes> {
  return get<PushPublicKeyRes>(`${PUSH_BASE}/public-key`);
}

export function savePushSubscription(req: SavePushSubscriptionReq): Promise<boolean> {
  return post<boolean>(`${PUSH_BASE}/subscriptions`, req);
}

export function removePushSubscription(deviceId: string): Promise<boolean> {
  return del<boolean>(`${PUSH_BASE}/subscriptions/${encodeURIComponent(deviceId)}`);
}

export function getPushHealth(): Promise<PushHealthRes> {
  return get<PushHealthRes>(`${PUSH_BASE}/health`);
}

export function sendPushHealthTest(): Promise<PushHealthTestRes> {
  return post<PushHealthTestRes>(`${PUSH_BASE}/health/test`, {});
}
