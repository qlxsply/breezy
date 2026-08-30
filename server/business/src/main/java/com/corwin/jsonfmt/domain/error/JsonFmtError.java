package com.corwin.jsonfmt.domain.error;

import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.error.ErrorCodeRange;
import com.corwin.framework.error.ErrorCodeRanges;

/**
 * @author Corwin 2026/3/2
 */
public enum JsonFmtError implements ErrorCode {
  JSONFMT_RECORD_NOT_FOUND("102001", "记录不存在或无权限访问"),
  JSONFMT_RECORD_NAME_REQUIRED("102002", "记录名称不能为空"),
  JSONFMT_RECORD_NAME_TOO_LONG("102003", "记录名称长度超过限制"),
  JSONFMT_RECORD_CONTENT_REQUIRED("102004", "记录内容不能为空"),
  JSONFMT_RECORD_CONTENT_INVALID("102005", "记录内容不是合法 JSON"),
  JSONFMT_RECORD_ORDER_INVALID("102006", "记录排序参数非法"),
  JSONFMT_RECORD_DELETE_INVALID("102007", "记录删除参数非法"),
  ;

  private final ErrorCodeRange RANGE = ErrorCodeRanges.BREEZY_JSON_FMT;
  private final String code;
  private final String msg;

  JsonFmtError(String code, String msg) {
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
