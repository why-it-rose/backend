package com.whyitrose.domain.notification;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // idx_notifications_user_date 활용
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Status status, Pageable pageable);

    // 안읽은 알림 수
    long countByUserIdAndReadAtIsNullAndStatus(Long userId, Status status);
}