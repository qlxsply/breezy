"use client";

import type { Store } from "@admin/shared/lib/store";
import { useSyncExternalStore } from "react";

export function useStoreValue<T, Selected>(
  store: Store<T>,
  selector: (state: T) => Selected,
): Selected {
  return useSyncExternalStore(
    store.subscribe,
    () => selector(store.getState()),
    () => selector(store.getState()),
  );
}
