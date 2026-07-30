package com.corwin.system.config.application.validation;

import com.corwin.framework.config.StoredConfig;

/**
 * Strategy interface for validating configuration values.
 * Implementations determine which config types they support and
 * perform specific validation logic on the raw input value.
 *
 * @author Corwin 2026/1/31
 */
public interface ConfigValueValidator {

    /**
     * Determine whether this validator supports the given configuration.
     *
     * @param config the stored configuration to check
     * @return true if this validator can validate the configuration
     */
    boolean supports(StoredConfig config);

    /**
     * Validate the raw value for the given configuration.
     *
     * @param config  the stored configuration to validate against
     * @param rawValue the raw value to validate
     * @throws com.corwin.framework.error.BizException if validation fails
     */
    void validate(StoredConfig config, String rawValue);

}
