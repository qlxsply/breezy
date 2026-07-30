package com.corwin.system.webuser.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.SignUtil;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility component for web user identity type detection, normalization, and hashing.
 *
 * @author Corwin 2026/5/11
 */
@Component
public class WebUserIdentitySupport {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{6,20}$");

    /**
     * Detect the identity type from the raw input value (email, phone, or username).
     */
    public WebUserIdentityType detectType(String raw) {
        String value = normalizeLoginInput(raw);
        if (value.contains("@")) {
            return WebUserIdentityType.EMAIL;
        }
        if (PHONE_PATTERN.matcher(value).matches()) {
            return WebUserIdentityType.PHONE;
        }
        return WebUserIdentityType.USERNAME;
    }

    /**
     * Normalize the raw value according to the identity type (lowercase for username/email, strip spaces for phone).
     */
    public String normalizeForType(WebUserIdentityType type, String raw) {
        BizAssert.notBlank(raw, BaseError.MISSING_PARAMETER);
        String trimmed = raw.trim();
        return switch (type) {
            case USERNAME, EMAIL -> trimmed.toLowerCase(Locale.ROOT);
            case PHONE -> trimmed.replace(" ", "");
            case OAUTH -> trimmed;
        };
    }

    /**
     * Compute a SHA-256 hash of the normalized identity value for the given type.
     */
    public String hash(WebUserIdentityType type, String raw) {
        return SignUtil.sha256(type.name() + ":" + normalizeForType(type, raw));
    }

    /**
     * Trim the raw login input value.
     */
    public String normalizeLoginInput(String raw) {
        BizAssert.notBlank(raw, BaseError.MISSING_PARAMETER);
        return raw.trim();
    }
}
