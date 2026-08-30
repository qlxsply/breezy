package com.corwin.framework.event.context;

/**
 * AutoCloseable scope for consume-context restoration.
 *
 * <p>Use with try-with-resources to guarantee the thread context is restored after asynchronous
 * consumption completes.
 *
 * @author Corwin 2026/4/9
 */
public interface ConsumeContextScope extends AutoCloseable {

  /** Closes the scope and restores the previous context. */
  @Override
  void close();
}
