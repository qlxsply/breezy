package com.corwin.framework.config.error;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;

/**
 * @author Corwin 2026/7/31
 */
public class ConfigVersionConflictException extends BizException {

    public ConfigVersionConflictException(String configKey) {
        super("Config version conflict: " + configKey, BaseError.CONFLICT);
    }
}
