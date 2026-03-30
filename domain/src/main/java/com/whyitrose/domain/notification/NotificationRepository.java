package com.whyitrose.domain.notification;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // idx_notifications_user_date 활용
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Status status, Pageable pageable);

    // 알림 목록 전체 조회 (페이징 없음)
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Status status);

    // 날짜 필터 조회
    List<Notification> findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, Status status, LocalDateTime from);

    boolean existsByDigestId(Long digestId);

    // 안읽은 알림 수
    long countByUserIdAndReadAtIsNullAndStatus(Long userId, Status status);

    // 전체 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL AND n.status = 'ACTIVE'")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}