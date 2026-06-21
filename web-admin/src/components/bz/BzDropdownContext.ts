"use client";

import { createContext, useContext } from "react";

export interface BzDropdownContextValue {
  close: () => void;
}

export const BzDropdownContext = createContext<BzDropdownContextValue | null>(null);

export function useBzDropdownContext() {
  return useContext(BzDropdownContext);
}
