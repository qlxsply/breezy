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

/**
 * @author Corwin 2026/2/8
 */
@Getter
@Entity
@Table(name = "clinic_item_spu", indexes = {
        @Index(name = "idx_clinic_item_spu_category", columnList = "category"),
        @Index(name = "idx_clinic_item_spu_name", columnList = "name")
})
public class ItemSpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemCategory category;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String remark;

    protected ItemSpu() {
    }

    public static ItemSpu create(ItemCategory category, String name, String remark) {
        ItemSpu spu = new ItemSpu();
        spu.category = category;
        spu.name = name;
        spu.remark = remark;
        return spu;
    }
}
