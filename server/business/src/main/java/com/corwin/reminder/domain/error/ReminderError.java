package com.corwin.reminder.domain.error;

import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.error.ErrorCodeRange;
import com.corwin.framework.error.ErrorCodeRanges;

/**
 * @author Corwin 2026/3/12
 */
public enum ReminderError implements ErrorCode {
  TODO_NOT_FOUND("103001", "待处理事项不存在"),
  TODO_CONTENT_REQUIRED("103002", "待处理事项内容不能为空"),
  TODO_STATUS_TRANSITION_INVALID("103003", "待处理事项状态流转非法"),
  TODO_ATTACHMENT_NOT_FOUND("103004", "待处理事项附件不存在"),
  TODO_ATTACHMENT_IMAGE_ONLY("103005", "待处理事项仅支持图片附件"),
  TODO_TEXT_TOO_LONG("103006", "事项内容/备注不能超过2000字符"),
  ;

  private final ErrorCodeRange RANGE = ErrorCodeRanges.BREEZY_REMINDER;
  private final String code;
  private final String msg;

  ReminderError(String code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getMsg() {
    return msg;
  }

  @Override
  public ErrorCodeRange getRange() {
    return RANGE;
  }
}
