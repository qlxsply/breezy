package com.corwin.framework.error;

/**
 * 统一错误码协议：用于对外返回 code/msg。
 * - 框架提供 BaseError
 * - 业务系统可自定义 enum 实现该接口
 *
 * @author Corwin 2026/3/30
 */
public interface ErrorCode {

    /**
     * 业务错误码（与 HTTP 状态码区分）
     */
    String getCode();

    /**
     * 面向调用方的提示信息
     */
    String getMsg();

    /**
     * 当前错误码所属号段
     */
    ErrorCodeRange getRange();

}