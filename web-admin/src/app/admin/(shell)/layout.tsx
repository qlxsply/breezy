import { AdminShell } from "@admin/components/admin-shell/AdminShell";
import { AdminRuntimeBootstrap } from "@admin/runtime/AdminRuntimeBootstrap";

export default function AdminShellLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <>
      <AdminRuntimeBootstrap />
      <AdminShell>{children}</AdminShell>
    </>
  );
}
