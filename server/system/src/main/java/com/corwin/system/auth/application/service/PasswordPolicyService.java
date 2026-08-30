package com.corwin.system.auth.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.system.auth.config.SystemAuthConfigSpecs;
import org.springframework.stereotype.Service;

/**
 * @author Corwin 2026/1/23
 */
@Service
public class PasswordPolicyService {

  public void validate(String password) {
    BizAssert.notBlank(password, BaseError.MISSING_PARAMETER);
    var policy = Configs.get(SystemAuthConfigSpecs.PASSWORD_POLICY);
    if (password.length() < policy.minLength()
        || policy.requireDigit() && !password.matches(".*\\d.*")
        || policy.requireLetter() && !password.matches(".*[A-Za-z].*")
        || policy.requireUpper() && !password.matches(".*[A-Z].*")
        || policy.requireLower() && !password.matches(".*[a-z].*")
        || policy.requireSpecial() && !password.matches(".*[^A-Za-z0-9].*")) {
      BizAssert.fail(BaseError.WEAK_PASSWORD);
    }
  }
}
