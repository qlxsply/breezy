package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

import java.math.RoundingMode;

/**
 * 数字舍入模式，映射 {@link java.math.RoundingMode}。
 *
 * @author Corwin 2026/7/31
 */
public enum DecimalRoundingMode implements DictEnumDefinition {

    HALF_UP("四舍五入", RoundingMode.HALF_UP),
    HALF_DOWN("五舍六入", RoundingMode.HALF_DOWN),
    HALF_EVEN("银行家舍入", RoundingMode.HALF_EVEN),
    UP("远离零舍入", RoundingMode.UP),
    DOWN("趋向零舍入", RoundingMode.DOWN),
    CEILING("向上取整", RoundingMode.CEILING),
    FLOOR("向下取整", RoundingMode.FLOOR);

    private final String label;
    private final RoundingMode roundingMode;

    DecimalRoundingMode(String label, RoundingMode roundingMode) {
        this.label = label;
        this.roundingMode = roundingMode;
    }

    @Override
    public String label() {
        return label;
    }

    public RoundingMode toJava() {
        return roundingMode;
    }
}
