package com.corwin.framework.web.response;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.ctx.CtxUtil;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API response envelope.
 * <p>
 * All REST endpoints return this structure, providing a consistent
 * contract with success flag, business code, message, timestamp,
 * trace ID, and optional data payload.
 *
 * @param <T> the data type
 * @author Corwin 2025/10/12
 */
@Getter
public class ApiResponse<T> {
    /**
     * Whether the request was processed successfully
     */
    private boolean success;
    /**
     * Business code (distinct from HTTP status code)
     */
    private String code;
    /**
     * Human-readable message
     */
    private String msg;
    /**
     * Response timestamp
     */
    private Instant timestamp;
    /**
     * Trace ID for distributed tracing
     */
    private String traceId;
    /**
     * Response data payload
     */
    private T data;

    public static ApiResponse<Object> ok() {
        return okWithCode(null, BaseError.SUCCESS.getCode(), BaseError.SUCCESS.getMsg());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return okWithCode(data, BaseError.SUCCESS.getCode(), BaseError.SUCCESS.getMsg());
    }

    public static <T> ApiResponse<T> okWithCode(T data, ErrorCode ec) {
        return okWithCode(data, ec.getCode(), ec.getMsg());
    }

    public static ApiResponse<Object> okWithCode(ErrorCode ec) {
        return okWithCode(null, ec.getCode(), ec.getMsg());
    }

    public static ApiResponse<Object> okWithCode(String code, String msg) {
        return okWithCode(null, code, msg);
    }

    public static <T> ApiResponse<T> okWithCode(T data, String code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.code = code;
        r.msg = msg;
        r.data = data;
        r.timestamp = HighDate.mockInstant();
        r.traceId = CtxUtil.getTraceId();
        return r;
    }

    public static <T> ApiResponse<T> fail(ErrorCode ec) {
        return fail(ec.getCode(), ec.getMsg());
    }

    public static <T> ApiResponse<T> fail(String code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.code = code;
        r.msg = msg;
        r.timestamp = HighDate.mockInstant();
        r.traceId = CtxUtil.getTraceId();
        return r;
    }

}
