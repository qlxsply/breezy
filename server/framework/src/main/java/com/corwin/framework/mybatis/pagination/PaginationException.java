package com.corwin.framework.mybatis.pagination;

/**
 * MyBatis PageData 分页异常。
 *
 * @author Corwin 2026/7/28
 */
public class PaginationException extends RuntimeException {

    public PaginationException(String message) {
        super(message);
    }

    public PaginationException(String message, Throwable cause) {
        super(message, cause);
    }
}
