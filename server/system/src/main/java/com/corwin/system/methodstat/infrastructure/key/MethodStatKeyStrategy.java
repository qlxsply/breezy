package com.corwin.system.methodstat.infrastructure.key;

import com.corwin.system.methodstat.domain.model.MethodStatKey;

/**
 * Strategy interface for building a {@link MethodStatKey} from a method signature string.
 * @author Corwin 2026/3/25
 */
public interface MethodStatKeyStrategy {

    /**
     * Build a method statistics key from a canonical method signature.
     * @param methodSignature the method signature string
     * @return the constructed key
     */
    MethodStatKey buildKey(String methodSignature);

}
