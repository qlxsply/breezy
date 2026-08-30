package com.corwin.system.notify.application.port;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.result.MessageDispatchResult;
import com.corwin.system.notify.published.MsgType;

/**
 * Outbound port for dispatching messages to users.
 *
 * @author Corwin 2026/4/15
 */
public interface MessageDispatchPort {

  /**
   * Dispatches a message to the specified user.
   *
   * @param userId the target user ID
   * @param userType the target user type
   * @param type the message type
   * @param title the message title
   * @param content the message content
   * @param fallbackRoute the fallback front-end route
   * @return the dispatch result
   */
  MessageDispatchResult dispatch(
      Long userId,
      UserType userType,
      MsgType type,
      String title,
      String content,
      String fallbackRoute);

  /**
   * Dispatches a message with optional business context.
   *
   * @param userId the target user ID
   * @param userType the target user type
   * @param type the message type
   * @param title the message title
   * @param content the message content
   * @param fallbackRoute the fallback front-end route
   * @param bizType the business type (optional)
   * @param bizId the business ID (optional)
   * @return the dispatch result
   */
  MessageDispatchResult dispatch(
      Long userId,
      UserType userType,
      MsgType type,
      String title,
      String content,
      String fallbackRoute,
      String bizType,
      String bizId);
}
