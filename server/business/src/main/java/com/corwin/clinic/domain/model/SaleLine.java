package com.corwin.clinic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/8
 */
@Getter
@Entity
@Table(name = "clinic_sale_line", indexes = {
        @Index(name = "idx_clinic_sale_line_sale", columnList = "sale_id"),
        @Index(name = "idx_clinic_sale_line_sku", columnList = "sku_id")
})
public class SaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal qty;

    @Column(nullable = false, length = 32)
    private String unit;

    @Column(name = "line_total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotalAmount;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(length = 512)
    private String remark;

    protected SaleLine() {
    }

    public static SaleLine create(Long saleId, Long skuId, BigDecimal qty, String unit,
            BigDecimal lineTotalAmount, BigDecimal unitPrice, String remark) {
        SaleLine line = new SaleLine();
        line.saleId = saleId;
        line.skuId = skuId;
        line.qty = qty;
        line.unit = unit;
        line.lineTotalAmount = lineTotalAmount;
        line.unitPrice = unitPrice;
        line.remark = remark;
        return line;
    }
}
