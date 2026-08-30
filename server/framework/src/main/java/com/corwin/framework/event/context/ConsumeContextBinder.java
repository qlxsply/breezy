package com.corwin.framework.event.context;

import com.corwin.framework.event.model.EventCtxSnapshot;

/**
 * Binds the consume context from {@link EventCtxSnapshot} to the current consumer thread.
 *
 * <p>Returns a {@link ConsumeContextScope} that restores the original context when closed. All
 * transports share this single context-restore abstraction.
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
