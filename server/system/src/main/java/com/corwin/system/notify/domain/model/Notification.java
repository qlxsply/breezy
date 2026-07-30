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
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain entity representing a notification message.
 * <p>Stores notification content, metadata (type, priority, route), and read status
 * for a specific user.</p>
 *
 * @author Corwin 2026/3/30
 */
@Getter
@Entity
@Table(name = "sys_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 16)
    private UserType userType;

    @Column(nullable = false)
    private String title;

    private String content;

    @Column(name = "msg_type", nullable = false, length = 20)
    private String msgType;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(length = 500)
    private String route;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    protected Notification() {
    }

    /**
     * Creates a new unread notification for the given user.
     *
     * @param userId   the target user ID
     * @param userType the target user type
     * @param title    the notification title
     * @param content  the notification content (optional)
     * @param msgType  the message type identifier
     * @param priority the priority level
     * @param route    the front-end route for navigation
     */
    public Notification(Long userId, UserType userType, String title, String content, String msgType, String priority,
            String route) {
        this.userId = userId;
        this.userType = userType;
        this.title = title;
        this.content = content;
        this.msgType = msgType;
        this.priority = priority;
        this.route = route;
        this.isRead = false;
        this.createdAt = HighDate.mockInstant();
    }

    /**
     * Marks this notification as read. No-op if already read.
     */
    public void markAsRead() {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        this.readAt = HighDate.mockInstant();
    }

}
