import { pageAssignableRoles } from "@admin/features/users/api/client";
import type { AssignableRole, UserEntry, UserStatus } from "@admin/features/users/model/types";
import { formatDateTime } from "@admin/shared/lib/formatter";
import { AdminDrawerPagedSelection } from "@admin/shared/ui/admin/AdminDrawerPagedSelection";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminInfoCell } from "@admin/shared/ui/admin/AdminInfoCell";
import { TableInput, TableSelect } from "@admin/shared/ui/admin/inputs";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { useEffect, useState } from "react";

import tableStyles from "./UserTable.module.css";

interface UserManageDrawerProps {
  open: boolean;
  mode: "create" | "detail" | "edit";
  model: UserEntry | null;
  selectedIds: string[];
  loading?: boolean;
  canEditBasic?: boolean;
  canEditRoles?: boolean;
  canViewRoles?: boolean;
  userTypeMetaMap?: Record<string, { label: string; tagType?: string | null }>;
  onClose: () => void;
  onSubmit: (payload: {
    username: string;
    nickname: string;
    password?: string;
    status: UserStatus;
    roleIds: string[];
  }) => void;
}

export function UserManageDrawer({
  open,
  mode,
  model,
  selectedIds,
  loading = false,
  canEditBasic = false,
  canEditRoles = false,
  canViewRoles = false,
  userTypeMetaMap = {},
  onClose,
  onSubmit,
}: UserManageDrawerProps) {
  const [username, setUsername] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<UserStatus>("ENABLED");
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [roleRows, setRoleRows] = useState<AssignableRole[]>([]);
  const [rolePageNo, setRolePageNo] = useState(1);
  const [rolePageSize, setRolePageSize] = useState(10);
  const [roleTotal, setRoleTotal] = useState(0);
  const [rolePageLoading, setRolePageLoading] = useState(false);
  const [selectedSet, setSelectedSet] = useState<Set<string>>(new Set());
  const [err, setErr] = useState("");

  const currentUserType = model?.userType || "ADMIN";
  const canShowRoles = canViewRoles && currentUserType !== "USER";
  const basicEditable = mode !== "detail" && canEditBasic;
  const editable = mode !== "detail" && (canEditBasic || canEditRoles);
  const resolveUserTypeLabel = (userType: string) => userTypeMetaMap[userType]?.label || userType;

  useEffect(() => {
    if (!model && mode !== "create") {
      return;
    }
    setUsername(model?.username || "");
    setNickname(model?.nickname || "");
    setPassword("");
    setStatus(model?.status || "ENABLED");
    setKeyword("");
    setSelectedSet(new Set(selectedIds || []));
    setErr("");
  }, [mode, model, selectedIds, open]);

  useEffect(() => {
    if (!open || !canShowRoles) return;
    let cancelled = false;
    setRolePageLoading(true);
    const controller = new AbortController();
    void pageAssignableRoles(
      {
        keyword: appliedKeyword || undefined,
        page: { pageNo: rolePageNo, pageSize: rolePageSize },
      },
      { signal: controller.signal },
    )
      .then((page) => {
        if (cancelled) return;
        setRoleRows(page.elements);
        setRoleTotal(page.totalElements);
        setRolePageNo(page.pageNo || rolePageNo);
        setRolePageSize(page.pageSize || rolePageSize);
      })
      .catch((cause: unknown) => {
        if (cancelled || isAbortError(cause)) return;
        setRoleRows([]);
        setRoleTotal(0);
        setErr(cause instanceof Error ? cause.message : "角色列表加载失败");
      })
      .finally(() => {
        if (!cancelled) setRolePageLoading(false);
      });
    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [appliedKeyword, canShowRoles, open, rolePageNo, rolePageSize]);

  function handleSubmit() {
    setErr("");
    if (mode === "create" && !username.trim()) {
      setErr("用户名不能为空");
      return;
    }
    if (canEditBasic && !nickname.trim()) {
      setErr("昵称不能为空");
      return;
    }
    if (mode === "create" && !password.trim()) {
      setErr("初始密码不能为空");
      return;
    }
    onSubmit({
      username: username.trim(),
      nickname: nickname.trim(),
      password: mode === "create" ? password.trim() : undefined,
      status,
      roleIds: Array.from(selectedSet),
    });
  }

  function applyRoleSearch() {
    setAppliedKeyword(keyword.trim());
    setRolePageNo(1);
  }

  function resetRoleSearch() {
    setKeyword("");
    setAppliedKeyword("");
    setRolePageNo(1);
  }

  const footer = (
    <div className="admin-drawer-footer">
      <div className="admin-drawer-footer__summary" />
      <div className="admin-drawer-footer__actions">
        <BzButton onClick={onClose}>{editable ? "取消" : "关闭"}</BzButton>
        {editable ? (
          <BzButton
            buttonType="primary"
            onClick={handleSubmit}
          >
            保存
          </BzButton>
        ) : null}
      </div>
    </div>
  );

  function renderValueCell(value: React.ReactNode, options?: { mono?: boolean }) {
    return (
      <AdminInfoCell
        state={mode === "detail" ? "display" : "readonly"}
        mono={options?.mono}
      >
        {value}
      </AdminInfoCell>
    );
  }

  function renderEditableTextCell(
    value: string,
    placeholder: string,
    onChange: (value: string) => void,
    options?: { mono?: boolean; password?: boolean },
  ) {
    return (
      <AdminInfoCell
        state="editable"
        mono={options?.mono}
      >
        <TableInput
          value={value}
          type={options?.password ? "password" : "text"}
          placeholder={placeholder}
          className={options?.mono ? "mono" : undefined}
          onValueChange={onChange}
        />
      </AdminInfoCell>
    );
  }

  function renderStatusValue() {
    if (mode === "edit" && basicEditable) {
      return (
        <AdminInfoCell state="editable">
          <TableSelect
            value={status}
            options={[
              { label: "启用", value: "ENABLED" },
              { label: "停用", value: "DISABLED" },
            ]}
            allowClear={false}
            onValueChange={(value) =>
              setStatus(Array.isArray(value) ? "ENABLED" : (String(value) as UserStatus))
            }
          />
        </AdminInfoCell>
      );
    }
    return renderValueCell(
      <BzTag
        className={`admin-info-status-tag${status === "ENABLED" ? " is-enabled" : " is-disabled"}`}
        type={status === "ENABLED" ? "success" : "danger"}
      >
        {status === "ENABLED" ? "启用" : "停用"}
      </BzTag>,
    );
  }

  return (
    <AdminEntityDrawer
      open={open}
      className="admin-entity-manage-drawer user-manage-drawer"
      title={mode === "create" ? "新增用户" : mode === "detail" ? "用户详情" : "编辑用户"}
      width="1180px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className="admin-entity-shell">
        <section className="admin-entity-section">
          <div className="admin-entity-section__head">
            <div className="admin-entity-section__title">用户信息</div>
          </div>

          <div className="admin-info-table-wrap">
            <table
              className="admin-info-table"
              aria-label="用户信息"
            >
              <tbody>
                <tr>
                  <th>
                    <span className={mode === "create" ? "is-required" : undefined}>用户名</span>
                  </th>
                  {mode === "create"
                    ? renderEditableTextCell(username, "请输入用户名", setUsername, { mono: true })
                    : renderValueCell(username || "-", { mono: true })}
                  <th>用户类型</th>
                  {renderValueCell(
                    <BzTag size="small">{resolveUserTypeLabel(currentUserType)}</BzTag>,
                  )}
                  <th>
                    <span className={basicEditable ? "is-required" : undefined}>昵称</span>
                  </th>
                  {basicEditable
                    ? renderEditableTextCell(nickname, "请输入昵称", setNickname)
                    : renderValueCell(nickname || "-")}
                </tr>

                {mode === "create" ? (
                  <tr>
                    <th>
                      <span className="is-required">初始密码</span>
                    </th>
                    {renderEditableTextCell(password, "请输入初始密码", setPassword, {
                      password: true,
                    })}
                    <th>状态</th>
                    {renderValueCell(
                      <BzTag
                        className="admin-info-status-tag is-enabled"
                        type="success"
                      >
                        启用
                      </BzTag>,
                    )}
                    <th>角色概览</th>
                    {renderValueCell(`已选 ${selectedSet.size} 项`, { mono: true })}
                  </tr>
                ) : (
                  <>
                    <tr>
                      <th>
                        <span
                          className={mode === "edit" && canEditBasic ? "is-required" : undefined}
                        >
                          状态
                        </span>
                      </th>
                      {renderStatusValue()}
                      <th>创建人</th>
                      {renderValueCell(model?.createdBy || "-", { mono: true })}
                      <th>创建时间</th>
                      {renderValueCell(formatDateTime(model?.createdAt) || "-", { mono: true })}
                    </tr>
                    <tr>
                      <th>更新人</th>
                      {renderValueCell(model?.updatedBy || "-", { mono: true })}
                      <th>更新时间</th>
                      {renderValueCell(formatDateTime(model?.updatedAt) || "-", { mono: true })}
                      <th>角色概览</th>
                      {renderValueCell(`已选 ${selectedSet.size} 项`, { mono: true })}
                    </tr>
                  </>
                )}
              </tbody>
            </table>
          </div>

          {err ? (
            <BzAlert
              title={err}
              type="error"
              showIcon
              className="form-error admin-entity-error"
            />
          ) : null}
        </section>

        {canShowRoles ? (
          <AdminDrawerPagedSelection
            title="角色分配"
            rows={roleRows}
            rowKey={(role) => role.id}
            columns={[
              {
                key: "code",
                title: "角色编码",
                render: (role) => <span className={tableStyles.mono}>{role.code}</span>,
              },
              {
                key: "name",
                title: "角色名称",
                render: (role) => <span className={tableStyles.text}>{role.name}</span>,
              },
            ]}
            gridTemplateColumns="minmax(240px, 1fr) minmax(240px, 1fr)"
            selectedKeys={Array.from(selectedSet)}
            total={roleTotal}
            pageNo={rolePageNo}
            pageSize={rolePageSize}
            keyword={keyword}
            loading={rolePageLoading}
            editable={mode !== "detail" && canEditRoles}
            searchPlaceholder="搜索角色编码/名称"
            emptyText="暂无角色"
            onKeywordChange={setKeyword}
            onSearch={applyRoleSearch}
            onReset={resetRoleSearch}
            onPageChange={setRolePageNo}
            onSelectedKeysChange={(keys) => setSelectedSet(new Set(keys))}
          />
        ) : null}
      </div>
    </AdminEntityDrawer>
  );
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof Error && cause.name === "AbortError";
}
