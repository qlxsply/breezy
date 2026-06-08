package com.corwin.system.auth.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.system.config.application.config.SystemConfigWrapper;
import org.springframework.stereotype.Service;

/**
 * @author Corwin 2026/1/23
 */
@Service
public class PasswordPolicyService {

    public void validate(String password) {
        BizAssert.notBlank(password, BaseError.MISSING_PARAMETER);
        if (password.length() < SystemConfigWrapper.passwordMinLength()) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
        if (SystemConfigWrapper.passwordRequireDigit() && !password.matches(".*\\d.*")) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
        if (SystemConfigWrapper.passwordRequireLetter() && !password.matches(".*[A-Za-z].*")) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
        if (SystemConfigWrapper.passwordRequireUpper() && !password.matches(".*[A-Z].*")) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
        if (SystemConfigWrapper.passwordRequireLower() && !password.matches(".*[a-z].*")) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
        if (SystemConfigWrapper.passwordRequireSpecial() && !password.matches(".*[^A-Za-z0-9].*")) {
            BizAssert.fail(BaseError.WEAK_PASSWORD);
        }
    }

}
