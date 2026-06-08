package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户功能组授权
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_normal_feature_group_grant", indexes = {
        @Index(name = "idx_sys_normal_feature_group_grant_unique", columnList = "group_id,feature_id", unique = true),
        @Index(name = "idx_sys_normal_feature_group_grant_group_id", columnList = "group_id"),
        @Index(name = "idx_sys_normal_feature_group_grant_feature_id", columnList = "feature_id")})
public class NormalFeatureGroupGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NormalFeatureGroupGrant() {
    }

    public NormalFeatureGroupGrant(Long groupId, Long featureId, Long operator) {
        this.groupId = groupId;
        this.featureId = featureId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}
