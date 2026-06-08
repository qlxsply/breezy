package com.corwin.datasource.domain.error;

import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.error.ErrorCodeRange;
import com.corwin.framework.error.ErrorCodeRanges;

/**
 * @author Corwin 2026/2/26
 */
public enum DatabaseSourceError implements ErrorCode {
    MANAGED_DATABASE_DUPLICATE("101001", "数据库已经接管，无法重复创建");

    private final ErrorCodeRange RANGE = ErrorCodeRanges.BREEZY_DATASOURCE;
    private final String code;
    private final String msg;

    DatabaseSourceError(String code, String msg) {
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
