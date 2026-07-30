package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * An auditable lifecycle event recorded for a web user (login, logout, password change, etc.).
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(name = "tb_user_lifecycle_event", indexes = {
        @Index(name = "idx_tb_user_event_user", columnList = "user_id"),
        @Index(name = "idx_tb_user_event_type", columnList = "event_type,created_at")
})
public class WebUserLifecycleEvent {

    /** Primary key, auto-generated identity. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user this event belongs to. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Type of lifecycle event. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private WebUserLifecycleEventType eventType;

    /** Type of the operator who triggered the event (SYSTEM, ADMIN, USER). */
    @Column(name = "operator_type", length = 16)
    private String operatorType;

    /** Identifier of the operator. */
    @Column(name = "operator_id", length = 64)
    private String operatorId;

    /** Arbitrary JSON metadata associated with the event. */
    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    /** Timestamp when the event was recorded. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** JPA required no-arg constructor. */
    protected WebUserLifecycleEvent() {
    }

    /**
     * Construct a new lifecycle event record.
     */
    public WebUserLifecycleEvent(Long userId, WebUserLifecycleEventType eventType, String operatorType,
            String operatorId, String metadataJson) {
        this.userId = userId;
        this.eventType = eventType;
        this.operatorType = operatorType;
        this.operatorId = operatorId;
        this.metadataJson = metadataJson;
        this.createdAt = HighDate.mockInstant();
    }
}
