package com.corwin.system.auth.application.error;

import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.error.ErrorCodeRange;
import com.corwin.framework.error.ErrorCodeRanges;
import com.corwin.framework.error.ErrorCodes;

/**
 * Error codes for the authentication and authorization domain.
 *
 * @author Corwin 2026/4/19
 */
public enum AuthError implements ErrorCode {
    UNAUTHENTICATED("100000", "Unauthenticated"),
    FORBIDDEN("100001", "Forbidden"),
    INVALID_TOKEN("100002", "Invalid token"),
    TOKEN_EXPIRED("100003", "Token expired"),
    TOKEN_REVOKED("100004", "Token revoked"),
    USER_DISABLED("100005", "User is disabled"),
    BAD_CREDENTIALS("100006", "Bad credentials"),
    PERMISSION_DECLARATION_MISSING("100007", "Permission declaration missing");

    private final String code;
    private final String msg;

    AuthError(String code, String msg) {
        ErrorCodes.validate(code, msg, ErrorCodeRanges.BREEZY_SYSTEM_AUTH);
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public ErrorCodeRange getRange() {
        return ErrorCodeRanges.BREEZY_SYSTEM_AUTH;
    }
}
