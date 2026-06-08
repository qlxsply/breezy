package com.corwin.system.notify.application.port;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.notify.application.result.MessageDispatchResult;

/**
 * @author Corwin 2026/4/15
 */
public interface MessageDispatchPort {

    MessageDispatchResult dispatch(Long userId, UserType userType, MsgType type, String title, String content,
            String fallbackRoute);

    MessageDispatchResult dispatch(Long userId, UserType userType, MsgType type, String title, String content,
            String fallbackRoute, String bizType, String bizId);
}
