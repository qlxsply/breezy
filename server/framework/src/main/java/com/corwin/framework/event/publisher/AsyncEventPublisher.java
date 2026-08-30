package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;
import java.time.Duration;
import java.time.Instant;

/**
 * Entry point for publishing async events.
 *
 * <p>Business code should depend on this interface only, not on the underlying transport or
 * message-queue client. The implementation handles envelope creation, context snapshotting, and
 * delivery.
 *
 * @author Corwin 2026/3/31
 */
public interface AsyncEventPublisher {

  /**
   * 发布事件，使用默认 source（若配置）或空来源。
   *
   * @param event 事件负载
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publish(T event);

  /**
   * 发布事件并显式指定事件来源。
   *
   * @param event 事件负载
   * @param source 事件来源标识，常用于消费端 sources 过滤
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publish(T event, String source);

  /**
   * 发布延时事件，指定绝对投递时间。
   *
   * @param event 事件负载
   * @param deliverAt 允许进入投递链路的绝对时间
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publishAt(T event, Instant deliverAt);

  /**
   * 发布延时事件并显式指定事件来源，使用绝对投递时间。
   *
   * @param event 事件负载
   * @param deliverAt 允许进入投递链路的绝对时间
   * @param source 事件来源标识
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publishAt(T event, Instant deliverAt, String source);

  /**
   * 发布延时事件，延后指定时长投递。
   *
   * @param event 事件负载
   * @param delay 延迟时长（非负）
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publishAfter(T event, Duration delay);

  /**
   * 发布延时事件并显式指定事件来源，延后指定时长投递。
   *
   * @param event 事件负载
   * @param delay 延迟时长（非负）
   * @param source 事件来源标识
   * @param <T> 事件类型
   */
  <T extends AsyncEvent> void publishAfter(T event, Duration delay, String source);
}
