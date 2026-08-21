"use client";

import { acquireAdminRuntime } from "@admin/runtime/admin-runtime";
import { useEffect } from "react";

export function AdminRuntimeBootstrap() {
  useEffect(() => acquireAdminRuntime(), []);
  return null;
}
