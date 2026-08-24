"use client";

import { changePassword } from "@admin/features/auth/public/session";
import { message } from "@admin/shared/lib/feedback/message";
import { AdminFormSection } from "@admin/shared/ui/admin";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { BzButton, BzForm, BzFormItem, BzInput } from "@admin/shared/ui/bz";
import { useEffect, useMemo, useRef, useState } from "react";

import { getPasswordPolicyConfig } from "./api/password-policy-client";
import type { PasswordPolicyConfig } from "./model/password-policy";
import styles from "./Profile.module.css";

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
  if (rule.passwordRequireSpecial && !/[^A-Za-z0-9]/.test(password))
    return "新密码必须包含特殊字符";
  return null;
}

export function AdminProfilePasswordPage() {
  const [saving, setSaving] = useState(false);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [policy, setPolicy] = useState<PasswordPolicyConfig | null>(null);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const saveLockRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    void loadPasswordPolicy(controller);
    return () => controller.abort();
  }, []);

  const passwordRuleText = useMemo(() => buildPasswordRuleText(policy), [policy]);

  async function loadPasswordPolicy(controller: AbortController) {
    setPolicyLoading(true);
    try {
      const nextPolicy = await getPasswordPolicyConfig({ signal: controller.signal });
      if (!controller.signal.aborted) setPolicy(nextPolicy);
    } catch (error) {
      if (!controller.signal.aborted) {
        message.error(error instanceof Error ? error.message : "密码规则加载失败");
      }
    } finally {
      if (!controller.signal.aborted) setPolicyLoading(false);
    }
  }

  async function submit() {
    const oldPasswordValue = oldPassword;
    const newPasswordValue = newPassword;
    const confirmPasswordValue = confirmPassword;

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

    if (saveLockRef.current) return;
    saveLockRef.current = true;
    setSaving(true);
    try {
      await changePassword(oldPasswordValue, newPasswordValue);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      message.success("已确认");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "密码修改失败");
    } finally {
      saveLockRef.current = false;
      setSaving(false);
    }
  }

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <div className={styles.pagePanel}>
            <AdminFormSection
              title="修改密码"
              className={styles.passwordSection}
              bodyClassName={styles.passwordBody}
              footerClassName={styles.passwordFooter}
              footerAlign="left"
              footer={
                <BzButton
                  buttonType="primary"
                  loading={saving || policyLoading}
                  onClick={() => void submit()}
                >
                  确认
                </BzButton>
              }
            >
              <BzForm className={styles.passwordForm}>
                <BzFormItem
                  label="当前密码"
                  contentWidth={240}
                >
                  <BzInput
                    modelValue={oldPassword}
                    type="password"
                    placeholder="请输入当前密码"
                    onValueChange={setOldPassword}
                  />
                </BzFormItem>
                <BzFormItem
                  label="新密码"
                  meta={passwordRuleText}
                  contentWidth={240}
                >
                  <BzInput
                    modelValue={newPassword}
                    type="password"
                    placeholder="请输入新密码"
                    onValueChange={setNewPassword}
                  />
                </BzFormItem>
                <BzFormItem
                  label="确认新密码"
                  meta="请再次输入与新密码一致的内容"
                  contentWidth={240}
                >
                  <BzInput
                    modelValue={confirmPassword}
                    type="password"
                    placeholder="请再次输入新密码"
                    onValueChange={setConfirmPassword}
                  />
                </BzFormItem>
              </BzForm>
            </AdminFormSection>
          </div>
        </div>
      </div>
    </div>
  );
}
