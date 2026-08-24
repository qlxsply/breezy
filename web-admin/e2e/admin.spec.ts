import { expect, test } from "./fixtures/admin-api";

test("匿名访问后台跳转登录页", async ({ page, adminApi }) => {
  await adminApi.install({ authenticated: false });

  await page.goto("/admin/users/");

  await expect(page).toHaveURL(/\/admin\/login\?redirect=%2Fadmin%2Fusers/);
  await expect(page.getByRole("heading", { name: "后台登录" })).toBeVisible();
});

test("管理员登录后进入后台", async ({ page, adminApi }) => {
  await adminApi.install({ authenticated: false });
  await page.goto("/admin/login/");

  await page.getByPlaceholder("请输入账号").fill("admin");
  await page.getByPlaceholder("请输入密码").fill("password");
  await page.getByRole("button", { name: "登录", exact: true }).click();

  await expect(page).toHaveURL(/\/admin\/?$/);
  await expect(page.getByRole("button", { name: "Breezy Admin" })).toBeVisible();
  await expect(page.getByTitle("用户菜单")).toBeVisible();
});

test("已登录但无菜单权限显示 403 页面", async ({ page, adminApi }) => {
  await adminApi.install({ allowUsersMenu: false });

  await page.goto("/admin/users/");

  await expect(page.getByRole("heading", { name: "无权限访问当前页面" })).toBeVisible();
  await expect(page.getByText("提示 / 无权限访问")).toBeVisible();
});

test("用户列表发送筛选和分页请求", async ({ page, adminApi }) => {
  await adminApi.install();
  await page.goto("/admin/users/");
  await expect(page.getByText("alice", { exact: true })).toBeVisible();

  await page.getByTitle("打开搜索框").click();
  await page.getByPlaceholder("按账号或昵称搜索").fill("alice");
  await page.getByRole("button", { name: "搜索", exact: true }).click();
  await expect.poll(() => adminApi.userPageRequests.at(-1)?.usernameLike).toBe("alice");

  await page.getByPlaceholder("按账号或昵称搜索").fill("");
  await page.getByRole("button", { name: "搜索", exact: true }).click();
  await page.getByLabel("每页条数").selectOption("20");

  await expect
    .poll(() => adminApi.userPageRequests.at(-1)?.page)
    .toEqual({
      pageNo: 1,
      pageSize: 20,
    });
});

test("用户新增抽屉保存并刷新列表", async ({ page, adminApi }) => {
  await adminApi.install();
  await page.goto("/admin/users/");
  await expect(page.getByText("alice", { exact: true })).toBeVisible();
  const requestsBeforeCreate = adminApi.userPageRequests.length;

  await page.getByRole("button", { name: "新增", exact: true }).click();
  const drawer = page.getByText("新增用户", { exact: true }).locator("xpath=ancestor::section[1]");
  await expect(drawer).toBeVisible();
  await drawer.getByPlaceholder("请输入用户名").fill("charlie");
  await drawer.getByPlaceholder("请输入昵称").fill("Charlie");
  await drawer.getByPlaceholder("请输入初始密码").fill("StrongPass123!");
  await drawer.getByRole("button", { name: "保存", exact: true }).click();

  await expect.poll(() => adminApi.createUserRequests).toHaveLength(1);
  expect(adminApi.createUserRequests[0]).toMatchObject({
    username: "charlie",
    nickname: "Charlie",
    password: "StrongPass123!",
  });
  await expect.poll(() => adminApi.userPageRequests.length).toBeGreaterThan(requestsBeforeCreate);
  await expect(page.getByText("charlie", { exact: true })).toBeVisible();
  await expect(page.getByText("新增成功", { exact: true })).toBeVisible();
});
