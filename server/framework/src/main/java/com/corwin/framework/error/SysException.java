package com.corwin.framework.error;

/**
 * System exception representing an unexpected, unrecoverable failure.
 * <p>
 * Carries an {@link ErrorCode} ({@code outwardCode}) that is safe to expose to API consumers
 * while the full stack trace is logged internally.
 *
 * @author Corwin 2025/5/12
 */
public class SysException extends RuntimeException {

    private final ErrorCode outwardCode;

    private SysException(ErrorCode outwardCode) {
        this.outwardCode = outwardCode;
    }

    public SysException(String message, ErrorCode outwardCode) {
        super(message);
        this.outwardCode = outwardCode;
    }

    public SysException(String message, Throwable cause, ErrorCode outwardCode) {
        super(message, cause);
        this.outwardCode = outwardCode;
    }

    public SysException(Throwable cause, ErrorCode outwardCode) {
        super(cause);
        this.outwardCode = outwardCode;
    }

    private SysException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
            ErrorCode outwardCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.outwardCode = outwardCode;
    }

    public final ErrorCode getOutwardCode() {
        return this.outwardCode;
    }

    public final String getCode() {
        return this.outwardCode.getCode();
    }

}
