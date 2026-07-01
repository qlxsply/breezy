import { useEffect, useState } from "react";

import type { RoleEntry } from "../../types/role-admin";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzDialog } from "../bz/BzDialog";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzSwitch } from "../bz/BzSwitch";

interface RoleFormDialogProps {
  mode: "create" | "edit";
  model: RoleEntry | null;
  onClose: () => void;
  onSubmit: (model: RoleEntry) => void;
}

const defaultModel: RoleEntry = { id: "", code: "", name: "", enabled: true };

export function RoleFormDialog({ mode, model, onClose, onSubmit }: RoleFormDialogProps) {
  const [form, setForm] = useState<RoleEntry>(defaultModel);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (model) setForm({ ...model });
  }, [model]);

  function validate(): string {
    if (!form.code.trim()) return "编码不能为空";
    if (!form.name.trim()) return "名称不能为空";
    if (!/^[a-zA-Z0-9_-]+$/.test(form.code.trim())) return "编码建议仅包含字母/数字/_/-";
    return "";
  }

  function handleSubmit() {
    setErr("");
    const e = validate();
    if (e) {
      setErr(e);
      return;
    }
    onSubmit({ ...form });
  }

  return (
    <BzDialog
      modelValue={true}
      title={mode === "create" ? "新增角色" : "编辑角色"}
      width="520px"
      onClose={onClose}
      footer={
        <div className="role-form-footer">
          <BzButton onClick={onClose}>取消</BzButton>
          <BzButton
            buttonType="primary"
            onClick={handleSubmit}
          >
            保存
          </BzButton>
        </div>
      }
    >
      <div className="role-form-shell">
        <BzForm>
          <div className="role-form-grid">
            <BzFormItem label="编码 *">
              <BzInput
                modelValue={form.code}
                placeholder="请输入角色编码"
                className="mono"
                onValueChange={(v) => setForm((prev) => ({ ...prev, code: v }))}
              />
            </BzFormItem>
            <BzFormItem label="名称 *">
              <BzInput
                modelValue={form.name}
                placeholder="请输入角色名称"
                onValueChange={(v) => setForm((prev) => ({ ...prev, name: v }))}
              />
            </BzFormItem>
          </div>
          <BzFormItem label="状态">
            <BzSwitch
              modelValue={form.enabled}
              activeText="启用"
              inactiveText="停用"
              onValueChange={(v) => setForm((prev) => ({ ...prev, enabled: v }))}
            />
          </BzFormItem>
        </BzForm>
      </div>
      {err ? (
        <BzAlert
          title={err}
          type="error"
          showIcon
          className="form-error"
        />
      ) : null}
    </BzDialog>
  );
}
