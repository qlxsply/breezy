package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * 事件信封构造器。
 * <p>
 * 负责将业务事件对象转换为统一的 {@code AsyncEventEnvelope}，并补齐
 * 事件 ID、事件发生时间、上下文快照、来源与生产者信息等元数据。
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

