"use client";

import { BzButton, BzCard, BzForm, BzFormItem, BzInput } from "@admin/components/bz";
import { message } from "@admin/core/message";
import { changePassword } from "@admin/core/registry/auth-registry";
import { useState } from "react";

export function AdminProfilePasswordPage() {
  const [saving, setSaving] = useState(false);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  async function submit() {
    const oldPasswordValue = oldPassword.trim();
    const newPasswordValue = newPassword.trim();
    const confirmPasswordValue = confirmPassword.trim();

    if (!oldPasswordValue || !newPasswordValue || !confirmPasswordValue) {
      message.warning("请完整填写密码信息");
      return;
    }
    if (newPasswordValue !== confirmPasswordValue) {
      message.warning("两次输入的新密码不一致");
      return;
    }

    setSaving(true);
    try {
      await changePassword("internal", oldPasswordValue, newPasswordValue);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      message.success("已确认");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "密码修改失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">修改密码</div>
              </div>
            }
          >
            <div className="password-layout">
              <BzForm className="password-form">
                <BzFormItem label="当前密码">
                  <BzInput modelValue={oldPassword} type="password" placeholder="请输入当前密码" onValueChange={setOldPassword} />
                </BzFormItem>
                <BzFormItem label="新密码">
                  <BzInput modelValue={newPassword} type="password" placeholder="请输入新密码" onValueChange={setNewPassword} />
                </BzFormItem>
                <BzFormItem label="确认新密码">
                  <BzInput modelValue={confirmPassword} type="password" placeholder="请再次输入新密码" onValueChange={setConfirmPassword} />
                </BzFormItem>
              </BzForm>
              <div className="password-actions">
                <BzButton buttonType="primary" loading={saving} onClick={() => void submit()}>
                  确认
                </BzButton>
              </div>
            </div>
          </BzCard>
        </div>
      </div>
    </div>
  );
}
