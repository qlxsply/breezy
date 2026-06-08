package com.corwin.framework.event.context;

import com.corwin.framework.event.model.EventCtxSnapshot;
import com.corwin.framework.web.ctx.Ctx;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.ctx.TraceGenerator;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;

/**
 * 默认消费上下文绑定器。
 * <p>
 * 该实现会在消费前保存当前线程上下文与 MDC，再根据事件快照恢复消费上下文，
 * 并在作用域关闭时完整回滚，避免异步线程复用导致上下文污染。
 *
 * @author Corwin 2026/4/9
 */
public class DefaultConsumeContextBinder implements ConsumeContextBinder {

    private final ConsumeSpanMode spanMode;

    public DefaultConsumeContextBinder(ConsumeSpanMode spanMode) {
        this.spanMode = Objects.requireNonNull(spanMode, "spanMode required");
    }

    /**
     * 绑定消费上下文，并返回可恢复前置上下文的作用域句柄。
     */
    @Override
    public ConsumeContextScope bind(EventCtxSnapshot snapshot) {
        Ctx previousCtx = CtxUtil.snapshot();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        bindCurrentContext(snapshot);
        return () -> restorePreviousContext(previousCtx, previousMdc);
    }

    /**
     * 将快照内容绑定到当前线程上下文。
     */
    private void bindCurrentContext(EventCtxSnapshot snapshot) {
        if (snapshot == null) {
            CtxUtil.reset();
            CtxUtil.bindMdc();
            return;
        }
        CtxUtil.restore(snapshot.toCtx());
        if (spanMode == ConsumeSpanMode.NEW_CHILD && snapshot.traceId() != null && !snapshot.traceId().isBlank()) {
            CtxUtil.setSpanInfo(TraceGenerator.newSpanId(), snapshot.spanId());
        }
        CtxUtil.bindMdc();
    }

    /**
     * 恢复调用绑定器前的线程上下文和 MDC。
     */
    private void restorePreviousContext(Ctx previousCtx, Map<String, String> previousMdc) {
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
}

