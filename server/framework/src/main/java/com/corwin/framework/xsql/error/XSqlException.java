package com.corwin.framework.xsql.error;

import com.corwin.framework.error.BizException;
import com.corwin.framework.error.ErrorCode;

/**
 * XSql 统一异常基类。
 * <p>
 * 所有 XSql 组件异常都应继承该类型，以统一错误码与异常处理通道。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlException extends BizException {

    /**
     * 使用错误码构造异常。
     */
    public XSqlException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用消息和错误码构造异常。
     */
    public XSqlException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    /**
     * 使用消息、原因与错误码构造异常。
     */
    public XSqlException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, errorCode);
        if (cause != null) {
            initCause(cause);
        }
    }
}

