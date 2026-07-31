package com.corwin.framework.web.response;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.ctx.CtxUtil;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API response envelope.
 *
 * @param <T> the data type
 * @author Corwin 2025/10/12
 */
@Getter
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String msg;
    private Instant timestamp;
    private String traceId;
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
        ApiResponse<T> response = base(true, code, msg);
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> fail(ErrorCode ec) {
        return fail(ec.getCode(), ec.getMsg());
    }

    public static <T> ApiResponse<T> fail(String code, String msg) {
        return base(false, code, msg);
    }

    public static <T> ApiResponse<T> fail(T data, String code, String msg) {
        ApiResponse<T> response = base(false, code, msg);
        response.data = data;
        return response;
    }

    private static <T> ApiResponse<T> base(boolean success, String code, String msg) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = success;
        response.code = code;
        response.msg = msg;
        response.timestamp = HighDate.mockInstant();
        response.traceId = CtxUtil.getTraceId();
        return response;
    }
}
