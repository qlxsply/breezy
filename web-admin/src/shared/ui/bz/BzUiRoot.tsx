"use client";

import { BzConfirmHost } from "./BzConfirmHost";
import { BzMessageHost } from "./BzMessageHost";
import { BzUiProvider } from "./store";

export function BzUiRoot({ children }: { children: React.ReactNode }) {
  return (
    <BzUiProvider>
      {children}
      <BzMessageHost />
      <BzConfirmHost />
    </BzUiProvider>
  );
}
