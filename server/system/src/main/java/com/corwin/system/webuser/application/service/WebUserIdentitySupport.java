package com.corwin.system.webuser.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.SignUtil;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Corwin 2026/5/11
 */
@Component
public class WebUserIdentitySupport {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{6,20}$");

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

    public String normalizeForType(WebUserIdentityType type, String raw) {
        BizAssert.notBlank(raw, BaseError.MISSING_PARAMETER);
        String trimmed = raw.trim();
        return switch (type) {
            case USERNAME, EMAIL -> trimmed.toLowerCase(Locale.ROOT);
            case PHONE -> trimmed.replace(" ", "");
            case OAUTH -> trimmed;
        };
    }

    public String hash(WebUserIdentityType type, String raw) {
        return SignUtil.sha256(type.name() + ":" + normalizeForType(type, raw));
    }

    public String normalizeLoginInput(String raw) {
        BizAssert.notBlank(raw, BaseError.MISSING_PARAMETER);
        return raw.trim();
    }
}
