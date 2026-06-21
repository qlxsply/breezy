"use client";

import { useSyncExternalStore } from "react";

type Listener = () => void;

export interface Store<T> {
  getState: () => T;
  setState: (updater: T | ((current: T) => T)) => void;
  subscribe: (listener: Listener) => () => void;
}

export function createStore<T>(initialState: T): Store<T> {
  let state = initialState;
  const listeners = new Set<Listener>();

  return {
    getState: () => state,
    setState: (updater) => {
      state = typeof updater === "function" ? (updater as (current: T) => T)(state) : updater;
      listeners.forEach((listener) => listener());
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}

export function useStoreValue<T, Selected>(store: Store<T>, selector: (state: T) => Selected): Selected {
  return useSyncExternalStore(store.subscribe, () => selector(store.getState()), () => selector(store.getState()));
}
