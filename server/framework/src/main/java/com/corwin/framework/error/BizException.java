package com.corwin.framework.error;

/**
 * Business exception representing an expected, recoverable failure.
 * <p>
 * Carries an {@link ErrorCode} that is returned to the API consumer.
 *
 * @author Corwin 2026/3/30
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode == null ? null : errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public BizException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    private BizException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    private BizException(Throwable cause, ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    private BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
            ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public final ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public final String getCode() {
        return this.errorCode.getCode();
    }

}
