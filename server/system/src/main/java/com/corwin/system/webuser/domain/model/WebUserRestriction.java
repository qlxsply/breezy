package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(name = "tb_user_restriction", indexes = {
        @Index(name = "idx_tb_user_restriction_user", columnList = "user_id"),
        @Index(name = "idx_tb_user_restriction_scope", columnList = "restriction_scope,start_at,end_at")
})
public class WebUserRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_type", nullable = false, length = 32)
    private WebUserRestrictionType restrictionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_scope", nullable = false, length = 16)
    private WebUserRestrictionScope restrictionScope;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected WebUserRestriction() {
    }

    public WebUserRestriction(Long userId, WebUserRestrictionType restrictionType,
            WebUserRestrictionScope restrictionScope, String reason, Instant endAt, String operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.restrictionType = restrictionType;
        this.restrictionScope = restrictionScope;
        this.reason = reason;
        this.startAt = now;
        this.endAt = endAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    public boolean activeAt(Instant at) {
        if (at.isBefore(startAt)) {
            return false;
        }
        return endAt == null || at.isBefore(endAt);
    }

    public void close(String operator) {
        this.endAt = HighDate.mockInstant();
        this.updatedAt = this.endAt;
        this.updatedBy = operator;
    }
}
