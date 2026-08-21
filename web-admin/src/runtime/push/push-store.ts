"use client";

import { useStoreValue } from "@admin/shared/hooks/useStoreValue";
import { createStore } from "@admin/shared/lib/store";

export type PushPermission = NotificationPermission | "unsupported";
export type PushStatus = "stopped" | "starting" | "ready" | "unavailable" | "error";

interface PushState {
  status: PushStatus;
  permission: PushPermission;
  serviceWorkerScope: string;
  error: string | null;
}

const pushStore = createStore<PushState>({
  status: "stopped",
  permission: "unsupported",
  serviceWorkerScope: "",
  error: null,
});

export function setPushState(state: PushState): void {
  pushStore.setState(state);
}

export function usePushState(): PushState {
  return useStoreValue(pushStore, (state) => state);
}
