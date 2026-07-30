package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * A time-bound restriction applied to a web user account (e.g. login ban).
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(name = "tb_user_restriction", indexes = {
        @Index(name = "idx_tb_user_restriction_user", columnList = "user_id"),
        @Index(name = "idx_tb_user_restriction_scope", columnList = "restriction_scope,start_at,end_at")
})
public class WebUserRestriction {

    /** Primary key, auto-generated identity. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the restricted user. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Type of restriction: DISABLE_LOGIN or DISABLE_ALL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_type", nullable = false, length = 32)
    private WebUserRestrictionType restrictionType;

    /** Scope of the restriction: LOGIN or ALL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_scope", nullable = false, length = 16)
    private WebUserRestrictionScope restrictionScope;

    /** Reason for the restriction. */
    @Column(name = "reason", length = 255)
    private String reason;

    /** Timestamp when the restriction takes effect. */
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    /** Timestamp when the restriction expires (null = indefinite). */
    @Column(name = "end_at")
    private Instant endAt;

    /** Timestamp when the record was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Who created the record. */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    /** Timestamp when the record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Who last updated the record. */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    /** JPA required no-arg constructor. */
    protected WebUserRestriction() {
    }

    /**
     * Construct a new restriction starting immediately.
     */
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

    /**
     * Check whether this restriction is active at the given instant.
     */
    public boolean activeAt(Instant at) {
        if (at.isBefore(startAt)) {
            return false;
        }
        return endAt == null || at.isBefore(endAt);
    }

    /**
     * Close the restriction immediately by setting end_at to now.
     */
    public void close(String operator) {
        this.endAt = HighDate.mockInstant();
        this.updatedAt = this.endAt;
        this.updatedBy = operator;
    }
}
