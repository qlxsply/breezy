package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * User-selectable time zone options.
 *
 * @author Corwin 2026/5/5
 */
public enum UserTimeZoneOption implements DictEnumDefinition {
    ASIA_SHANGHAI("Asia/Shanghai (UTC+08:00)", "Asia/Shanghai"),
    UTC("UTC (UTC+00:00)", "UTC"),
    ASIA_TOKYO("Asia/Tokyo (UTC+09:00)", "Asia/Tokyo"),
    EUROPE_BERLIN("Europe/Berlin (UTC+01:00)", "Europe/Berlin"),
    AMERICA_NEW_YORK("America/New_York (UTC-05:00)", "America/New_York");

    private final String label;
    private final String zoneId;

    UserTimeZoneOption(String label, String zoneId) {
        this.label = label;
        this.zoneId = zoneId;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String itemValue() {
        return zoneId;
    }

    public String zoneId() {
        return zoneId;
    }

    public static UserTimeZoneOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ASIA_SHANGHAI;
        }
        String normalized = code.trim();
        for (UserTimeZoneOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        return ASIA_SHANGHAI;
    }

    public static String zoneIdOf(String code) {
        return fromCode(code).zoneId();
    }
}
