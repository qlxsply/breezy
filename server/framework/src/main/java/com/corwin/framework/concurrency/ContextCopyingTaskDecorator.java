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
 * {@link TaskDecorator} that propagates {@link Ctx} and MDC context across thread pool boundaries.
 * <p>
 * Captures a snapshot of the submitting thread's context (Ctx + MDC) and restores it
 * in the worker thread before execution. After execution, the worker thread's original
 * context (or a cleared state) is restored to avoid cross-request pollution.
 * <p>
 * This is required because thread-pool threads are reused and
 * {@link InheritableThreadLocal} only propagates context at thread creation time.
 *
 * @author Corwin 2026/3/30
 */
public class ContextCopyingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        // 1. Capture context snapshot from the submitting thread
        Ctx parentCtx = CtxUtil.snapshot();
        String parentSpanIdAtSubmit = CtxUtil.getSpanId();
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        return () -> {
            // 2. Save the worker thread's current context (may be polluted from a previous task)
            Ctx previousCtx = CtxUtil.snapshot();
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();

            try {
                // 3. Restore the parent thread Ctx
                CtxUtil.restore(parentCtx);

                // 4. Create a new spanId at the async boundary, preserving the parent spanId
                String traceId = CtxUtil.getTraceId();
                if (traceId != null) {
                    CtxUtil.setSpanInfo(TraceGenerator.newSpanId(), parentSpanIdAtSubmit);
                }

                // 5. Bind MDC based on the current Ctx
                CtxUtil.bindMdc();

                // 6. Copy additional parent MDC fields (avoid overwriting the new spanId)
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
                // 7. Restore the worker thread's original context to avoid polluting subsequent tasks
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
