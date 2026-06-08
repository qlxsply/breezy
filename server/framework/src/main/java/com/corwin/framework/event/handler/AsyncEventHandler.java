package com.corwin.framework.event.handler;

import com.corwin.framework.event.model.AsyncEvent;

/**
 * V1 接口式事件处理器模型（兼容保留）。
 * <p>
 * V2 推荐使用 {@code @AsyncEventListener} 注解监听，本接口保留用于历史代码迁移期。
 *
 * @author Corwin 2026/3/31
 */
public interface AsyncEventHandler<T extends AsyncEvent> {

    /**
     * 声明当前处理器支持的事件类型。
     */
    Class<T> eventType();

    /**
     * 处理事件。
     */
    void onEvent(T event);

    /**
     * 处理顺序，值越小优先级越高。
     */
    default int order() {
        return 0;
    }
}
