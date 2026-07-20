"use client";

import {
  ensureAuthLoaded,
  getCurrentUser,
  getCurrentUserType,
} from "@admin/core/registry/auth-registry";
import { ensureRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import {
  bindNotificationRealtime,
  ensureUnreadLoaded,
} from "@admin/core/registry/notifications-registry";
import { initSseLifecycle } from "@admin/core/registry/sse-registry";
import { initTodoReminderPermission } from "@admin/core/registry/todo-reminder-registry";
import { useEffect } from "react";

export function AdminRuntimeBootstrap() {
  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      bindNotificationRealtime();
      await ensureAuthLoaded();
      if (cancelled) return;

      if (getCurrentUserType() === "ADMIN") {
        await ensureRegistryLoaded();
      }

      await ensureUnreadLoaded();
      initTodoReminderPermission();
      initSseLifecycle(() => getCurrentUser());
    };

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  return null;
}
