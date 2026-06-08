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
@Table(name = "tb_user_lifecycle_event", indexes = {
        @Index(name = "idx_tb_user_event_user", columnList = "user_id"),
        @Index(name = "idx_tb_user_event_type", columnList = "event_type,created_at")
})
public class WebUserLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private WebUserLifecycleEventType eventType;

    @Column(name = "operator_type", length = 16)
    private String operatorType;

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebUserLifecycleEvent() {
    }

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
