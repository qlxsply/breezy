package com.corwin.framework.error;

/**
 * 基础错误码定义。
 * <p>
 * 约定：
 * - code：业务错误码
 * - msg ：对外提示信息，统一使用英文
 * <p>
 * 说明：
 * - 该枚举仅包含“框架级 / 通用级”错误
 * - 各业务系统可自行定义 enum 实现 ErrorCode
 *
 * @author Corwin 2026/3/30
 */
public enum BaseError implements ErrorCode {
    SUCCESS("000000", "Success"),
    INTERNAL_ERROR("000001", "Internal error"),
    SERVICE_ERROR("000002", "Service error"),
    SERVICE_BUSY("000003", "Service busy, please try again later"),
    TIMEOUT("000004", "Operation timeout"),
    RATE_LIMITED("000005", "Too many requests"),

    BAD_CREDENTIALS("000010", "Bad credentials"),
    INVALID_TOKEN("000011", "Invalid token"),
    TOKEN_EXPIRED("000012", "Token expired"),
    FORBIDDEN("000013", "Forbidden"),

    MISSING_PARAMETER("000020", "Missing required parameter"),
    INVALID_PARAMETER("000021", "Invalid parameter format"),
    ILLEGAL_ARGUMENT("000022", "Illegal argument"),
    BODY_NOT_READABLE("000023", "Request body is not readable"),
    METHOD_NOT_ALLOWED("000024", "Method not allowed"),
    UNSUPPORTED_MEDIA_TYPE("000025", "Unsupported media type"),

    DUPLICATE_REQUEST("000030", "Duplicate request"),
    TOO_FREQUENT("000031", "Request too frequent"),

    CONFLICT("000040", "Resource conflict"),
    NOT_FOUND("000041", "Resource not found"),

    FILE_TOO_LARGE("000050", "File too large"),
    UNSUPPORTED_FILE_TYPE("000051", "Unsupported file type"),

    INVALID_VERIFICATION_CODE("000060", "Invalid verification code"),
    USER_DISABLED("000061", "User is disabled"),
    WEAK_PASSWORD("000062", "Password does not meet policy"),

    THIRD_PARTY_ERROR("000070", "Third-party service error"),
    THIRD_PARTY_TIMEOUT("000071", "Third-party service timeout"),
    THIRD_PARTY_UNAVAILABLE("000072", "Third-party service unavailable"),

    DB_ERROR("000080", "Database error"),
    DB_DUPLICATE_KEY("000081", "Duplicate key"),
    DB_DEADLOCK("000082", "Database deadlock"),
    DB_TIMEOUT("000083", "Database timeout"),
    ;

    private final ErrorCodeRange RANGE = ErrorCodeRanges.FRAMEWORK_BASE;
    private final String code;
    private final String msg;

    BaseError(String code, String msg) {
        ErrorCodes.validate(code, msg, RANGE);
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    @Override
    public ErrorCodeRange getRange() {
        return RANGE;
    }

}
