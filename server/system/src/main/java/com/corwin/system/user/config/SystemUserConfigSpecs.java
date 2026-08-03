package com.corwin.system.user.config;

import com.corwin.framework.config.UserDateFormatOption;
import com.corwin.framework.config.UserDateTimeFormatOption;
import com.corwin.framework.config.UserDecimalFormatOption;
import com.corwin.framework.config.UserTimeZoneOption;
import com.corwin.framework.config.definition.*;

import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredEnum;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemUserConfigSpecs {

    public static final ConfigSpec<UserPreferenceDefaults> USER_PREFERENCE_DEFAULTS = new SimpleConfigSpec<>(
            new ConfigKey("system.user.preference-defaults"), "system", "user", "用户偏好默认值",
            "用户未设置个人偏好时使用的公共默认值", UserPreferenceDefaults.class,
            new UserPreferenceDefaults(UserTimeZoneOption.ASIA_SHANGHAI, UserDateTimeFormatOption.YYYY_MM_DD_HH_MM_SS,
                    UserDateFormatOption.YYYY_MM_DD, UserDecimalFormatOption.COMMA_DOT), 1, 10,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(requiredEnum("timeZone", "默认时区", UserTimeZoneOption.values(), 10),
                    requiredEnum("dateTimeFormat", "默认日期时间格式", UserDateTimeFormatOption.values(), 20),
                    requiredEnum("dateFormat", "默认日期格式", UserDateFormatOption.values(), 30),
                    requiredEnum("decimalFormat", "默认数字符号组合", UserDecimalFormatOption.values(), 40)), "default",
            value -> List.of(), value -> List.of());

    public record UserPreferenceDefaults(
            UserTimeZoneOption timeZone,
            UserDateTimeFormatOption dateTimeFormat,
            UserDateFormatOption dateFormat,
            UserDecimalFormatOption decimalFormat
    ) {
    }

    private SystemUserConfigSpecs() {
    }
}
