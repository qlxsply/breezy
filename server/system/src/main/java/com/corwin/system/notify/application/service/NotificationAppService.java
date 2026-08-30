package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.notify.application.view.NotificationPullView;
import com.corwin.system.notify.application.view.NotificationView;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import com.corwin.system.notify.domain.repo.NotificationPageQuery;
import com.corwin.system.notify.domain.repo.NotificationRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for notification management.
 *
 * <p>Handles querying, pulling, and marking notifications as read.
 *
 * @author Corwin 2026/3/30
 */
@Service
@RequiredArgsConstructor
public class NotificationAppService {

  private final NotificationRepository notificationRepository;
  private final MessageDeliveryRepository messageDeliveryRepository;

  /**
   * Lists unread notifications for the given user, limited to the specified count.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param limit the maximum number of results
   * @return the list of unread notification views
   */
  public List<NotificationView> listUnread(Long userId, UserType userType, int limit) {
    return notificationRepository
        .findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType)
        .stream()
        .limit(limit)
        .map(this::toRes)
        .toList();
  }

  /**
   * Paginates notifications for the given user, optionally filtering by status.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param status the filter status ("all" or "unread")
   * @param spec the page specification
   * @return the paginated notification view data
   */
  public PageData<NotificationView> page(
      Long userId, UserType userType, String status, PageSpec spec) {
    NotificationPageQuery query =
        new NotificationPageQuery(userId, userType, isUnreadStatus(status));
    PageData<Notification> page =
        notificationRepository.pageByQuery(query, PageSpecSorts.apply(spec));
    List<NotificationView> items = page.elements().stream().map(this::toRes).toList();
    return new PageData<>(
        page.pageNo(),
        page.pageSize(),
        page.numberOfElements(),
        page.totalPages(),
        page.totalElements(),
        items);
  }

  /**
   * Returns the count of unread notifications for the given user.
   *
   * @param userId the user ID
   * @param userType the user type
   * @return the unread count
   */
  public long getUnreadCount(Long userId, UserType userType) {
    return notificationRepository.countByUserIdAndUserTypeAndIsReadFalse(userId, userType);
  }

  /**
   * Pulls unread notifications created after the specified timestamp.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param afterMillis the earliest creation time (epoch millis) to include
   * @param limit the maximum number of results (clamped to 1-200)
   * @return the pull result with items and last pull timestamp
   */
  public NotificationPullView pullUnread(
      Long userId, UserType userType, long afterMillis, int limit) {
    int safeLimit = Math.clamp(limit, 1, 200);
    Instant after = afterMillis > 0 ? Instant.ofEpochMilli(afterMillis) : Instant.EPOCH;
    List<Notification> list =
        notificationRepository
            .findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                userId, userType, after)
            .stream()
            .limit(safeLimit)
            .toList();
    long lastPullAt = resolveLastPullAt(afterMillis, list);
    List<NotificationView> items = list.stream().map(this::toRes).toList();
    return new NotificationPullView(items, lastPullAt);
  }

  /**
   * Marks a single notification as read and acknowledges its deliveries.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param id the notification ID
   */
  @Transactional
  public void markRead(Long userId, UserType userType, Long id) {
    notificationRepository
        .findByIdAndUserIdAndUserType(id, userId, userType)
        .ifPresent(
            n -> {
              n.markAsRead();
              notificationRepository.save(n);
              ackNotificationDeliveries(userId, userType, n);
            });
  }

  /**
   * Marks all unread notifications as read for the given user and acknowledges their deliveries.
   *
   * @param userId the user ID
   * @param userType the user type
   */
  @Transactional
  public void markAllRead(Long userId, UserType userType) {
    List<Notification> unread =
        notificationRepository.findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(
            userId, userType);
    notificationRepository.markAllReadByUserIdAndUserType(userId, userType);
    for (Notification notification : unread) {
      ackNotificationDeliveries(userId, userType, notification);
    }
  }

  private void ackNotificationDeliveries(
      Long userId, UserType userType, Notification notification) {
    List<MessageDelivery> deliveries =
        messageDeliveryRepository.findByUserIdAndUserTypeAndNotificationId(
            userId, userType, notification.getId());
    for (MessageDelivery delivery : deliveries) {
      if (delivery.getStatus() == MessageDeliveryStatus.ACKED) {
        continue;
      }
      delivery.markAcked();
      messageDeliveryRepository.save(delivery);
    }
  }

  private NotificationView toRes(Notification n) {
    return new NotificationView(
        String.valueOf(n.getId()),
        n.getTitle(),
        n.getContent(),
        n.getMsgType(),
        n.getPriority(),
        n.getRoute(),
        n.getCreatedAt(),
        n.getIsRead());
  }

  private boolean isUnreadStatus(String status) {
    return status != null && status.equalsIgnoreCase("unread");
  }

  private long resolveLastPullAt(long afterMillis, List<Notification> list) {
    long maxCreatedAt = 0;
    for (Notification item : list) {
      Instant createdAt = item.getCreatedAt();
      if (createdAt == null) {
        continue;
      }
      long time = createdAt.toEpochMilli();
      if (time > maxCreatedAt) {
        maxCreatedAt = time;
      }
    }
    if (maxCreatedAt > 0) {
      return maxCreatedAt;
    }
    return afterMillis;
  }
}
