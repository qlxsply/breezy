"use client";

import { BzAlert, BzButton, BzForm, BzFormItem, BzInput, type BzInputRef } from "@admin/components/bz";
import { type AuthSpace,login, resolveLandingPathForUser } from "@admin/core/registry/auth-registry";
import { ensureRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { ensurePermissionsLoaded } from "@admin/core/registry/permissions-registry";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

interface AuthLoginPageCardProps {
  scope: AuthSpace;
  formTitle: string;
  returnLabel: string;
  returnTo: string;
  extraActionLabel?: string;
  extraActionTo?: string;
}

export function AuthLoginPageCard({
  scope,
  formTitle,
  returnLabel,
  returnTo,
  extraActionLabel,
  extraActionTo,
}: AuthLoginPageCardProps) {
  const router = useRouter();
  const usernameInputRef = useRef<BzInputRef | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    usernameInputRef.current?.focus();
  }, []);

  function resolveRedirectPath(userType: "INTERNAL" | "EXTERNAL" | "GUEST"): string {
    const redirect =
      typeof window === "undefined"
        ? ""
        : new URLSearchParams(window.location.search).get("redirect")?.trim() || "";
    if (!redirect) {
      return resolveLandingPathForUser(userType);
    }
    if (userType === "INTERNAL") {
      return redirect.startsWith("/") ? redirect : resolveLandingPathForUser(userType);
    }
    return redirect.startsWith("/") ? resolveLandingPathForUser(userType) : redirect;
  }

  async function submit() {
    if (!username.trim() || !password) {
      setError("账号和密码不能为空");
      return;
    }

    try {
      setSubmitting(true);
      setError("");

      const current = await login(scope, username.trim(), password);
      await ensureRegistryLoaded(true);
      await ensurePermissionsLoaded();
      router.push(resolveRedirectPath(current.userType));
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card">
        <h1 className="auth-card__title">{formTitle}</h1>

        <BzForm className="auth-card__form">
          <BzFormItem label="账号">
            <BzInput
              ref={usernameInputRef}
              modelValue={username}
              placeholder="请输入账号"
              onValueChange={setUsername}
              onKeyUp={(event) => event.key === "Enter" && void submit()}
            />
          </BzFormItem>

          <BzFormItem label="密码">
            <BzInput
              modelValue={password}
              type="password"
              placeholder="请输入密码"
              onValueChange={setPassword}
              onKeyUp={(event) => event.key === "Enter" && void submit()}
            />
          </BzFormItem>
        </BzForm>

        {error ? <BzAlert title={error} type="error" showIcon className="auth-card__error" /> : null}

        <div className={`auth-card__actions${extraActionLabel && extraActionTo ? " has-extra-action" : ""}`}>
          <BzButton buttonType="primary" size="large" loading={submitting} onClick={() => void submit()}>
            登录
          </BzButton>

          <BzButton size="large" onClick={() => router.push(returnTo)}>
            {returnLabel}
          </BzButton>

          {extraActionLabel && extraActionTo ? (
            <BzButton size="large" onClick={() => router.push(extraActionTo)}>
              {extraActionLabel}
            </BzButton>
          ) : null}
        </div>
      </div>
    </section>
  );
}
