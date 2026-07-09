import { pageRoles } from "@admin/api/roles";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { useEffect, useMemo, useState } from "react";

import { formatDateTime } from "../../core/formatter";
import type { RoleEntry } from "../../types/role-admin";
import type { UserEntry, UserStatus } from "../../types/user-admin";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { BzTag } from "../bz/BzTag";

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
  const [roleRows, setRoleRows] = useState<RoleEntry[]>([]);
  const [rolePageNo, setRolePageNo] = useState(1);
  const [rolePageSize, setRolePageSize] = useState(10);
  const [roleTotal, setRoleTotal] = useState(0);
  const [rolePageLoading, setRolePageLoading] = useState(false);
  const [selectedSet, setSelectedSet] = useState<Set<string>>(new Set());
  const [err, setErr] = useState("");

  const currentUserType = model?.userType || "INTERNAL";
  const canShowRoles = canViewRoles && currentUserType !== "EXTERNAL";
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
    void pageRoles({
      keyword: appliedKeyword || undefined,
      enabled: true,
      page: { pageNo: rolePageNo, pageSize: rolePageSize },
    })
      .then((page) => {
        if (cancelled) return;
        setRoleRows(page.elements);
        setRoleTotal(page.totalElements);
        setRolePageNo(page.pageNo || rolePageNo);
        setRolePageSize(page.pageSize || rolePageSize);
      })
      .finally(() => {
        if (!cancelled) setRolePageLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [appliedKeyword, canShowRoles, open, rolePageNo, rolePageSize]);

  const currentPageSelectableIds = useMemo(() => roleRows.map((role) => role.id), [roleRows]);

  const currentPageAllSelected = useMemo(
    () =>
      currentPageSelectableIds.length > 0 &&
      currentPageSelectableIds.every((id) => selectedSet.has(id)),
    [currentPageSelectableIds, selectedSet],
  );

  const currentPageSomeSelected = useMemo(
    () => currentPageSelectableIds.some((id) => selectedSet.has(id)),
    [currentPageSelectableIds, selectedSet],
  );

  function toggleRole(id: string) {
    if (!canEditRoles) return;
    setSelectedSet((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

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

  function toggleCurrentPageAll(checked: boolean) {
    if (!canEditRoles) return;
    setSelectedSet((prev) => {
      const next = new Set(prev);
      if (checked) {
        currentPageSelectableIds.forEach((id) => next.add(id));
      } else {
        currentPageSelectableIds.forEach((id) => next.delete(id));
      }
      return next;
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
    <div className="permission-dialog-footer">
      <div className="permission-dialog-footer__summary" />
      <div className="permission-dialog-footer__actions">
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
    return <td className={`role-info-cell${options?.mono ? " mono" : ""}`}>{value}</td>;
  }

  function renderEditableTextCell(
    value: string,
    placeholder: string,
    onChange: (value: string) => void,
    options?: { mono?: boolean; password?: boolean },
  ) {
    return (
      <td className={`role-info-cell role-info-cell--edit${options?.mono ? " mono" : ""}`}>
        <BzInput
          modelValue={value}
          type={options?.password ? "password" : "text"}
          placeholder={placeholder}
          className={options?.mono ? "mono" : undefined}
          onValueChange={onChange}
        />
      </td>
    );
  }

  function renderStatusValue() {
    if (mode === "edit" && canEditBasic) {
      return (
        <td className="role-info-cell role-info-cell--edit">
          <BzSelect
            modelValue={status}
            onValueChange={(v) => setStatus((v as UserStatus) || "ENABLED")}
          >
            <BzOption
              label="启用"
              value="ENABLED"
            />
            <BzOption
              label="停用"
              value="DISABLED"
            />
          </BzSelect>
        </td>
      );
    }
    return (
      <td className="role-info-cell">
        <BzTag
          className={`role-info-status-tag${status === "ENABLED" ? " is-enabled" : " is-disabled"}`}
          type={status === "ENABLED" ? "success" : "danger"}
        >
          {status === "ENABLED" ? "启用" : "停用"}
        </BzTag>
      </td>
    );
  }

  return (
    <AdminEntityDrawer
      open={open}
      className="role-manage-drawer user-manage-drawer"
      title={mode === "create" ? "新增用户" : mode === "detail" ? "用户详情" : "编辑用户"}
      width="1180px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className="role-manage-shell">
        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">用户信息</div>
          </div>

          <div className="role-info-table-wrap">
            <table
              className="role-info-table"
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
                    <span className={editable ? "is-required" : undefined}>昵称</span>
                  </th>
                  {editable
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
                        className="role-info-status-tag is-enabled"
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
              className="form-error role-manage-error"
            />
          ) : null}
        </section>

        {canShowRoles ? (
          <section className="role-manage-section">
            <div className="role-manage-section__head">
              <div className="role-manage-section__title">角色分配</div>
              <div className="role-manage-section__stat">
                已选 {selectedSet.size} / {roleTotal} 项
              </div>
            </div>

            <div className="role-permission-toolbar">
              <BzInput
                modelValue={keyword}
                placeholder="搜索角色编码/名称"
                clearable
                className="role-permission-toolbar__search"
                onValueChange={setKeyword}
                onKeyUp={(event) => {
                  if (event.key === "Enter") applyRoleSearch();
                }}
              />
              <div className="role-permission-toolbar__actions">
                <BzButton
                  className="permission-toolbar-button"
                  onClick={resetRoleSearch}
                >
                  重置
                </BzButton>
                <BzButton
                  className="permission-toolbar-button"
                  onClick={applyRoleSearch}
                >
                  搜索
                </BzButton>
              </div>
            </div>

            <div className="admin-grid-table">
              <div className="admin-grid-table__viewport">
                <div
                  className="admin-grid-table__row admin-grid-table__row--head role-permission-table__head"
                  style={{ gridTemplateColumns: "44px minmax(240px, 1fr) minmax(240px, 1fr)" }}
                >
                  <div className="admin-grid-table__cell admin-grid-table__cell--check">
                    {mode === "detail" ? null : (
                      <input
                        className="admin-node-checkbox"
                        type="checkbox"
                        checked={currentPageAllSelected}
                        ref={(el) => {
                          if (el)
                            el.indeterminate = !currentPageAllSelected && currentPageSomeSelected;
                        }}
                        disabled={!canEditRoles || roleRows.length === 0}
                        onChange={(event) => toggleCurrentPageAll(event.target.checked)}
                      />
                    )}
                  </div>
                  <div className="admin-grid-table__cell">角色编码</div>
                  <div className="admin-grid-table__cell">角色名称</div>
                </div>

                <BzLoading
                  loading={rolePageLoading && roleRows.length > 0}
                  text="加载中..."
                >
                  <div className="admin-grid-table__body">
                    {roleRows.length === 0 ? (
                      <div className="permission-empty">
                        {rolePageLoading ? "加载中..." : "暂无角色"}
                      </div>
                    ) : (
                      roleRows.map((role) => (
                        <div
                          key={role.id}
                          className="admin-grid-table__row"
                          style={{
                            gridTemplateColumns: "44px minmax(240px, 1fr) minmax(240px, 1fr)",
                          }}
                        >
                          <div className="admin-grid-table__cell admin-grid-table__cell--check">
                            <input
                              className="admin-node-checkbox"
                              type="checkbox"
                              checked={selectedSet.has(role.id)}
                              disabled={!canEditRoles || mode === "detail"}
                              onChange={() => toggleRole(role.id)}
                            />
                          </div>
                          <div className="admin-grid-table__cell">
                            <span className="role-table-mono">{role.code}</span>
                          </div>
                          <div className="admin-grid-table__cell">
                            <span className="role-table-text">{role.name}</span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </BzLoading>
              </div>

              {roleTotal > 0 ? (
                <div className="dict-pagination-bar admin-grid-table__footer">
                  <div className="dict-pagination-summary">共 {roleTotal} 条记录</div>
                  <div className="dict-pagination-right">
                    <BzPagination
                      total={roleTotal}
                      pageSize={rolePageSize}
                      currentPage={rolePageNo}
                      showSizeChanger={false}
                      showFirstLast={false}
                      showJumper={false}
                      onCurrentChange={setRolePageNo}
                    />
                  </div>
                </div>
              ) : null}
            </div>
          </section>
        ) : null}
      </div>
    </AdminEntityDrawer>
  );
}
