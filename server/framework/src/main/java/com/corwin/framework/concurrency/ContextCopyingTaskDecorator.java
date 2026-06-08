package com.corwin.framework.concurrency;

import com.corwin.framework.constant.MdcKeys;
import com.corwin.framework.web.ctx.Ctx;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.ctx.TraceGenerator;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 线程池任务上下文复制装饰器。
 *
 * <p>用于在线程切换时传播以下上下文：
 * <ul>
 *     <li>自定义请求上下文 {@link Ctx}</li>
 *     <li>日志上下文 MDC</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>在任务提交线程抓取上下文快照</li>
 *     <li>在工作线程执行前恢复上下文</li>
 *     <li>在任务执行完成后恢复工作线程原有上下文或清理上下文</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *     <li>不能依赖 InheritableThreadLocal 在线程池中传播上下文，因为线程池线程会复用</li>
 *     <li>必须在 finally 中清理或恢复上下文，避免串请求</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 */
public class ContextCopyingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        // 1. 抓取提交任务线程的上下文快照
        Ctx parentCtx = CtxUtil.snapshot();
        String parentSpanIdAtSubmit = CtxUtil.getSpanId();
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        return () -> {
            // 2. 保存工作线程原上下文（线程池线程可能已被复用）
            Ctx previousCtx = CtxUtil.snapshot();
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();

            try {
                // 3. 恢复父线程 Ctx
                CtxUtil.restore(parentCtx);

                // 4. 异步边界创建新 spanId，并记录父 spanId
                String traceId = CtxUtil.getTraceId();
                if (traceId != null) {
                    CtxUtil.setSpanInfo(TraceGenerator.newSpanId(), parentSpanIdAtSubmit);
                }

                // 5. 先按 Ctx 绑定 MDC
                CtxUtil.bindMdc();

                // 6. 若父线程有额外 MDC 字段，补充复制（避免覆盖当前线程新 spanId）
                if (parentMdc != null && !parentMdc.isEmpty()) {
                    parentMdc.forEach((key, value) -> {
                        if (MdcKeys.TRACE_ID.equals(key) || MdcKeys.SPAN_ID.equals(key)) {
                            return;
                        }
                        MDC.put(key, value);
                    });
                }

                runnable.run();
            } finally {
                // 6. 恢复工作线程原上下文，避免污染线程池后续任务
                if (previousCtx != null) {
                    CtxUtil.restore(previousCtx);
                    CtxUtil.bindMdc();
                } else {
                    CtxUtil.reset();
                }

                if (previousMdc != null && !previousMdc.isEmpty()) {
                    MDC.setContextMap(previousMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

}
