package com.corwin.system.config.application.validation;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.config.ConfigValueParser;
import com.corwin.framework.config.ConfigValueType;
import com.corwin.framework.config.StoredConfig;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Universal configuration value validator that checks type-level constraints.
 * Validates that the raw value can be parsed according to the config's
 * declared {@link com.corwin.framework.config.ConfigValueType} (e.g. INT, LONG, BOOL, etc.).
 *
 * @author Corwin 2026/1/31
 */
@Order(0)
@Component
public class CommonValidator implements ConfigValueValidator {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(StoredConfig config) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(StoredConfig config, String rawValue) {
        if (rawValue == null) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
        try {
            String code = config.code();
            ConfigValueType type = config.valueType();
            switch (type) {
                case INT -> ConfigValueParser.parseInt(code, rawValue);
                case LONG -> ConfigValueParser.parseLong(code, rawValue);
                case BOOL -> ConfigValueParser.parseBool(code, rawValue);
                case DEC -> ConfigValueParser.parseDec(code, rawValue);
                case STR -> {
                }
                case STR_LIST -> ConfigValueParser.parseStrList(code, rawValue);
                case STR_SET -> ConfigValueParser.parseStrSet(code, rawValue);
                default -> throw new BizException(BaseError.INVALID_PARAMETER);
            }
        } catch (RuntimeException ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }
}
