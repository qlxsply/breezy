import { AdminRuntimeBootstrap } from "@admin/runtime/AdminRuntimeBootstrap";

import { AdminAccessBoundary } from "./_components/AdminAccessBoundary";
import { AdminResourceBoundary } from "./_components/AdminResourceBoundary";
import { AdminShell } from "./_components/AdminShell";

export default function ProtectedAdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <AdminRuntimeBootstrap />
      <AdminAccessBoundary>
        <AdminResourceBoundary>
          <AdminShell>{children}</AdminShell>
        </AdminResourceBoundary>
      </AdminAccessBoundary>
    </>
  );
}
