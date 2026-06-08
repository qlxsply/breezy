package com.corwin.clinic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * @author Corwin 2026/2/8
 */
@Getter
@Entity
@Table(name = "clinic_supplier",
        indexes = @Index(name = "idx_clinic_supplier_name", columnList = "name"),
        uniqueConstraints = @UniqueConstraint(name = "uk_clinic_supplier_name_norm", columnNames = "name_norm"))
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "name_norm", nullable = false, length = 128)
    private String nameNorm;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 512)
    private String remark;

    protected Supplier() {
    }

    public static Supplier create(String name, String nameNorm, String remark) {
        Supplier supplier = new Supplier();
        supplier.name = name;
        supplier.nameNorm = nameNorm;
        supplier.enabled = true;
        supplier.remark = remark;
        return supplier;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }
}
