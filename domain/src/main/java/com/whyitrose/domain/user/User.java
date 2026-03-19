package com.whyitrose.domain.user;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uq_users_provider", columnNames = {"provider", "provider_uid"})
        },
        indexes = {
                @Index(name = "idx_users_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 소셜 전용이면 null
    @Column(name = "email", length = 255)
    private String email;

    // 소셜 전용이면 null
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private AuthProvider provider;

    // 이메일 가입이면 null
    @Column(name = "provider_uid", length = 255)
    private String providerUid;

    // DEFAULT 1
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    // DEFAULT 0
    @Column(name = "marketing_agreed", nullable = false)
    private boolean marketingAgreed;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static User create(String name, String email, String passwordHash, String nickname,
                               AuthProvider provider, String providerUid) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.provider = provider;
        user.providerUid = providerUid;
        user.pushEnabled = true;
        user.marketingAgreed = false;
        user.status = Status.ACTIVE;
        return user;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}