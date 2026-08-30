package com.corwin.datasource.application.port;

/**
 * @author Corwin 2026/1/11
 */
public interface SecretCodec {
  String encode(String raw);

  String decode(String enc);
}
