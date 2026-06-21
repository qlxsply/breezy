import { ApisAdminPage } from "./ApisAdminPage";
import { AdminHelpPage } from "./AdminHelpPage";
import { AdminPlaceholderPage } from "./AdminPlaceholderPage";
import { AdminProfilePasswordPage } from "./AdminProfilePasswordPage";

type PageRenderer = () => React.ReactNode;

const pageMap = new Map<string, PageRenderer>([
  ["/admin/apis", () => <ApisAdminPage />],
  ["/admin/help", () => <AdminHelpPage />],
  ["/admin/profile/password", () => <AdminProfilePasswordPage />],
  [
    "/admin/permission-policies",
    () => <AdminPlaceholderPage title="权限策略" description="当前页面已预留，后续可以在这里补充对应功能。" />,
  ],
]);

export function renderMappedAdminPage(pathname: string): React.ReactNode | null {
  const renderer = pageMap.get(pathname);
  return renderer ? renderer() : null;
}
