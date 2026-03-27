package com.whyitrose.domain.user;

import com.whyitrose.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// DB에 저장되는 refresh token의 엔티티
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_tokens_user", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_refresh_tokens_expiry_at", columnList = "expiry_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자당 1개 토큰
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "refresh_token", length = 1000, nullable = false)
    private String refreshToken;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    public static RefreshToken create(User user, String refreshToken, LocalDateTime expiryAt) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.refreshToken = refreshToken;
        token.expiryAt = expiryAt;
        return token;
    }

    public void update(String refreshToken, LocalDateTime expiryAt) {
        this.refreshToken = refreshToken;
        this.expiryAt = expiryAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiryAt.isBefore(now);
    }
}
