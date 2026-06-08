package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户功能组
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_normal_feature_group",
        indexes = {@Index(name = "idx_sys_normal_feature_group_code", columnList = "code", unique = true),
                @Index(name = "idx_sys_normal_feature_group_type", columnList = "group_type"),
                @Index(name = "idx_sys_normal_feature_group_enabled", columnList = "enabled")})
public class NormalFeatureGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 16)
    private NormalFeatureGroupType groupType;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "default_group", nullable = false)
    private Boolean defaultGroup;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NormalFeatureGroup() {
    }

    public NormalFeatureGroup(String code, String name, NormalFeatureGroupType groupType, String description,
            Boolean defaultGroup, Boolean systemBuiltin, Long operator) {
        Instant now = HighDate.mockInstant();
        this.code = code;
        this.name = name;
        this.groupType = groupType;
        this.description = description;
        this.enabled = true;
        this.defaultGroup = defaultGroup;
        this.systemBuiltin = systemBuiltin;
        this.createdBy = operator;
        this.createdAt = now;
        this.updatedBy = operator;
        this.updatedAt = now;
    }

    public void enable(Long operator) {
        this.enabled = true;
        touch(operator);
    }

    public void disable(Long operator) {
        this.enabled = false;
        touch(operator);
    }

    public void update(String code, String name, NormalFeatureGroupType groupType, String description,
            Boolean defaultGroup, Long operator) {
        this.code = code;
        this.name = name;
        this.groupType = groupType;
        this.description = description;
        this.defaultGroup = defaultGroup;
        touch(operator);
    }

    private void touch(Long operator) {
        this.updatedBy = operator;
        this.updatedAt = HighDate.mockInstant();
    }
}
