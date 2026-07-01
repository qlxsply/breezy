import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { useEffect, useMemo, useState } from "react";

import { formatDateTime } from "../../core/formatter";
import type { RoleEntry } from "../../types/role-admin";
import type { UserEntry, UserStatus } from "../../types/user-admin";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzCheckbox } from "../bz/BzCheckbox";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";

interface UserManageDrawerProps {
  open: boolean;
  mode: "detail" | "edit";
  model: UserEntry | null;
  roles: RoleEntry[];
  selectedIds: string[];
  loading?: boolean;
  canEditBasic?: boolean;
  canEditRoles?: boolean;
  canViewRoles?: boolean;
  userTypeMetaMap?: Record<string, { label: string; tagType?: string | null }>;
  onClose: () => void;
  onSubmit: (payload: { nickname: string; status: UserStatus; roleIds: string[] }) => void;
}

export function UserManageDrawer({
  open,
  mode,
  model,
  roles,
  selectedIds,
  loading = false,
  canEditBasic = false,
  canEditRoles = false,
  canViewRoles = false,
  userTypeMetaMap = {},
  onClose,
  onSubmit,
}: UserManageDrawerProps) {
  const [nickname, setNickname] = useState("");
  const [status, setStatus] = useState<UserStatus>("ENABLED");
  const [keyword, setKeyword] = useState("");
  const [selectedSet, setSelectedSet] = useState<Set<string>>(new Set());
  const [err, setErr] = useState("");

  useEffect(() => {
    if (!model) {
      return;
    }
    setNickname(model.nickname || "");
    setStatus(model.status);
    setKeyword("");
    setSelectedSet(new Set(selectedIds || []));
    setErr("");
  }, [model, selectedIds, open]);

  const canShowRoles = canViewRoles && model?.userType !== "EXTERNAL";
  const editable = mode === "edit" && (canEditBasic || canEditRoles);
  const resolveUserTypeLabel = (userType: string) => userTypeMetaMap[userType]?.label || userType;

  const filteredRoles = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    if (!kw) return roles;
    return roles.filter((role) => `${role.code} ${role.name}`.toLowerCase().includes(kw));
  }, [roles, keyword]);

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
    if (!model) return;
    if (canEditBasic && !nickname.trim()) {
      setErr("昵称不能为空");
      return;
    }
    onSubmit({
      nickname: nickname.trim(),
      status,
      roleIds: Array.from(selectedSet),
    });
  }

  const footer = (
    <div style={{ display: "flex", gap: 8 }}>
      <BzButton onClick={onClose}>{editable ? "取消" : "关闭"}</BzButton>
      {editable ? (
        <BzButton buttonType="primary" onClick={handleSubmit}>
          保存
        </BzButton>
      ) : null}
    </div>
  );

  return (
    <AdminEntityDrawer
      open={open}
      title={mode === "detail" ? "用户详情" : "编辑用户"}
      width="920px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className="user-manage-drawer">
        <section className="user-manage-panel">
          <div className="user-manage-panel__title">基础信息</div>
          <BzForm>
            <div className="user-manage-grid">
              <BzFormItem label="用户名">
                <BzInput modelValue={model?.username || ""} disabled readOnly />
              </BzFormItem>
              <BzFormItem label="用户类型">
                <BzInput modelValue={model?.userType ? resolveUserTypeLabel(model.userType) : "-"} disabled readOnly />
              </BzFormItem>
              <BzFormItem
                label={
                  canEditBasic && mode === "edit" ? (
                    <>
                      昵称<span className="form-required-mark">*</span>
                    </>
                  ) : (
                    "昵称"
                  )
                }
              >
                <BzInput
                  modelValue={nickname}
                  placeholder="请输入昵称"
                  disabled={!canEditBasic || mode === "detail"}
                  readOnly={!canEditBasic || mode === "detail"}
                  onValueChange={setNickname}
                />
              </BzFormItem>
              <BzFormItem
                label={
                  canEditBasic && mode === "edit" ? (
                    <>
                      状态<span className="form-required-mark">*</span>
                    </>
                  ) : (
                    "状态"
                  )
                }
              >
                {mode === "edit" && canEditBasic ? (
                  <BzSelect
                    modelValue={status}
                    disabled={!canEditBasic}
                    onValueChange={(v) => setStatus((v as UserStatus) || "ENABLED")}
                  >
                    <BzOption label="启用" value="ENABLED" />
                    <BzOption label="停用" value="DISABLED" />
                  </BzSelect>
                ) : (
                  <BzInput modelValue={status === "ENABLED" ? "启用" : "停用"} disabled readOnly />
                )}
              </BzFormItem>
              <BzFormItem label="创建人">
                <BzInput modelValue={model?.createdBy || "-"} disabled readOnly />
              </BzFormItem>
              <BzFormItem label="创建时间">
                <BzInput modelValue={formatDateTime(model?.createdAt)} disabled readOnly />
              </BzFormItem>
              <BzFormItem label="更新人">
                <BzInput modelValue={model?.updatedBy || "-"} disabled readOnly />
              </BzFormItem>
              <BzFormItem label="更新时间">
                <BzInput modelValue={formatDateTime(model?.updatedAt)} disabled readOnly />
              </BzFormItem>
            </div>
          </BzForm>
        </section>

        {err ? <BzAlert title={err} type="error" showIcon className="form-error" /> : null}

        {canShowRoles ? (
          <section className="user-manage-panel">
            <div className="user-manage-panel__header">
              <div className="user-manage-panel__title">角色分配</div>
              <div className="user-manage-panel__meta">已选 {selectedSet.size} 项</div>
            </div>

            <div className="user-role-toolbar">
              <BzInput
                modelValue={keyword}
                placeholder="搜索角色编码/名称"
                clearable
                onValueChange={setKeyword}
              />
            </div>

            <BzLoading loading={loading} className="user-role-list-wrap">
              {!loading && filteredRoles.length === 0 ? (
                <BzEmpty description="暂无角色" />
              ) : (
                <div className="user-role-list">
                  {filteredRoles.map((role) => (
                    <div key={role.id} className={`user-role-row${selectedSet.has(role.id) ? " is-selected" : ""}`}>
                      <BzCheckbox
                        modelValue={selectedSet.has(role.id)}
                        disabled={!canEditRoles || mode === "detail"}
                        onValueChange={() => toggleRole(role.id)}
                      />
                      <div className="user-role-row__code">{role.code}</div>
                      <div className="user-role-row__name">{role.name}</div>
                    </div>
                  ))}
                </div>
              )}
            </BzLoading>
          </section>
        ) : null}
      </div>

      <style jsx>{`
        .user-manage-drawer {
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .user-manage-panel {
          border: 1px solid #e2e8f0;
          border-radius: 16px;
          background: #fff;
          padding: 18px;
        }

        .user-manage-panel__header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 16px;
          margin-bottom: 12px;
        }

        .user-manage-panel__title {
          color: #0f172a;
          font-size: 16px;
          font-weight: 700;
          margin-bottom: 8px;
        }

        .user-manage-panel__meta {
          color: #64748b;
          font-size: 13px;
          line-height: 1.6;
        }

        .user-manage-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px 16px;
        }

        .user-role-toolbar {
          margin-bottom: 12px;
        }

        .user-role-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .user-role-row {
          display: grid;
          grid-template-columns: 28px 180px minmax(0, 1fr);
          align-items: center;
          gap: 12px;
          padding: 10px 4px;
          border-bottom: 1px solid #e5e7eb;
        }

        .user-role-row.is-selected {
          background: #eff6ff;
          border-radius: 8px;
        }

        .user-role-row__name {
          color: #0f172a;
          font-weight: 600;
          min-width: 0;
        }

        .user-role-row__code {
          color: #64748b;
          font-size: 12px;
          font-family: Consolas, "Courier New", monospace;
        }

        @media (max-width: 900px) {
          .user-manage-grid {
            grid-template-columns: 1fr;
          }

          .user-manage-panel__header {
            flex-direction: column;
          }

          .user-role-row {
            grid-template-columns: 28px minmax(0, 1fr);
          }

          .user-role-row__name {
            grid-column: 2;
          }

          .user-role-row__code {
            grid-column: 2;
          }
        }
      `}</style>
    </AdminEntityDrawer>
  );
}
