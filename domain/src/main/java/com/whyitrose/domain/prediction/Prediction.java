package com.whyitrose.domain.prediction;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "predictions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_predictions", columnNames = {"user_id", "digest_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_predictions_user", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prediction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digest_id", nullable = false)
    private DailyNewsDigest digest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // UP | DOWN | SIDEWAYS
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10, nullable = false)
    private PredictionDirection direction;

    // 예측 근거
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // 복기용 실제 등락률 — 배치로 채움
    @Column(name = "actual_change_pct", precision = 6, scale = 2)
    private BigDecimal actualChangePct;

    // 복기 확인 시각
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static Prediction create(User user, DailyNewsDigest digest, Stock stock) {
        Prediction prediction = new Prediction();
        prediction.user = user;
        prediction.digest = digest;
        prediction.stock = stock;
        prediction.status = Status.ACTIVE;
        return prediction;
    }

    public void updatePrediction(PredictionDirection direction, String reason) {
        this.direction = direction;
        this.reason = reason;
    }

    // 배치에서 실제 등락률을 채울 때 호출
    public void review(BigDecimal actualChangePct) {
        this.actualChangePct = actualChangePct;
        this.reviewedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}