package com.corwin.framework.error;

/**
 * Built-in error-code ranges for framework and Breezy business modules.
 *
 * @author Corwin 2026/3/30
 */
public enum ErrorCodeRanges implements ErrorCodeRange {
  FRAMEWORK_BASE("000000", "009999"),
  FRAMEWORK_XSQL("010000", "019999"),
  FRAMEWORK_CACHE("020000", "029999"),
  BREEZY_SYSTEM_AUTH("100000", "100999"),
  BREEZY_DATASOURCE("101000", "101999"),
  BREEZY_JSON_FMT("102000", "102999"),
  BREEZY_REMINDER("103000", "103999"),
  ;

  private final String start;
  private final String end;

  ErrorCodeRanges(String start, String end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public String getStart() {
    return start;
  }

  @Override
  public String getEnd() {
    return end;
  }
}
