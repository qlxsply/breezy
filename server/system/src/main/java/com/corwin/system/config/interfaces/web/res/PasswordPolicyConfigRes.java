package com.corwin.system.config.interfaces.web.res;

/**
 * Response DTO for password policy configuration.
 * Contains all password strength and policy rules such as minimum length,
 * character requirements, and forced change settings.
 *
 * @author Corwin 2026/7/13
 */
public record PasswordPolicyConfigRes(
        int passwordMinLength,
        boolean passwordRequireDigit,
        boolean passwordRequireLetter,
        boolean passwordRequireUpper,
        boolean passwordRequireLower,
        boolean passwordRequireSpecial,
        boolean passwordForceChangeOnFirstLogin,
        boolean passwordForceChangeOnReset
) {
}
