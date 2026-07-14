"use client";

import { getPasswordPolicyConfig, type PasswordPolicyConfig } from "@admin/api/configs";
import { AdminFormSection } from "@admin/components/admin";
import { BzButton, BzForm, BzFormItem, BzInput } from "@admin/components/bz";
import { message } from "@admin/core/message";
import { changePassword } from "@admin/core/registry/auth-registry";
import { useEffect, useMemo, useState } from "react";

function buildPasswordRuleText(rule: PasswordPolicyConfig | null): string {
  if (!rule) return "正在加载密码规则...";

  const conditions: string[] = [];
  if (rule.passwordRequireDigit) conditions.push("数字");
  if (rule.passwordRequireLetter) conditions.push("字母");
  if (rule.passwordRequireUpper) conditions.push("大写字母");
  if (rule.passwordRequireLower) conditions.push("小写字母");
  if (rule.passwordRequireSpecial) conditions.push("特殊字符");

  const base = `密码最少 ${rule.passwordMinLength} 位`;
  return conditions.length > 0 ? `${base}，且需包含：${conditions.join("、")}` : base;
}

function validatePasswordByPolicy(password: string, rule: PasswordPolicyConfig): string | null {
  if (password.length < rule.passwordMinLength) {
    return `新密码长度不能少于 ${rule.passwordMinLength} 位`;
  }
  if (rule.passwordRequireDigit && !/\d/.test(password)) return "新密码必须包含数字";
  if (rule.passwordRequireLetter && !/[A-Za-z]/.test(password)) return "新密码必须包含字母";
  if (rule.passwordRequireUpper && !/[A-Z]/.test(password)) return "新密码必须包含大写字母";
  if (rule.passwordRequireLower && !/[a-z]/.test(password)) return "新密码必须包含小写字母";
  if (rule.passwordRequireSpecial && !/[^A-Za-z0-9]/.test(password)) return "新密码必须包含特殊字符";
  return null;
}

export function AdminProfilePasswordPage() {
  const [saving, setSaving] = useState(false);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [policy, setPolicy] = useState<PasswordPolicyConfig | null>(null);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    void loadPasswordPolicy();
  }, []);

  const passwordRuleText = useMemo(() => buildPasswordRuleText(policy), [policy]);

  async function loadPasswordPolicy() {
    setPolicyLoading(true);
    try {
      setPolicy(await getPasswordPolicyConfig());
    } catch (error) {
      message.error(error instanceof Error ? error.message : "密码规则加载失败");
    } finally {
      setPolicyLoading(false);
    }
  }

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
    if (!policy) {
      message.warning("密码规则尚未加载完成，请稍后重试");
      return;
    }

    const passwordError = validatePasswordByPolicy(newPasswordValue, policy);
    if (passwordError) {
      message.warning(passwordError);
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
          <div className="profile-page-panel">
            <AdminFormSection
              title="修改密码"
              className="profile-password-section"
              bodyClassName="profile-password-section__body"
              footerClassName="profile-password-section__footer"
              footerAlign="left"
              footer={
                <BzButton buttonType="primary" loading={saving || policyLoading} onClick={() => void submit()}>
                  确认
                </BzButton>
              }
            >
              <BzForm className="password-form">
                <BzFormItem label="当前密码" contentWidth={240}>
                  <BzInput modelValue={oldPassword} type="password" placeholder="请输入当前密码" onValueChange={setOldPassword} />
                </BzFormItem>
                <BzFormItem label="新密码" meta={passwordRuleText} contentWidth={240}>
                  <BzInput modelValue={newPassword} type="password" placeholder="请输入新密码" onValueChange={setNewPassword} />
                </BzFormItem>
                <BzFormItem label="确认新密码" meta="请再次输入与新密码一致的内容" contentWidth={240}>
                  <BzInput modelValue={confirmPassword} type="password" placeholder="请再次输入新密码" onValueChange={setConfirmPassword} />
                </BzFormItem>
              </BzForm>
            </AdminFormSection>
          </div>
        </div>
      </div>
    </div>
  );
}
