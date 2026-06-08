package com.corwin.framework.web.response;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.ctx.CtxUtil;
import lombok.Getter;

import java.time.Instant;

/**
 * 返回结果
 *
 * @author Corwin 2025/10/12
 */
@Getter
public class ApiResponse<T> {
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 业务编码：成功/失败/具体错误码（与HTTP状态码区分）
     */
    private String code;
    /**
     * 提示信息
     */
    private String msg;
    /**
     * 响应时间
     */
    private Instant timestamp;
    /**
     * 链路追踪ID
     */
    private String traceId;
    /**
     * 返回数据
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
