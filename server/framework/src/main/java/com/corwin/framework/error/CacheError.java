package com.corwin.framework.error;

/**
 * Cache 组件错误码定义。
 *
 * @author Corwin 2026/4/19
 */
public enum CacheError implements ErrorCode {
    CACHE_MODE_REQUIRED("020000", "Cache mode is required"),
    CACHE_MODE_NOT_IMPLEMENTED("020001", "Cache mode is not implemented yet"),
    CACHE_KEY_REQUIRED("020002", "Cache key is required"),
    CACHE_KEY_TOO_LONG("020003", "Cache key is too long"),
    CACHE_FIELD_REQUIRED("020004", "Cache field is required"),
    CACHE_VALUE_REQUIRED("020005", "Cache value is required"),
    CACHE_VALUE_TYPE_REQUIRED("020006", "Cache value type is required"),
    CACHE_TTL_INVALID("020007", "Cache TTL must be greater than zero"),
    CACHE_DATA_TYPE_MISMATCH("020008", "Cache data type does not match existing key type"),
    CACHE_VALUE_TYPE_MISMATCH("020009", "Cache value type does not match stored value type"),
    CACHE_OPERATION_NOT_SUPPORTED("020010", "Cache operation is not supported"),
    CACHE_LOCAL_STORE_ERROR("020011", "Cache local store error"),
    ;

    private final String code;
    private final String msg;

    CacheError(String code, String msg) {
        ErrorCodes.validate(code, msg, ErrorCodeRanges.FRAMEWORK_CACHE);
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
        return ErrorCodeRanges.FRAMEWORK_CACHE;
    }
}
