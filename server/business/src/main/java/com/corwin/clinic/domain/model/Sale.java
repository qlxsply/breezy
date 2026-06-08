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
@Table(name = "clinic_sale", indexes = {
        @Index(name = "idx_clinic_sale_date", columnList = "sold_at")
})
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sold_at", nullable = false)
    private LocalDate soldAt;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(length = 512)
    private String remark;

    protected Sale() {
    }

    public static Sale create(LocalDate soldAt, BigDecimal totalAmount, String customerName, String remark) {
        Sale sale = new Sale();
        sale.soldAt = soldAt;
        sale.totalAmount = totalAmount;
        sale.customerName = customerName;
        sale.remark = remark;
        return sale;
    }
}
