package com.corwin.framework.web.ctx;

import java.security.SecureRandom;

/**
 * Trace / Span ID generator using Base32 (no separators, URL-safe).
 *
 * <p>Characteristics:
 *
 * <ul>
 *   <li>Fixed length
 *   <li>High randomness ({@link SecureRandom})
 *   <li>Lowercase letters + digits only (ambiguous characters excluded)
 * </ul>
 *
 * @author Corwin
 */
public final class TraceGenerator {

  /** Ambiguous characters removed from the digit set: 0 o l i */
  private static final char[] DIGITS = "123456789abcdefghjkmnpqrstuvwxyz".toCharArray(); // 32 chars

  private static final SecureRandom RANDOM = new SecureRandom();

  private TraceGenerator() {}

  /** Generates a new trace ID (default length: 16 characters). */
  public static String newTraceId() {
    return generate(16);
  }

  /** Generates a new span ID (default length: 8 characters). */
  public static String newSpanId() {
    return generate(8);
  }

  /**
   * Generates a fixed-length random string using Base32 (5 bits per character).
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Each character encodes 5 bits (32-character alphabet)
   *   <li>Uses the DIGITS array (must have length 32)
   *   <li>Backed by {@link SecureRandom}, suitable for traceId / spanId
   *   <li>Fixed output length, no padding characters
   * </ul>
   *
   * <p>Algorithm outline:
   *
   * <pre>
   * 1. Compute required bits: outLen * 5
   * 2. Convert to bytes: ceil(bits / 8)
   * 3. Slice the random byte stream into 5-bit chunks, map to DIGITS
   * </pre>
   *
   * <p>Example:
   *
   * <pre>
   * outLen = 16
   * bits needed = 16 * 5 = 80 bits
   * bytes needed = ceil(80 / 8) = 10 bytes
   * </pre>
   *
   * @param outLen the desired output length in characters (&gt;0, recommended 8 / 16 / 32)
   * @return a fixed-length random string
   */
  public static String generate(int outLen) {

    if (outLen <= 0) {
      throw new IllegalArgumentException("outLen must be > 0");
    }

    // ===== 1. calculate the number of random bytes needed =====
    // Each character encodes 5 bits → total bits = outLen * 5
    // Round up to the nearest byte (8 bits):
    //   bytesNeeded = ceil(outLen * 5 / 8)
    // Bit-level optimisation to avoid floating-point:
    //   (x + 7) >> 3 is equivalent to ceil(x / 8)
    int bytesNeeded = (outLen * 5 + 7) >> 3;

    byte[] randomBytes = new byte[bytesNeeded];
    RANDOM.nextBytes(randomBytes);

    char[] buf = new char[outLen];

    // ===== 2. bit buffer =====
    int bitBuffer = 0; // accumulated bits
    int bitsInBuffer = 0; // number of valid bits in the buffer
    int charPos = 0; // number of characters generated

    // ===== 3. slice into 5-bit chunks and map to characters =====
    for (byte b : randomBytes) {

      // append 1 byte (8 bits) to the buffer
      bitBuffer = (bitBuffer << 8) | (b & 0xFF);
      bitsInBuffer += 8;

      // extract 5 bits at a time from the buffer
      while (bitsInBuffer >= 5 && charPos < outLen) {

        bitsInBuffer -= 5;

        // take the high-order 5 bits
        int index = (bitBuffer >>> bitsInBuffer) & 0x1F; // 0x1F = 31 = 11111b

        buf[charPos++] = DIGITS[index];
      }

      if (charPos >= outLen) {
        break;
      }
    }

    return new String(buf);
  }
}
