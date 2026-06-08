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
import lombok.Getter;

import java.time.Instant;

/**
 * 消息投递记录。
 *
 * @author Corwin 2026/3/19
 */
@Getter
@Entity
@Table(name = "sys_message_delivery", indexes = {
        @Index(name = "idx_msg_delivery_user_status", columnList = "user_type, user_id, delivery_status"),
        @Index(name = "idx_msg_delivery_user_id", columnList = "user_type, user_id, id"),
        @Index(name = "idx_msg_delivery_user_notification", columnList = "user_type, user_id, notification_id")
})
public class MessageDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 16)
    private UserType userType;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "msg_type", nullable = false, length = 64)
    private String msgType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(length = 500)
    private String route;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "panel_auto_open", nullable = false)
    private boolean panelAutoOpen;

    @Column(name = "os_notification_enabled", nullable = false)
    private boolean osNotificationEnabled;

    @Column(name = "biz_type", length = 64)
    private String bizType;

    @Column(name = "biz_id", length = 128)
    private String bizId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 16)
    private MessageDeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "acked_at")
    private Instant ackedAt;

    protected MessageDelivery() {
    }

    public static MessageDelivery pending(Long userId, UserType userType, Long notificationId, String msgType, String title,
            String content,
            String route, String priority, boolean panelAutoOpen, boolean osNotificationEnabled,
            String bizType, String bizId) {
        MessageDelivery delivery = new MessageDelivery();
        delivery.userId = userId;
        delivery.userType = userType;
        delivery.notificationId = notificationId;
        delivery.msgType = msgType;
        delivery.title = title;
        delivery.content = content;
        delivery.route = route;
        delivery.priority = priority;
        delivery.panelAutoOpen = panelAutoOpen;
        delivery.osNotificationEnabled = osNotificationEnabled;
        delivery.bizType = bizType;
        delivery.bizId = bizId;
        delivery.status = MessageDeliveryStatus.PENDING;
        delivery.createdAt = HighDate.mockInstant();
        return delivery;
    }

    public static MessageDelivery pending(Long userId, UserType userType, String msgType, String title, String content,
            String route, String priority, String bizType, String bizId) {
        return pending(userId, userType, null, msgType, title, content, route, priority,
                priority != null && !"LOW".equalsIgnoreCase(priority),
                "HIGH".equalsIgnoreCase(priority),
                bizType, bizId);
    }

    public void markSent() {
        if (status == MessageDeliveryStatus.ACKED) {
            return;
        }
        status = MessageDeliveryStatus.SENT;
        sentAt = HighDate.mockInstant();
    }

    public void markAcked() {
        status = MessageDeliveryStatus.ACKED;
        ackedAt = HighDate.mockInstant();
    }

    public void markFailed() {
        if (status == MessageDeliveryStatus.ACKED) {
            return;
        }
        status = MessageDeliveryStatus.FAILED;
    }
}
