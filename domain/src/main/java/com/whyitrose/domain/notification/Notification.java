package com.whyitrose.domain.notification;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_read", columnList = "user_id, read_at"),
                @Index(name = "idx_notifications_user_date", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // NEWS 타입일 때만 값 존재, 나머지 타입은 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "digest_id")
    private DailyNewsDigest digest;

    // NEWS | EVENT | REVIEW | SYSTEM
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private NotificationType type;

    // null이면 안읽음
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @OneToMany(mappedBy = "notification", fetch = FetchType.LAZY)
    private List<NotificationLog> logs = new ArrayList<>();

    public static Notification create(User user, NotificationType type, DailyNewsDigest digest) {
        Notification notification = new Notification();
        notification.user = user;
        notification.type = type;
        notification.digest = digest;
        notification.status = Status.ACTIVE;
        return notification;
    }

    public static Notification create(User user, NotificationType type) {
        return create(user, type, null);
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}