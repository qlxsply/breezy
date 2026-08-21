"use client";

import { endSession } from "@admin/runtime/session/session-service";
import { registerUnauthorizedHandler } from "@admin/shared/transport";
import { useEffect } from "react";

import { startSessionChannel } from "./session-channel";

export function SessionLifecycleBridge() {
  useEffect(() => {
    const unregisterUnauthorized = registerUnauthorizedHandler(() => endSession());
    const stopChannel = startSessionChannel(() => {
      void endSession({ redirectToLogin: true, broadcast: false });
    });
    return () => {
      unregisterUnauthorized();
      stopChannel();
    };
  }, []);
  return null;
}
