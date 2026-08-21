"use client";

import { useStoreValue } from "@admin/shared/hooks/useStoreValue";
import { createStore } from "@admin/shared/lib/store";

export type SseStatus =
  "stopped" | "electing" | "follower" | "connecting" | "connected" | "reconnecting";

interface SseState {
  status: SseStatus;
  connected: boolean;
}

const sseStore = createStore<SseState>({ status: "stopped", connected: false });

export function setSseState(status: SseStatus, connected = false): void {
  sseStore.setState({ status, connected });
}

export function useSseStatus(): SseState {
  return useStoreValue(sseStore, (state) => state);
}
