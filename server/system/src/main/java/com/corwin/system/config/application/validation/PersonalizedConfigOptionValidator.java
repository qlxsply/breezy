package com.corwin.system.config.application.validation;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.config.UserTimeZoneOption;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.dict.application.service.DictQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Validator for personalized user configuration options such as time zone,
 * date/time format, and decimal format. Ensures the selected option exists
 * in the dictionary and that time zone values are valid Java ZoneIds.
 *
 * @author Corwin 2026/3/13
 */
@Order(30)
@Component
@RequiredArgsConstructor
public class PersonalizedConfigOptionValidator implements ConfigValueValidator {

    private final DictQueryService dictQueryService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(StoredConfig config) {
        if (config == null) {
            return false;
        }
        String code = config.code();
        return DefaultConfigKeys.USER_TIME_ZONE.name().equals(code)
                || DefaultConfigKeys.USER_DATE_TIME_FORMAT.name().equals(code)
                || DefaultConfigKeys.USER_DATE_FORMAT.name().equals(code)
                || DefaultConfigKeys.USER_DECIMAL_FORMAT.name().equals(code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(StoredConfig config, String rawValue) {
        if (config == null) {
            return;
        }
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }

        String code = config.code();
        if (!dictQueryService.existsCode(code, value)) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
        if (DefaultConfigKeys.USER_TIME_ZONE.name().equals(code)
                && !ZoneId.getAvailableZoneIds().contains(UserTimeZoneOption.zoneIdOf(value))) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }
}
