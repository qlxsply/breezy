package com.corwin.system.methodstat.infrastructure.key;

import com.corwin.system.methodstat.domain.model.MethodStatKey;

/**
 * @author Corwin 2026/3/25
 */
public interface MethodStatKeyStrategy {

    MethodStatKey buildKey(String methodSignature);

}
