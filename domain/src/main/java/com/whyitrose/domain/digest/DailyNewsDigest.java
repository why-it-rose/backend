package com.whyitrose.domain.digest;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "daily_news_digest",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_daily_news_digest_date", columnNames = {"digest_date"})
        },
        indexes = {
                @Index(name = "idx_daily_news_digest_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyNewsDigest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 큐레이션 기준일
    @Column(name = "digest_date", nullable = false)
    private LocalDate digestDate;

    // DEFAULT 0
    @Column(name = "total_news_count", nullable = false)
    private int totalNewsCount;

    // DEFAULT 'PENDING'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @OneToMany(mappedBy = "digest", fetch = FetchType.LAZY)
    private List<DailyNewsDigestItem> items = new ArrayList<>();

    public static DailyNewsDigest create(LocalDate digestDate) {
        DailyNewsDigest digest = new DailyNewsDigest();
        digest.digestDate = digestDate;
        digest.totalNewsCount = 0;
        digest.status = Status.PENDING;
        return digest;
    }

    public void activate(int totalNewsCount) {
        this.totalNewsCount = totalNewsCount;
        this.status = Status.ACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}