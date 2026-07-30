package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * Factory that converts a business {@link AsyncEvent} into a fully-populated
 * {@link AsyncEventEnvelope}, filling in event ID, timestamp, context snapshot,
 * source, and producer metadata.
 *
 * @author Corwin 2026/4/9
 */
public interface AsyncEventEnvelopeFactory {

    /**
     * 构建事件信封。
     *
     * @param event 业务事件对象
     * @param source 显式事件来源，可为空并由实现回退默认值
     * @param <T> 事件类型
     * @return 已标准化的事件信封
     */
    <T extends AsyncEvent> AsyncEventEnvelope<T> create(T event, String source);

    /**
     * 构建事件信封并指定可投递时间点。
     *
     * @param event 业务事件对象
     * @param source 显式事件来源，可为空并由实现回退默认值
     * @param deliverAtMillis 允许进入投递链路的时间戳（epoch millis）
     * @param <T> 事件类型
     * @return 已标准化的事件信封
     */
    <T extends AsyncEvent> AsyncEventEnvelope<T> create(T event, String source, long deliverAtMillis);
}

