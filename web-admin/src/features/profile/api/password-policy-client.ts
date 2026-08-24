import { get, type RequestOptions } from "@admin/shared/transport";

import type { PasswordPolicyConfig } from "../model/password-policy";

interface EffectiveConfigPayload {
  effectiveValue?: Record<string, unknown> | null;
}

export async function getPasswordPolicyConfig(
  options?: Pick<RequestOptions, "signal">,
): Promise<PasswordPolicyConfig> {
  const result = await get<EffectiveConfigPayload>(
    "/sys/configs/effective/system.security.password-policy",
    options,
  );
  const value = result.effectiveValue || {};
  const minLength = Number(value.minLength);
  if (!Number.isFinite(minLength) || minLength < 1) throw new Error("密码策略配置无效");
  return {
    passwordMinLength: minLength,
    passwordRequireDigit: value.requireDigit === true,
    passwordRequireLetter: value.requireLetter === true,
    passwordRequireUpper: value.requireUpper === true,
    passwordRequireLower: value.requireLower === true,
    passwordRequireSpecial: value.requireSpecial === true,
    passwordForceChangeOnFirstLogin: value.forceChangeOnFirstLogin === true,
    passwordForceChangeOnReset: value.forceChangeOnReset === true,
  };
}
