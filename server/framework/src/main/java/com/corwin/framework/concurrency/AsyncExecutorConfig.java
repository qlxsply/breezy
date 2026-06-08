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
 * Spring 异步执行器配置。
 *
 * <p>用于指定 {@code @Async} 默认使用的线程池。
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
        //仅处理返回值为 void 的 @Async 方法抛出的异常。
        return (ex, method, params) -> {
            String traceId = CtxUtil.getTraceId();
            String spanId = CtxUtil.getSpanId();

            log.error("Async method execute error. traceId={}, spanId={}, class={}, method={}, params={}", traceId,
                    spanId, method.getDeclaringClass().getName(), method.getName(), Json.toStr(params), ex);
        };
    }

}