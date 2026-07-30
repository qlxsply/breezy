package com.corwin.framework.config;

import com.corwin.framework.util.LowDate;

import java.util.TimeZone;

/**
 * Convenience wrapper for reading framework default config values.
 *
 * @author Corwin 2026/1/31
 */
public class DefaultConfigWrapper {

    public static TimeZone timeZone() {
        return ConfigRegistry.customV(DefaultConfigKeys.USER_TIME_ZONE,
                code -> LowDate.parseTimeZone(UserTimeZoneOption.zoneIdOf(code)));
    }

    private DefaultConfigWrapper() {
    }

}
