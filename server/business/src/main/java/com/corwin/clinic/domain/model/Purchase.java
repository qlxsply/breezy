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
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
@Getter
@Entity
@Table(name = "clinic_purchase", indexes = {
        @Index(name = "idx_clinic_purchase_supplier", columnList = "supplier_id"),
        @Index(name = "idx_clinic_purchase_date", columnList = "purchased_at")
})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 512)
    private String remark;

    protected Purchase() {
    }

    public static Purchase create(Long supplierId, LocalDate purchasedAt, BigDecimal totalAmount, String remark) {
        Purchase purchase = new Purchase();
        purchase.supplierId = supplierId;
        purchase.purchasedAt = purchasedAt;
        purchase.totalAmount = totalAmount;
        purchase.remark = remark;
        return purchase;
    }
}
