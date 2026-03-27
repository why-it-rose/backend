package com.whyitrose.domain.fcm;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "fcm_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_fcm_tokens_token", columnNames = {"token"})
        },
        indexes = {
                @Index(name = "idx_fcm_tokens_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", length = 255, nullable = false)
    private String token;

    public static FcmToken create(User user, String token) {
        FcmToken fcmToken = new FcmToken();
        fcmToken.user = user;
        fcmToken.token = token;
        return fcmToken;
    }

    public void updateUser(User user) {
        this.user = user;
    }
}
