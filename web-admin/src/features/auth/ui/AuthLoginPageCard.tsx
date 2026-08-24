"use client";

import type { AuthUserType } from "@admin/features/auth/model/types";
import { login, resolveLandingPathForUser } from "@admin/features/auth/service/auth-service";
import {
  BzAlert,
  BzButton,
  BzForm,
  BzFormItem,
  BzInput,
  type BzInputRef,
} from "@admin/shared/ui/bz";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import styles from "./AuthLoginPageCard.module.css";

interface AuthLoginPageCardProps {
  formTitle: string;
  returnLabel: string;
  returnTo: string;
}

export function AuthLoginPageCard({ formTitle, returnLabel, returnTo }: AuthLoginPageCardProps) {
  const router = useRouter();
  const usernameInputRef = useRef<BzInputRef | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    usernameInputRef.current?.focus();
  }, []);

  function resolveRedirectPath(userType: AuthUserType): string {
    const redirect =
      typeof window === "undefined"
        ? ""
        : new URLSearchParams(window.location.search).get("redirect")?.trim() || "";
    if (!redirect) {
      return resolveLandingPathForUser(userType);
    }
    if (
      userType === "ADMIN" &&
      (redirect === "/admin" || redirect.startsWith("/admin/")) &&
      redirect !== "/admin/login" &&
      !redirect.startsWith("//")
    ) {
      return redirect;
    }
    return resolveLandingPathForUser(userType);
  }

  async function submit() {
    if (!username.trim() || !password) {
      setError("账号和密码不能为空");
      return;
    }

    try {
      setSubmitting(true);
      setError("");

      const current = await login(username.trim(), password);
      router.push(
        current.mustChangePassword
          ? "/admin/profile/password"
          : resolveRedirectPath(current.userType),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.title}>{formTitle}</h1>

        <BzForm className={styles.form}>
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

        {error ? (
          <BzAlert
            title={error}
            type="error"
            showIcon
            className={styles.error}
          />
        ) : null}

        <div className={styles.actions}>
          <BzButton
            buttonType="primary"
            size="large"
            loading={submitting}
            onClick={() => void submit()}
          >
            登录
          </BzButton>

          <BzButton
            size="large"
            onClick={() => router.push(returnTo)}
          >
            {returnLabel}
          </BzButton>
        </div>
      </div>
    </section>
  );
}
