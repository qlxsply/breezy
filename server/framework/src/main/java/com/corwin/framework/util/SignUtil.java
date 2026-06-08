package com.corwin.framework.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author wl0180 2026/2/12
 */

/**
 * @author Corwin 2026/3/30
 */
public class SignUtil {

    /**
     * 计算流的摘要
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

    public static String sha256(InputStream is) throws IOException {
        return digestHex("SHA-256", is);
    }

    public static String digestHex(String algorithm, String payload) {
        MessageDigest md = getDigest(algorithm);
        md.reset();
        byte[] bytes = md.digest(payload.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(bytes);
    }

    public static String md5(String payload) {
        return digestHex("MD5", payload);
    }

    public static String sha1(String payload) {
        return digestHex("SHA-1", payload);
    }

    public static String sha256(String payload) {
        return digestHex("SHA-256", payload);
    }

    public static String sha512(String payload) {
        return digestHex("SHA-512", payload);
    }

    public static String hmacHex(String algorithm, String payload, String secret) {
        try {
            Mac mac = getMac(algorithm);
            mac.reset();
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    public static String hmacSha256(String payload, String secret) {
        return hmacHex("HmacSHA256", payload, secret);
    }

    public static String hmacSha512(String payload, String secret) {
        return hmacHex("HmacSHA512", payload, secret);
    }

    // ------------------------ 内部方法 ------------------------

    private static final Map<String, ThreadLocal<MessageDigest>> DIGEST_CACHE = new ConcurrentHashMap<>();

    private static MessageDigest getDigest(String algorithm) {
        return DIGEST_CACHE.computeIfAbsent(algorithm, alg -> ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance(alg);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("不支持的算法: " + alg, e);
            }
        })).get();
    }

    private static final Map<String, ThreadLocal<Mac>> MAC_CACHE = new ConcurrentHashMap<>();

    private static Mac getMac(String algorithm) {
        return MAC_CACHE.computeIfAbsent(algorithm, alg -> ThreadLocal.withInitial(() -> {
            try {
                return Mac.getInstance(alg);
            } catch (Exception e) {
                throw new IllegalArgumentException("不支持的算法: " + alg, e);
            }
        })).get();
    }

    private static final HexFormat HEX = HexFormat.of();

    private SignUtil() {
    }

}
