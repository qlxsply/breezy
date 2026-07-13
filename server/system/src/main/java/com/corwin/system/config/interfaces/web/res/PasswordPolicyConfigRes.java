package com.corwin.system.config.interfaces.web.res;

/**
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
