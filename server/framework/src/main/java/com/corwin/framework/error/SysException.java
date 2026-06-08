package com.corwin.framework.error;

/**
 * 系统异常：用于表达“非预期系统失败”，内部日志需要完整堆栈；
 * <p>
 * 对外返回时使用 outwardCode（对外可展示的错误码）。
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
