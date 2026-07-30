package com.corwin.framework.mybatis.pagination;

/**
 * Exception thrown by the MyBatis {@link PageData} pagination component.
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
