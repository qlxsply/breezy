package com.corwin.clinic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
@Getter
@Entity
@Table(name = "clinic_item_ledger", indexes = {
        @Index(name = "idx_clinic_ledger_sku", columnList = "sku_id"),
        @Index(name = "idx_clinic_ledger_type", columnList = "biz_type"),
        @Index(name = "idx_clinic_ledger_occurred", columnList = "occurred_at")
})
public class ItemLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biz_type", nullable = false, length = 16)
    private LedgerBizType bizType;

    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    @Column(name = "biz_line_id", nullable = false)
    private Long bizLineId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal qty;

    @Column(nullable = false, length = 32)
    private String unit;

    @Column(name = "amount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountTotal;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(length = 512)
    private String remark;

    protected ItemLedger() {
    }

    public static ItemLedger create(Long skuId, LedgerBizType bizType, Long bizId, Long bizLineId,
            LocalDate occurredAt, BigDecimal qty, String unit, BigDecimal amountTotal, Long supplierId,
            String remark) {
        ItemLedger ledger = new ItemLedger();
        ledger.skuId = skuId;
        ledger.bizType = bizType;
        ledger.bizId = bizId;
        ledger.bizLineId = bizLineId;
        ledger.occurredAt = occurredAt;
        ledger.qty = qty;
        ledger.unit = unit;
        ledger.amountTotal = amountTotal;
        ledger.supplierId = supplierId;
        ledger.remark = remark;
        return ledger;
    }
}
