package com.corwin.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for computing message digests and HMACs.
 *
 * <p>Provides convenient methods for:
 *
 * <ul>
 *   <li>Digest algorithms: MD5, SHA-1, SHA-256, SHA-512
 *   <li>HMAC algorithms: HmacSHA256, HmacSHA512
 *   <li>Stream-based and string-based digest computation
 * </ul>
 *
 * <p>Digest and MAC instances are cached per-thread via {@link ThreadLocal} for reuse, avoiding
 * repeated allocation overhead.
 *
 * @author Corwin 2026/3/30
 */
public class SignUtil {

  /**
   * Computes the digest of an input stream using the given algorithm.
   *
   * @param algorithm the digest algorithm (e.g. "SHA-256")
   * @param is the input stream to read
   * @return the hex-encoded digest string
   * @throws IOException if an I/O error occurs while reading the stream
   */
  public static String digestHex(String algorithm, InputStream is) throws IOException {
    MessageDigest md = getDigest(algorithm);
    md.reset();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = is.read(buffer)) != -1) {
      md.update(buffer, 0, read);
    }
    return HEX.formatHex(md.digest());
  }

  /**
   * Computes the SHA-256 digest of an input stream.
   *
   * @param is the input stream to read
   * @return the hex-encoded SHA-256 digest
   * @throws IOException if an I/O error occurs
   */
  public static String sha256(InputStream is) throws IOException {
    return digestHex("SHA-256", is);
  }

  /**
   * Computes the digest of a string payload using the given algorithm.
   *
   * @param algorithm the digest algorithm (e.g. "MD5", "SHA-256")
   * @param payload the string to digest
   * @return the hex-encoded digest string
   */
  public static String digestHex(String algorithm, String payload) {
    MessageDigest md = getDigest(algorithm);
    md.reset();
    byte[] bytes = md.digest(payload.getBytes(StandardCharsets.UTF_8));
    return HEX.formatHex(bytes);
  }

  /**
   * Computes the MD5 digest of a string payload.
   *
   * @param payload the string to digest
   * @return the hex-encoded MD5 digest
   */
  public static String md5(String payload) {
    return digestHex("MD5", payload);
  }

  /**
   * Computes the SHA-1 digest of a string payload.
   *
   * @param payload the string to digest
   * @return the hex-encoded SHA-1 digest
   */
  public static String sha1(String payload) {
    return digestHex("SHA-1", payload);
  }

  /**
   * Computes the SHA-256 digest of a string payload.
   *
   * @param payload the string to digest
   * @return the hex-encoded SHA-256 digest
   */
  public static String sha256(String payload) {
    return digestHex("SHA-256", payload);
  }

  /**
   * Computes the SHA-512 digest of a string payload.
   *
   * @param payload the string to digest
   * @return the hex-encoded SHA-512 digest
   */
  public static String sha512(String payload) {
    return digestHex("SHA-512", payload);
  }

  /**
   * Computes an HMAC digest using the given algorithm, payload, and secret.
   *
   * @param algorithm the HMAC algorithm (e.g. "HmacSHA256")
   * @param payload the message to authenticate
   * @param secret the shared secret key
   * @return the hex-encoded HMAC digest
   * @throws IllegalStateException if the HMAC computation fails
   */
  public static String hmacHex(String algorithm, String payload, String secret) {
    try {
      Mac mac = getMac(algorithm);
      mac.reset();
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
      byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(result);
    } catch (Exception e) {
      throw new IllegalStateException("HMAC computation failed", e);
    }
  }

  /**
   * Computes an HmacSHA256 digest.
   *
   * @param payload the message to authenticate
   * @param secret the shared secret key
   * @return the hex-encoded HmacSHA256 digest
   */
  public static String hmacSha256(String payload, String secret) {
    return hmacHex("HmacSHA256", payload, secret);
  }

  /**
   * Computes an HmacSHA512 digest.
   *
   * @param payload the message to authenticate
   * @param secret the shared secret key
   * @return the hex-encoded HmacSHA512 digest
   */
  public static String hmacSha512(String payload, String secret) {
    return hmacHex("HmacSHA512", payload, secret);
  }

  // ------------------------ Internal utilities ------------------------

  private static final Map<String, ThreadLocal<MessageDigest>> DIGEST_CACHE =
      new ConcurrentHashMap<>();

  private static MessageDigest getDigest(String algorithm) {
    return DIGEST_CACHE
        .computeIfAbsent(
            algorithm,
            alg ->
                ThreadLocal.withInitial(
                    () -> {
                      try {
                        return MessageDigest.getInstance(alg);
                      } catch (NoSuchAlgorithmException e) {
                        throw new IllegalArgumentException("Unsupported algorithm: " + alg, e);
                      }
                    }))
        .get();
  }

  private static final Map<String, ThreadLocal<Mac>> MAC_CACHE = new ConcurrentHashMap<>();

  private static Mac getMac(String algorithm) {
    return MAC_CACHE
        .computeIfAbsent(
            algorithm,
            alg ->
                ThreadLocal.withInitial(
                    () -> {
                      try {
                        return Mac.getInstance(alg);
                      } catch (Exception e) {
                        throw new IllegalArgumentException("Unsupported algorithm: " + alg, e);
                      }
                    }))
        .get();
  }

  private static final HexFormat HEX = HexFormat.of();

  private SignUtil() {}
}
