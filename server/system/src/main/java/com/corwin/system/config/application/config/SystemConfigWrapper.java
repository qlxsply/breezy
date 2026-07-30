package com.corwin.system.config.application.config;

import com.corwin.framework.config.ConfigRegistry;

/**
 * Static wrapper around ConfigRegistry for accessing system password policy configurations.
 * Provides typed convenience methods to read password-related config values.
 *
 * @author Corwin 2026/1/31
 */
public class SystemConfigWrapper {

    /**
     * Get the minimum password length requirement.
     *
     * @return minimum password length
     */
    public static int passwordMinLength() {
        return ConfigRegistry.intV(SystemConfigKeys.PASSWORD_MIN_LENGTH);
    }

    /**
     * Check whether passwords must contain at least one digit.
     *
     * @return true if digits are required
     */
    public static boolean passwordRequireDigit() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_DIGIT);
    }

    /**
     * Check whether passwords must contain at least one letter.
     *
     * @return true if letters are required
     */
    public static boolean passwordRequireLetter() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_LETTER);
    }

    /**
     * Check whether passwords must contain at least one uppercase letter.
     *
     * @return true if uppercase letters are required
     */
    public static boolean passwordRequireUpper() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_UPPER);
    }

    /**
     * Check whether passwords must contain at least one lowercase letter.
     *
     * @return true if lowercase letters are required
     */
    public static boolean passwordRequireLower() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_LOWER);
    }

    /**
     * Check whether passwords must contain at least one special character.
     *
     * @return true if special characters are required
     */
    public static boolean passwordRequireSpecial() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_SPECIAL);
    }

    /**
     * Check whether users must change their password on first login.
     *
     * @return true if forced change on first login is enabled
     */
    public static boolean passwordForceChangeOnFirstLogin() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_FIRST_LOGIN);
    }

    /**
     * Check whether users must change their password after a password reset.
     *
     * @return true if forced change on reset is enabled
     */
    public static boolean passwordForceChangeOnReset() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_RESET);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private SystemConfigWrapper() {
    }

}
