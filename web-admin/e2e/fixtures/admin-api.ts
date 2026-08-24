import { expect, type Page, type Route, test as base } from "@playwright/test";

export interface UserPageRequest {
  usernameLike?: string;
  status?: string;
  page?: { pageNo?: number; pageSize?: number };
}

export interface CreateUserRequest {
  username: string;
  nickname: string;
  password: string;
  roleIds?: number[];
}

interface MockUser {
  id: string;
  username: string;
  nickname: string;
  userType: "ADMIN";
  status: "ENABLED" | "DISABLED";
  createdBy: string;
  createdAt: string;
  updatedBy: string;
  updatedAt: string;
}

interface AdminApiMockOptions {
  authenticated?: boolean;
  allowUsersMenu?: boolean;
}

export interface AdminApiMock {
  userPageRequests: UserPageRequest[];
  createUserRequests: CreateUserRequest[];
  unexpectedRequests: string[];
  install(options?: AdminApiMockOptions): Promise<void>;
}

interface Fixtures {
  adminApi: AdminApiMock;
}

const ADMIN_USER = {
  id: "1",
  account: "admin",
  userType: "ADMIN",
  mustChangePassword: false,
  configs: [],
};

const USER_PERMISSIONS = [
  "user-manage-view",
  "user-manage-create",
  "user-manage-edit",
  "user-manage-reset-password",
  "user-manage-batch-disable",
  "user-manage-batch-reset-password",
  "user-manage-batch-delete",
  "user-manage-role-edit",
  "user-manage-role-view",
  "user-manage-delete",
];

const INITIAL_USERS: MockUser[] = [
  mockUser("1", "admin", "系统管理员"),
  mockUser("2", "alice", "Alice"),
  mockUser("3", "bob", "Bob", "DISABLED"),
];

export const test = base.extend<Fixtures>({
  adminApi: async ({ page }, use) => {
    const mock = createAdminApiMock(page);
    await use(mock);
    expect(mock.unexpectedRequests).toEqual([]);
  },
});

export { expect };

function createAdminApiMock(page: Page): AdminApiMock {
  const userPageRequests: UserPageRequest[] = [];
  const createUserRequests: CreateUserRequest[] = [];
  const unexpectedRequests: string[] = [];

  return {
    userPageRequests,
    createUserRequests,
    unexpectedRequests,
    async install(options = {}) {
      let authenticated = options.authenticated ?? true;
      const users = INITIAL_USERS.map((user) => ({ ...user }));
      await page.route("**/api/**", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        const path = url.pathname.replace(/^\/api/, "");
        const method = request.method();

        if (path === "/admin/auth/csrf" && method === "GET") {
          await fulfill(route, true, {
            "set-cookie": "__Host-breezy-admin-csrf=e2e-csrf; Path=/; Secure; SameSite=Strict",
          });
          return;
        }
        if (path === "/admin/auth/me" && method === "GET") {
          if (authenticated) await fulfill(route, ADMIN_USER);
          else await fulfillError(route, 401, "UNAUTHENTICATED", "未登录");
          return;
        }
        if (path === "/admin/auth/login" && method === "POST") {
          authenticated = true;
          await fulfill(route, { user: ADMIN_USER, sessionExpiresAt: null });
          return;
        }
        if (path === "/admin/menu-resources" && method === "GET") {
          await fulfill(route, { resources: resources(options.allowUsersMenu ?? true) });
          return;
        }
        if (path === "/notifications/page" && method === "POST") {
          await fulfill(route, pageResult([], 0, 1, 100));
          return;
        }
        if (path === "/public/dicts/USER_TYPE/items" && method === "GET") {
          await fulfill(route, [
            { itemCode: "ADMIN", itemLabel: "账号", itemValue: "ADMIN", tagType: "info" },
          ]);
          return;
        }
        if (path === "/roles/page" && method === "POST") {
          await fulfill(
            route,
            pageResult([{ id: 10, code: "ADMIN", name: "管理员", enabled: true }], 1, 1, 10),
          );
          return;
        }
        if (path === "/users/page" && method === "POST") {
          const body = request.postDataJSON() as UserPageRequest;
          userPageRequests.push(body);
          const keyword = body.usernameLike?.toLowerCase() ?? "";
          const filtered = users.filter(
            (user) =>
              (!keyword ||
                user.username.toLowerCase().includes(keyword) ||
                user.nickname.toLowerCase().includes(keyword)) &&
              (!body.status || user.status === body.status),
          );
          const pageNo = body.page?.pageNo ?? 1;
          const pageSize = body.page?.pageSize ?? 10;
          const start = (pageNo - 1) * pageSize;
          await fulfill(
            route,
            pageResult(filtered.slice(start, start + pageSize), filtered.length, pageNo, pageSize),
          );
          return;
        }
        if (path === "/users" && method === "POST") {
          const body = request.postDataJSON() as CreateUserRequest;
          createUserRequests.push(body);
          const created = mockUser(String(users.length + 1), body.username, body.nickname);
          users.push(created);
          await fulfill(route, created);
          return;
        }
        if (path === "/sse/ticket" && method === "POST") {
          await fulfill(route, { ticket: "e2e-ticket", expiresAtEpochMillis: Date.now() + 60_000 });
          return;
        }
        if (path === "/sse/stream" && method === "GET") {
          await route.fulfill({
            status: 200,
            contentType: "text/event-stream",
            body: ": e2e stream\n\n",
          });
          return;
        }

        unexpectedRequests.push(`${method} ${path}`);
        await fulfillError(route, 501, "UNEXPECTED_E2E_REQUEST", "E2E Mock 未声明该接口");
      });
    },
  };
}

function resources(allowUsersMenu: boolean): object[] {
  const root = {
    id: "root",
    parentId: null,
    type: "DIRECTORY",
    code: "system",
    name: "系统管理",
    openMode: "NONE",
    enabled: true,
  };
  const home = {
    id: "home",
    parentId: null,
    type: "MENU",
    code: "admin-home",
    name: "工作台",
    url: "/admin",
    openMode: "PAGE",
    enabled: true,
  };
  if (!allowUsersMenu) return [home];
  return [
    home,
    root,
    {
      id: "users-menu",
      parentId: "root",
      type: "MENU",
      code: "users-menu",
      name: "用户管理",
      url: "/admin/users",
      openMode: "PAGE",
      enabled: true,
    },
    ...USER_PERMISSIONS.map((code, index) => ({
      id: `users-permission-${index}`,
      parentId: "users-menu",
      type: "BUTTON",
      code,
      name: code,
      openMode: "NONE",
      enabled: true,
    })),
  ];
}

function mockUser(
  id: string,
  username: string,
  nickname: string,
  status: "ENABLED" | "DISABLED" = "ENABLED",
): MockUser {
  return {
    id,
    username,
    nickname,
    userType: "ADMIN",
    status,
    createdBy: "admin",
    createdAt: "2026-08-24T08:00:00Z",
    updatedBy: "admin",
    updatedAt: "2026-08-24T08:00:00Z",
  };
}

function pageResult<T>(elements: T[], total: number, pageNo: number, pageSize: number) {
  return {
    pageNo,
    pageSize,
    numberOfElements: elements.length,
    totalPages: Math.max(1, Math.ceil(total / pageSize)),
    totalElements: total,
    elements,
  };
}

async function fulfill(route: Route, data: unknown, headers: Record<string, string> = {}) {
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    headers,
    body: JSON.stringify({
      success: true,
      code: "SUCCESS",
      msg: "success",
      timestamp: new Date().toISOString(),
      data,
    }),
  });
}

async function fulfillError(route: Route, status: number, code: string, msg: string) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify({
      success: false,
      code,
      msg,
      timestamp: new Date().toISOString(),
      data: null,
    }),
  });
}
