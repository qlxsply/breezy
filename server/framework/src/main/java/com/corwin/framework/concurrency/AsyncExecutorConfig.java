package com.corwin.framework.concurrency;

import com.corwin.framework.json.Json;
import com.corwin.framework.web.ctx.CtxUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * {@link org.springframework.scheduling.annotation.AsyncConfigurer} that wires
 * the application's async thread pool into Spring's {@code @Async} infrastructure.
 *
 * @author Corwin 2026/3/30
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class AsyncExecutorConfig implements AsyncConfigurer {

    private final Executor taskExecutor;

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // Only handles exceptions thrown by void @Async methods.
        return (ex, method, params) -> {
            String traceId = CtxUtil.getTraceId();
            String spanId = CtxUtil.getSpanId();

            log.error("Async method execute error. traceId={}, spanId={}, class={}, method={}, params={}", traceId,
                    spanId, method.getDeclaringClass().getName(), method.getName(), Json.toStr(params), ex);
        };
    }

}