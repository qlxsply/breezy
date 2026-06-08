package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户功能组成员
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_normal_feature_group_member", indexes = {
        @Index(name = "idx_sys_normal_feature_group_member_unique", columnList = "group_id,user_id", unique = true),
        @Index(name = "idx_sys_normal_feature_group_member_group_id", columnList = "group_id"),
        @Index(name = "idx_sys_normal_feature_group_member_user_id", columnList = "user_id")})
public class NormalFeatureGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NormalFeatureGroupMember() {
    }

    public NormalFeatureGroupMember(Long groupId, Long userId, Long operator) {
        this.groupId = groupId;
        this.userId = userId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}
