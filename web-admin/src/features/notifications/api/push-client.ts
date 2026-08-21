import { del, get, post } from "@admin/shared/transport";

interface PushPublicKeyResponse {
  publicKey: string;
}

interface SavePushSubscriptionRequest {
  deviceId: string;
  endpoint: string;
  p256dh: string;
  auth: string;
}

export function getPushPublicKey(): Promise<PushPublicKeyResponse> {
  return get<PushPublicKeyResponse>("/push/public-key", NON_BLOCKING_SESSION_TERMINATION);
}

export function savePushSubscription(request: SavePushSubscriptionRequest): Promise<boolean> {
  return post<boolean>("/push/subscriptions", request, NON_BLOCKING_SESSION_TERMINATION);
}

export function removePushSubscription(deviceId: string): Promise<boolean> {
  return del<boolean>(
    `/push/subscriptions/${encodeURIComponent(deviceId)}`,
    undefined,
    NON_BLOCKING_SESSION_TERMINATION,
  );
}

const NON_BLOCKING_SESSION_TERMINATION = { waitForSessionTermination: false } as const;
