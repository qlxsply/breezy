export interface PasswordPolicyConfig {
  passwordMinLength: number;
  passwordRequireDigit: boolean;
  passwordRequireLetter: boolean;
  passwordRequireUpper: boolean;
  passwordRequireLower: boolean;
  passwordRequireSpecial: boolean;
  passwordForceChangeOnFirstLogin: boolean;
  passwordForceChangeOnReset: boolean;
}
