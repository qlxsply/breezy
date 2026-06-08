package com.corwin.framework.error;

/**
 * 错误码号段定义。
 * <p>
 * 约定：
 * - start / end 均为包含边界
 * - code 使用纯数字字符串，长度固定
 *
 * @author Corwin 2026/3/30
 */
public interface ErrorCodeRange {

    /**
     * 号段名称，便于测试报错时识别
     */
    String getName();

    /**
     * 起始编码（包含）
     */
    String getStart();

    /**
     * 结束编码（包含）
     */
    String getEnd();

    /**
     * 判断指定 code 是否在当前号段内
     */
    default boolean contains(String code) {
        if (code == null) {
            return false;
        }
        return code.compareTo(getStart()) >= 0 && code.compareTo(getEnd()) <= 0;
    }

}