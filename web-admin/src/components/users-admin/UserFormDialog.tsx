import { useEffect, useState } from "react";

import type { UserStatus } from "../../types/user-admin";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzDialog } from "../bz/BzDialog";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";

interface UserFormDialogProps {
  mode: "create" | "edit";
  model: { id: string; username: string; nickname: string; userType: string; status: UserStatus } | null;
  onClose: () => void;
  onSubmit: (payload: { username: string; nickname: string; password?: string; status: UserStatus }) => void;
}

export function UserFormDialog({ mode, model, onClose, onSubmit }: UserFormDialogProps) {
  const [username, setUsername] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<UserStatus>("ENABLED");
  const [err, setErr] = useState("");

  useEffect(() => {
    if (model) {
      setUsername(model.username);
      setNickname(model.nickname || "");
      setPassword("");
      setStatus(model.status);
    }
  }, [model]);

  function validate(): string {
    if (!username.trim()) return "用户名不能为空";
    if (!nickname.trim()) return "昵称不能为空";
    if (mode === "create" && !password.trim()) return "密码不能为空";
    return "";
  }

  function handleSubmit() {
    setErr("");
    const e = validate();
    if (e) { setErr(e); return; }
    const payload: { username: string; nickname: string; password?: string; status: UserStatus } = {
      username: username.trim(), nickname: nickname.trim(), status,
    };
    if (mode === "create") payload.password = password.trim();
    onSubmit(payload);
  }

  return (
    <BzDialog modelValue={true} title={mode === "create" ? "新增用户" : "编辑用户"} width="520px" onClose={onClose}
      footer={<div style={{ display: "flex", gap: 8 }}><BzButton onClick={onClose}>取消</BzButton><BzButton buttonType="primary" onClick={handleSubmit}>{mode === "create" ? "创建" : "保存"}</BzButton></div>}>
      <BzForm>
        <BzFormItem label="用户名 *">
          <BzInput modelValue={username} disabled={mode === "edit"} placeholder="请输入用户名" onValueChange={setUsername} />
        </BzFormItem>
        <BzFormItem label="昵称 *">
          <BzInput modelValue={nickname} placeholder="请输入昵称" onValueChange={setNickname} />
        </BzFormItem>
        {mode === "create" ? (
          <BzFormItem label="密码 *">
            <BzInput modelValue={password} type="password" placeholder="请输入初始密码" onValueChange={setPassword} />
          </BzFormItem>
        ) : null}
        {mode === "edit" ? (
          <BzFormItem label="状态 *">
            <BzSelect modelValue={status} onValueChange={(v) => setStatus((v as UserStatus) || "ENABLED")}>
              <BzOption label="启用" value="ENABLED" />
              <BzOption label="停用" value="DISABLED" />
            </BzSelect>
          </BzFormItem>
        ) : null}
      </BzForm>
      {err ? <BzAlert title={err} type="error" showIcon className="form-error" /> : null}
    </BzDialog>
  );
}
