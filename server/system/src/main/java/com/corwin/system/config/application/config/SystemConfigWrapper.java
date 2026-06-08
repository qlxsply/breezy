package com.corwin.system.config.application.config;

import com.corwin.framework.config.ConfigRegistry;

/**
 * @author Corwin 2026/1/31
 */
public class SystemConfigWrapper {

    public static int passwordMinLength() {
        return ConfigRegistry.intV(SystemConfigKeys.PASSWORD_MIN_LENGTH);
    }

    public static boolean passwordRequireDigit() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_DIGIT);
    }

    public static boolean passwordRequireLetter() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_LETTER);
    }

    public static boolean passwordRequireUpper() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_UPPER);
    }

    public static boolean passwordRequireLower() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_LOWER);
    }

    public static boolean passwordRequireSpecial() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_REQUIRE_SPECIAL);
    }

    public static boolean passwordForceChangeOnFirstLogin() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_FIRST_LOGIN);
    }

    public static boolean passwordForceChangeOnReset() {
        return ConfigRegistry.booleanV(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_RESET);
    }

    private SystemConfigWrapper() {
    }

}
