package com.corwin.system.methodstat.infrastructure.key;

import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.framework.constant.TextConstants;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/3/25
 */
@Component
public class StandaloneMethodStatKeyStrategy implements MethodStatKeyStrategy {

    @Override
    public MethodStatKey buildKey(String methodSignature) {
        String normalizedSignature = methodSignature == null ? TextConstants.EMPTY : methodSignature.trim();
        if (normalizedSignature.isEmpty()) {
            throw new IllegalArgumentException("methodSignature required");
        }
        return MethodStatKey.of(normalizedSignature);
    }

}
