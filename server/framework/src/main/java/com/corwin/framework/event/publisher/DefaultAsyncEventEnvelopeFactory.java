package com.corwin.framework.event.publisher;

import com.corwin.framework.event.config.AsyncEventProperties;
import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.model.EventCtxSnapshot;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.ctx.CtxUtil;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Default {@link AsyncEventEnvelopeFactory} implementation following the V2 unified model.
 * <p>
 * When building an envelope, this implementation:
 * <ul>
 *     <li>Captures the current thread context snapshot for traceability.</li>
 *     <li>Uses mock time for the event timestamp (consistent with project conventions).</li>
 *     <li>Resolves source via "explicit source → default source → null".</li>
 *     <li>Resolves producer via "configured producerService → spring.application.name".</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public class DefaultAsyncEventEnvelopeFactory implements AsyncEventEnvelopeFactory {

    private final AsyncEventProperties properties;
    private final String applicationName;

    public DefaultAsyncEventEnvelopeFactory(AsyncEventProperties properties, Environment environment) {
        this.properties = Objects.requireNonNull(properties, "properties required");
        Objects.requireNonNull(environment, "environment required");
        String candidate = environment.getProperty("spring.application.name");
        this.applicationName = StringUtils.hasText(candidate) ? candidate.trim() : "application";
    }

    /**
     * 将业务事件转换为统一信封对象。
     *
     * @param event 业务事件
     * @param source 事件来源（可为空）
     * @param <T> 事件类型
     * @return 可直接交给 transport 发布的信封
     */
    @Override
    public <T extends AsyncEvent> AsyncEventEnvelope<T> create(T event, String source) {
        return create(event, source, HighDate.mockTimestampMillis());
    }

    /**
     * 将业务事件转换为统一信封对象，并显式指定可投递时间。
     *
     * @param event 业务事件
     * @param source 事件来源（可为空）
     * @param deliverAtMillis 可投递时间戳（epoch millis）
     * @param <T> 事件类型
     * @return 可直接交给 transport 发布的信封
     */
    @Override
    public <T extends AsyncEvent> AsyncEventEnvelope<T> create(T event, String source, long deliverAtMillis) {
        Objects.requireNonNull(event, "event required");
        EventCtxSnapshot ctxSnapshot = EventCtxSnapshot.from(CtxUtil.snapshot());
        String finalSource = resolveSource(source);
        long occurredAtMillis = HighDate.mockTimestampMillis();
        long finalDeliverAtMillis = deliverAtMillis <= 0L ? occurredAtMillis : deliverAtMillis;
        return new AsyncEventEnvelope<>(UUID.randomUUID().toString(), event.getClass().getName(),
                occurredAtMillis, finalDeliverAtMillis, ctxSnapshot, event, finalSource, resolveProducerService(),
                null, Map.of());
    }

    /**
     * 按优先级解析事件来源：方法参数 > 配置默认值 > 空。
     */
    private String resolveSource(String source) {
        if (StringUtils.hasText(source)) {
            return source.trim();
        }
        String configured = properties.getPublish().getDefaultSource();
        return StringUtils.hasText(configured) ? configured.trim() : null;
    }

    /**
     * 解析生产者服务标识，优先使用显式配置，缺省回退为应用名。
     */
    private String resolveProducerService() {
        String configured = properties.getPublish().getProducerService();
        return StringUtils.hasText(configured) ? configured.trim() : applicationName;
    }
}

