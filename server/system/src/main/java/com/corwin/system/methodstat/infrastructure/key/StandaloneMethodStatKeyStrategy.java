package com.corwin.system.methodstat.infrastructure.key;

import com.corwin.framework.constant.TextConstants;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link MethodStatKeyStrategy} that uses the method signature directly
 * as the key.
 *
 * @author Corwin 2026/3/25
 */
@Component
public class StandaloneMethodStatKeyStrategy implements MethodStatKeyStrategy {

  /**
   * Build a key by normalizing and using the method signature string directly.
   *
   * @param methodSignature the method signature string
   * @return the constructed key
   */
  @Override
  public MethodStatKey buildKey(String methodSignature) {
    String normalizedSignature =
        methodSignature == null ? TextConstants.EMPTY : methodSignature.trim();
    if (normalizedSignature.isEmpty()) {
      throw new IllegalArgumentException("methodSignature required");
    }
    return MethodStatKey.of(normalizedSignature);
  }
}
