package com.corwin.datasource.infrastructure.crypto;

import com.corwin.datasource.application.port.SecretCodec;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/1/11
 */
@Component
public class NoopSecretCodec implements SecretCodec {
  @Override
  public String encode(String raw) {
    return raw;
  }

  @Override
  public String decode(String enc) {
    return enc;
  }
}
