package com.corwin.system.config.application.validation;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.config.UserTimeZoneOption;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

/**
 * @author Corwin 2026/1/31
 */
@Order(10)
@Component
public class TimeZoneValidator implements ConfigValueValidator {

    @Override
    public boolean supports(StoredConfig config) {
        return config != null && DefaultConfigKeys.USER_TIME_ZONE.name().equals(config.code());
    }

    @Override
    public void validate(StoredConfig config, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }

        String zoneId = UserTimeZoneOption.zoneIdOf(rawValue.trim());
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        if (!tz.getID().equals(zoneId) && !"GMT".equalsIgnoreCase(zoneId)) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }
}
