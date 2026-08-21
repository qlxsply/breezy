import { ApisPage } from "@admin/features/apis/ApisPage";
import { ConfigsPage } from "@admin/features/configs/ConfigsPage";
import { DictsPage } from "@admin/features/dicts/DictsPage";
import { ResourcesPage } from "@admin/features/resources/management/ResourcesPage";
import { RolesPage } from "@admin/features/roles/RolesPage";
import { UsersPage } from "@admin/features/users/UsersPage";

import { AdminHelpPage } from "./AdminHelpPage";
import { AdminHomePage } from "./AdminHomePage";
import { AdminPlaceholderPage } from "./AdminPlaceholderPage";
import { AdminProfilePage } from "./AdminProfilePage";
import { AdminProfilePasswordPage } from "./AdminProfilePasswordPage";
import { AdminProfilePreferencesPage } from "./AdminProfilePreferencesPage";
import { AuditLogsPage } from "./AuditLogsPage";
import { DiagnosticPage } from "./DiagnosticPage";
import { LoginLogsPage } from "./LoginLogsPage";
import { MethodStatPage } from "./MethodStatPage";
import { SystemFilesPage } from "./SystemFilesPage";
import { UserFeatureApplicationsPage } from "./UserFeatureApplicationsPage";
import { UserFeaturePackagesPage } from "./UserFeaturePackagesPage";
import { WebUsersAdminPage } from "./WebUsersAdminPage";

type PageRenderer = () => React.ReactNode;

const pageMap = new Map<string, PageRenderer>([
  ["/admin", () => <AdminHomePage />],
  ["/admin/apis", () => <ApisPage />],
  ["/admin/audit-logs", () => <AuditLogsPage />],
  ["/admin/configs", () => <ConfigsPage />],
  ["/admin/diagnostic", () => <DiagnosticPage />],
  ["/admin/dicts", () => <DictsPage />],
  ["/admin/login-logs", () => <LoginLogsPage />],
  ["/admin/method-stat", () => <MethodStatPage />],
  ["/admin/resources", () => <ResourcesPage />],
  ["/admin/roles", () => <RolesPage />],
  ["/admin/system-files", () => <SystemFilesPage />],
  ["/admin/users", () => <UsersPage />],
  ["/admin/help", () => <AdminHelpPage />],
  ["/admin/profile", () => <AdminProfilePage />],
  ["/admin/profile/password", () => <AdminProfilePasswordPage />],
  ["/admin/profile/preferences", () => <AdminProfilePreferencesPage />],
  ["/admin/user-feature-applications", () => <UserFeatureApplicationsPage />],
  ["/admin/user-feature-packages", () => <UserFeaturePackagesPage />],
  ["/admin/web-users", () => <WebUsersAdminPage />],
  [
    "/admin/permission-policies",
    () => (
      <AdminPlaceholderPage
        title="权限策略"
        description="当前页面已预留，后续可以在这里补充对应功能。"
      />
    ),
  ],
]);

export function renderMappedAdminPage(pathname: string): React.ReactNode | null {
  const renderer = pageMap.get(pathname);
  return renderer ? renderer() : null;
}

export const KNOWN_ADMIN_SLUGS = [
  "apis",
  "audit-logs",
  "configs",
  "diagnostic",
  "dicts",
  "login-logs",
  "method-stat",
  "resources",
  "roles",
  "system-files",
  "users",
  "help",
  "profile",
  "profile/password",
  "profile/preferences",
  "user-feature-applications",
  "user-feature-packages",
  "web-users",
  "permission-policies",
];
