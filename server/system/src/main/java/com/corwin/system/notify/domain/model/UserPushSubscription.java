package com.corwin.system.notify.domain.model;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain entity representing a user's Web Push subscription.
 * <p>Stores browser push subscription details including endpoint, encryption keys,
 * and tracks push delivery status per device.</p>
 *
 * @author Corwin 2026/3/19
 */
@Getter
@Entity
@Table(name = "sys_user_push_subscription",
        indexes = {
                @Index(name = "idx_push_user_active", columnList = "user_type, user_id, active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_push_user_device", columnNames = {"user_type", "user_id", "device_id"})
        })
public class UserPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 16)
    private UserType userType;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(nullable = false, length = 2000)
    private String endpoint;

    @Column(nullable = false, length = 512)
    private String p256dh;

    @Column(name = "auth_secret", nullable = false, length = 512)
    private String authSecret;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_push_at")
    private Instant lastPushAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected UserPushSubscription() {
    }

    /**
     * Creates a new active push subscription for the given user and device.
     *
     * @param userId     the user ID
     * @param userType   the user type
     * @param deviceId   the unique device identifier
     * @param endpoint   the push endpoint URL
     * @param p256dh     the p256dh encryption key
     * @param authSecret the auth secret
     * @return a new {@link UserPushSubscription}
     */
    public static UserPushSubscription register(Long userId, UserType userType, String deviceId, String endpoint,
            String p256dh, String authSecret) {
        UserPushSubscription subscription = new UserPushSubscription();
        Instant now = HighDate.mockInstant();
        subscription.userId = userId;
        subscription.userType = userType;
        subscription.deviceId = deviceId;
        subscription.endpoint = endpoint;
        subscription.p256dh = p256dh;
        subscription.authSecret = authSecret;
        subscription.active = true;
        subscription.createdAt = now;
        subscription.updatedAt = now;
        return subscription;
    }

    /**
     * Refreshes the subscription with updated endpoint and encryption keys.
     *
     * @param endpoint   the new push endpoint URL
     * @param p256dh     the new p256dh key
     * @param authSecret the new auth secret
     */
    public void refresh(String endpoint, String p256dh, String authSecret) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.authSecret = authSecret;
        this.active = true;
        this.updatedAt = HighDate.mockInstant();
    }

    /**
     * Marks this subscription as having a successful push delivery.
     * Resets the error state and updates the last push timestamp.
     */
    public void markPushSuccess() {
        this.active = true;
        this.lastPushAt = HighDate.mockInstant();
        this.lastError = null;
        this.updatedAt = HighDate.mockInstant();
    }

    /**
     * Marks this subscription as having a failed push delivery.
     *
     * @param errorMessage the error description
     */
    public void markPushFailure(String errorMessage) {
        this.lastError = errorMessage;
        this.updatedAt = HighDate.mockInstant();
    }

    /**
     * Deactivates this subscription with a given reason.
     *
     * @param reason the deactivation reason (e.g. "用户主动取消订阅")
     */
    public void deactivate(String reason) {
        this.active = false;
        this.lastError = reason;
        this.updatedAt = HighDate.mockInstant();
    }
}
