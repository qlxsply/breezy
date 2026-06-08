package com.corwin.framework.event.context;

import com.corwin.framework.event.model.EventCtxSnapshot;

/**
 * 消费上下文绑定器。
 * <p>
 * 负责在消费线程中将事件信封里的 {@link EventCtxSnapshot} 恢复为当前线程上下文，
 * 并返回一个可关闭作用域用于在消费结束后恢复原上下文。
 * 不同 transport 通过该抽象复用同一套上下文恢复逻辑。
 *
 * @author Corwin 2026/4/9
 */
public interface ConsumeContextBinder {

    /**
     * 绑定事件上下文快照到当前线程。
     *
     * @param snapshot 事件携带的上下文快照，可为空
     * @return 可关闭作用域，关闭时应恢复绑定前上下文
     */
    ConsumeContextScope bind(EventCtxSnapshot snapshot);
}

