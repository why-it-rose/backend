package com.whyitrose.domain.notification;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_logs",
        indexes = {
                @Index(name = "idx_notification_logs_status", columnList = "notification_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    // 발송용 문구 스냅샷 — {"title": "...", "body": "...", "link": "..."}
    // JSON 타입: Jackson 으로 직렬화/역직렬화
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    // DEFAULT 'PENDING'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    // 실제 발송 시각
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public static NotificationLog create(Notification notification, String payload) {
        NotificationLog log = new NotificationLog();
        log.notification = notification;
        log.payload = payload;
        log.status = Status.PENDING;
        return log;
    }

    // 연관관계 편의 메서드
    public void assignNotification(Notification notification) {
        this.notification = notification;
        notification.getLogs().add(this);
    }

    public void markAsSent() {
        this.status = Status.ACTIVE;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = Status.INACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}