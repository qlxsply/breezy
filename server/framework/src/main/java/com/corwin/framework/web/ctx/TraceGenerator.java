package com.corwin.framework.web.ctx;

import java.security.SecureRandom;

/**
 * Trace / Span ID 生成器（Base32, 无分隔符, URL-safe）
 * <p>
 * 特点：
 * - 固定长度
 * - 高随机性（SecureRandom）
 * - 仅小写字母 + 数字（避免歧义字符）
 *
 * @author Corwin
 */
public final class TraceGenerator {

    /**
     * 去掉容易混淆的字符：
     * 0 o
     * l i
     */
    private static final char[] DIGITS = "123456789abcdefghjkmnpqrstuvwxyz".toCharArray(); // 32 个字符

    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceGenerator() {
    }

    /**
     * 生成 TraceId（默认 16 位）
     */
    public static String newTraceId() {
        return generate(16);
    }

    /**
     * 生成 SpanId（默认 8 位）
     */
    public static String newSpanId() {
        return generate(8);
    }

    /**
     * 使用 Base32（5bit/字符）生成定长随机字符串。
     *
     * <p>特点：
     * <ul>
     *   <li>每个字符占 5 bit（共 32 个字符）</li>
     *   <li>字符集为 DIGITS（需保证长度为 32）</li>
     *   <li>使用 SecureRandom，适用于 traceId / spanId</li>
     *   <li>输出长度固定，不包含填充字符</li>
     * </ul>
     *
     * <p>核心思路：
     * <pre>
     * 1. 计算所需 bit 数：outLen * 5
     * 2. 转换为 byte 数：ceil(bit / 8)
     * 3. 从随机字节流中按 5bit 切片，映射到 DIGITS
     * </pre>
     *
     * <p>示例：
     * <pre>
     * outLen = 16
     * 需要 bit = 16 * 5 = 80 bit
     * 需要 byte = ceil(80 / 8) = 10 byte
     * </pre>
     *
     * @param outLen 输出字符长度（>0，建议 8 / 16 / 32）
     * @return 固定长度随机字符串
     */
    public static String generate(int outLen) {

        if (outLen <= 0) {
            throw new IllegalArgumentException("outLen must be > 0");
        }

        // ===== 1. 计算所需随机字节数 =====
        // 每个字符 5bit，总 bit = outLen * 5
        // 向上取整到 byte（8bit）
        //
        // 等价写法：
        // int bytesNeeded = (int) Math.ceil(outLen * 5.0 / 8);
        //
        // 使用位运算优化（避免浮点）：
        // (x + 7) >> 3 等价于 ceil(x / 8)
        int bytesNeeded = (outLen * 5 + 7) >> 3;

        byte[] randomBytes = new byte[bytesNeeded];
        RANDOM.nextBytes(randomBytes);

        char[] buf = new char[outLen];

        // ===== 2. bit 缓冲区 =====
        int bitBuffer = 0;      // 累积 bit 的缓存
        int bitsInBuffer = 0;   // 当前缓存中有效 bit 数
        int charPos = 0;        // 已生成字符数

        // ===== 3. 按 5bit 切片生成字符 =====
        for (byte b : randomBytes) {

            // 将 1 byte（8bit）加入缓冲区
            bitBuffer = (bitBuffer << 8) | (b & 0xFF);
            bitsInBuffer += 8;

            // 每次从 buffer 中取 5bit
            while (bitsInBuffer >= 5 && charPos < outLen) {

                bitsInBuffer -= 5;

                // 取高位 5bit
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
