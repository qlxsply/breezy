package com.corwin.system.dict.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;

/**
 * JPA entity representing a dictionary type.
 * <p>A dictionary type defines a logical group of dictionary items, including its value type, structure type, source type, and enabled status.</p>
 *
 * @author Corwin 2026/3/15
 */
@Getter
@Entity
@Table(name = "sys_dict_type",
        uniqueConstraints = {@UniqueConstraint(name = "uk_sys_dict_type_code", columnNames = {"code"})},
        indexes = {@Index(name = "idx_sys_dict_type_enabled", columnList = "enabled"),
                @Index(name = "idx_sys_dict_type_source", columnList = "source_type")})
public class DictType {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 128)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "enum_class", length = 255)
    private String enumClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 32)
    private DictValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", nullable = false, length = 32)
    private DictStructureType structureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private DictSourceType sourceType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 64)
    private String updatedBy;

    protected DictType() {
    }

    /**
     * Constructs a new DictType entity with the given attributes.
     */
    public DictType(String id, String code, String name, String description, String enumClass, DictValueType valueType,
            DictStructureType structureType, DictSourceType sourceType,
            boolean enabled, String operator) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.enumClass = enumClass;
        this.valueType = valueType;
        this.structureType = structureType;
        this.sourceType = sourceType;
        this.enabled = enabled;
        this.createdAt = HighDate.mockInstant();
        this.updatedAt = this.createdAt;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    /**
     * Updates the mutable fields of this dictionary type.
     */
    public void update(String name, String description, String enumClass, DictValueType valueType,
            DictStructureType structureType, boolean enabled,
            String operator) {
        this.name = name;
        this.description = description;
        this.enumClass = enumClass;
        this.valueType = valueType;
        this.structureType = structureType;
        this.enabled = enabled;
        touch(operator);
    }

    /**
     * Enables this dictionary type.
     */
    public void enable(String operator) {
        this.enabled = true;
        touch(operator);
    }

    /**
     * Disables this dictionary type.
     */
    public void disable(String operator) {
        this.enabled = false;
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
