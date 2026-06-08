package com.corwin.clinic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "clinic_item_sku", indexes = {
        @Index(name = "idx_clinic_item_sku_spu", columnList = "spu_id"),
        @Index(name = "idx_clinic_item_sku_name", columnList = "display_name")
})
public class ItemSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spu_id", nullable = false)
    private Long spuId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 128)
    private String manufacturer;

    @Column(length = 128)
    private String spec;

    @Column(nullable = false)
    private boolean enabled;

    protected ItemSku() {
    }

    public static ItemSku create(Long spuId, String displayName, String manufacturer, String spec) {
        ItemSku sku = new ItemSku();
        sku.spuId = spuId;
        sku.displayName = displayName;
        sku.manufacturer = manufacturer;
        sku.spec = spec;
        sku.enabled = true;
        return sku;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }
}
