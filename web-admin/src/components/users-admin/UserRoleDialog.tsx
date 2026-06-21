import { useEffect, useMemo, useState } from "react";

import type { RoleEntry } from "../../types/role-admin";
import { BzButton } from "../bz/BzButton";
import { BzCheckbox } from "../bz/BzCheckbox";
import { BzDialog } from "../bz/BzDialog";
import { BzEmpty } from "../bz/BzEmpty";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzTag } from "../bz/BzTag";

interface UserRoleDialogProps {
  userName: string;
  roles: RoleEntry[];
  selectedIds: string[];
  loading?: boolean;
  canSave?: boolean;
  onClose: () => void;
  onSubmit: (ids: string[]) => void;
}

export function UserRoleDialog({
  userName,
  roles,
  selectedIds,
  loading = false,
  canSave = true,
  onClose,
  onSubmit,
}: UserRoleDialogProps) {
  const [keyword, setKeyword] = useState("");
  const [selectedSet, setSelectedSet] = useState<Set<string>>(new Set());

  useEffect(() => {
    setSelectedSet(new Set(selectedIds || []));
  }, [selectedIds]);

  const filtered = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    if (!kw) return roles;
    return roles.filter((role) => `${role.code} ${role.name}`.toLowerCase().includes(kw));
  }, [roles, keyword]);

  function toggle(id: string) {
    setSelectedSet((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleSubmit() {
    if (canSave === false) return;
    onSubmit(Array.from(selectedSet));
  }

  return (
    <BzDialog
      modelValue={true}
      title={`角色分配 - ${userName}`}
      width="720px"
      onClose={onClose}
      footer={
        <div style={{ display: "flex", gap: 8 }}>
          <BzButton onClick={onClose}>取消</BzButton>
          {canSave !== false ? (
            <BzButton
              buttonType="primary"
              onClick={handleSubmit}
            >
              保存
            </BzButton>
          ) : null}
        </div>
      }
    >
      <div className="toolbar">
        <BzInput
          modelValue={keyword}
          placeholder="搜索角色编码/名称"
          clearable
          onValueChange={setKeyword}
        />
        <div className="count">已选 {selectedSet.size} 项</div>
      </div>
      <BzLoading
        loading={loading}
        className="list"
      >
        {!loading && filtered.length === 0 ? (
          <BzEmpty description="暂无角色" />
        ) : (
          <div>
            {filtered.map((role) => (
              <div
                key={role.id}
                className="row"
              >
                {canSave !== false ? (
                  <BzCheckbox
                    modelValue={selectedSet.has(role.id)}
                    onChange={() => toggle(role.id)}
                  />
                ) : (
                  <span className="readonly-mark">{selectedSet.has(role.id) ? "已分配" : "-"}</span>
                )}
                <div className="meta">
                  <div className="name">{role.name}</div>
                  <div className="code">{role.code}</div>
                </div>
                <BzTag type={role.enabled ? "success" : "warning"}>
                  {role.enabled ? "启用" : "停用"}
                </BzTag>
              </div>
            ))}
          </div>
        )}
      </BzLoading>
    </BzDialog>
  );
}
