package com.corwin.system.config.application.validation;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.ClientIpMode;
import com.corwin.framework.web.auth.AuthWhitelistItem;
import com.corwin.framework.web.auth.CompiledWhitelistRule;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Validator for CLIENT_IP_MODE and AUTH_WHITELIST configuration values.
 * Ensures the client IP mode string is a valid enum value and the
 * auth whitelist JSON can be parsed and compiled into whitelist rules.
 *
 * @author Corwin 2026/3/11
 */
@Order(20)
@Component
@RequiredArgsConstructor
public class ConfigPatternValidator implements ConfigValueValidator {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(StoredConfig config) {
        if (config == null) {
            return false;
        }
        String code = config.code();
        return DefaultConfigKeys.CLIENT_IP_MODE.name().equals(code)
                || DefaultConfigKeys.AUTH_WHITELIST.name().equals(code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(StoredConfig config, String rawValue) {
        if (config == null) {
            return;
        }

        String code = config.code();
        if (DefaultConfigKeys.CLIENT_IP_MODE.name().equals(code)) {
            validateClientIpMode(rawValue);
            return;
        }

        if (DefaultConfigKeys.AUTH_WHITELIST.name().equals(code)) {
            validateAuthWhitelist(rawValue);
        }
    }

    private void validateAuthWhitelist(String rawValue) {
        try {
            for (AuthWhitelistItem item : parseAuthWhitelistItems(rawValue)) {
                CompiledWhitelistRule.compile(item);
            }
        } catch (Exception ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }

    private List<AuthWhitelistItem> parseAuthWhitelistItems(String rawValue) throws Exception {
        String normalized = normalize(rawValue);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("auth whitelist cannot be blank");
        }
        List<AuthWhitelistItem> items = Json.parse(normalized, new TypeReference<>() {
        });
        return items == null ? Collections.emptyList() : items;
    }

    private void validateClientIpMode(String rawValue) {
        String value = normalize(rawValue);
        try {
            ClientIpMode.valueOf(value);
        } catch (Exception ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }

    private String normalize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }
}
