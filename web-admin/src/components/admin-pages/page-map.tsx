import { AdminHelpPage } from "./AdminHelpPage";
import { AdminHomePage } from "./AdminHomePage";
import { AdminPlaceholderPage } from "./AdminPlaceholderPage";
import { AdminProfilePage } from "./AdminProfilePage";
import { AdminProfilePasswordPage } from "./AdminProfilePasswordPage";
import { AdminProfilePreferencesPage } from "./AdminProfilePreferencesPage";
import { ApisAdminPage } from "./ApisAdminPage";
import { AuditLogsPage } from "./AuditLogsPage";
import { ConfigsAdminPage } from "./ConfigsAdminPage";
import { DiagnosticPage } from "./DiagnosticPage";
import { DictAdminPage } from "./DictAdminPage";
import { LoginLogsPage } from "./LoginLogsPage";
import { MethodStatPage } from "./MethodStatPage";
import { ResourcesAdminPage } from "./ResourcesAdminPage";
import { RolesAdminPage } from "./RolesAdminPage";
import { SystemFilesPage } from "./SystemFilesPage";
import { UserFeatureApplicationsPage } from "./UserFeatureApplicationsPage";
import { UserFeaturePackagesPage } from "./UserFeaturePackagesPage";
import { UsersAdminPage } from "./UsersAdminPage";
import { WebUsersAdminPage } from "./WebUsersAdminPage";

type PageRenderer = () => React.ReactNode;

const pageMap = new Map<string, PageRenderer>([
  ["/admin", () => <AdminHomePage />],
  ["/admin/apis", () => <ApisAdminPage />],
  ["/admin/audit-logs", () => <AuditLogsPage />],
  ["/admin/configs", () => <ConfigsAdminPage />],
  ["/admin/diagnostic", () => <DiagnosticPage />],
  ["/admin/dicts", () => <DictAdminPage />],
  ["/admin/login-logs", () => <LoginLogsPage />],
  ["/admin/method-stat", () => <MethodStatPage />],
  ["/admin/resources", () => <ResourcesAdminPage />],
  ["/admin/roles", () => <RolesAdminPage />],
  ["/admin/system-files", () => <SystemFilesPage />],
  ["/admin/users", () => <UsersAdminPage />],
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
