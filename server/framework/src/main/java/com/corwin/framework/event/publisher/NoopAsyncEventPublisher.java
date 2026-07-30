package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;

import java.time.Duration;
import java.time.Instant;

/**
 * No-op {@link AsyncEventPublisher} used as a fallback when the event component is disabled.
 * <p>
 * All publish calls succeed silently without triggering any transport.
 *
 * @author Corwin 2026/3/31
 */
public class NoopAsyncEventPublisher implements AsyncEventPublisher {

    /**
     * 空实现：忽略事件。
     */
    @Override
    public <T extends AsyncEvent> void publish(T event) {
    }

    /**
     * 空实现：忽略事件和来源。
     */
    @Override
    public <T extends AsyncEvent> void publish(T event, String source) {
    }

    /**
     * 空实现：忽略延时投递。
     */
    @Override
    public <T extends AsyncEvent> void publishAt(T event, Instant deliverAt) {
    }

    /**
     * 空实现：忽略带 source 的延时投递。
     */
    @Override
    public <T extends AsyncEvent> void publishAt(T event, Instant deliverAt, String source) {
    }

    /**
     * 空实现：忽略相对延时投递。
     */
    @Override
    public <T extends AsyncEvent> void publishAfter(T event, Duration delay) {
    }

    /**
     * 空实现：忽略带 source 的相对延时投递。
     */
    @Override
    public <T extends AsyncEvent> void publishAfter(T event, Duration delay, String source) {
    }
}
