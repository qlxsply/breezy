package com.corwin.system.scheduler.domain.model;

import com.corwin.framework.error.BaseError;

/**
 * 任务执行结果。
 *
 * @author Corwin 2026/4/15
 */
public record JobResult(
        boolean success,
        String code,
        String message
) {

    public static JobResult success(String message) {
        return new JobResult(true, BaseError.SUCCESS.getCode(), message);
    }

    public static JobResult failure(String code, String message) {
        return new JobResult(false, code, message);
    }
}
