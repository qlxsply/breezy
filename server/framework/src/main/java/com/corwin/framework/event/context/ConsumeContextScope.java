package com.corwin.framework.event.context;

/**
 * 消费上下文作用域。
 * <p>
 * 与 {@link ConsumeContextBinder} 配合使用，推荐在 try-with-resources 中使用，
 * 保障异步消费完成后总能恢复调用前线程上下文。
 *
 * @author Corwin 2026/4/9
 */
public interface ConsumeContextScope extends AutoCloseable {

    /**
     * 关闭作用域并恢复先前上下文。
     */
    @Override
    void close();
}

