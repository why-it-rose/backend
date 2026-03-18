package com.whyitrose.domain.notification;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // idx_notification_logs_status 활용 — 재시도 대상 조회
    List<NotificationLog> findByNotificationIdAndStatus(Long notificationId, Status status);

    Optional<NotificationLog> findTopByNotificationIdOrderByCreatedAtDesc(Long notificationId);
}