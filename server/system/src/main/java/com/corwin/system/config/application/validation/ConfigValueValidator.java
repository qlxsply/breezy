package com.corwin.system.config.application.validation;

import com.corwin.framework.config.StoredConfig;

/**
 * @author Corwin 2026/1/31
 */
public interface ConfigValueValidator {

    boolean supports(StoredConfig config);

    void validate(StoredConfig config, String rawValue);

}
