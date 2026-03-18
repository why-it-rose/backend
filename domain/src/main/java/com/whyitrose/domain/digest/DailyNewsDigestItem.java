package com.whyitrose.domain.digest;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.news.News;
import com.whyitrose.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "daily_news_digest_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_digest_items", columnNames = {"digest_id", "news_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_digest_items_stock", columnList = "digest_id, stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyNewsDigestItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digest_id", nullable = false)
    private DailyNewsDigest digest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    // 해당 뉴스의 대표 종목 — 알림센터 종목 필터링 최적화용 비정규화 컬럼
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static DailyNewsDigestItem create(DailyNewsDigest digest, News news, Stock stock) {
        DailyNewsDigestItem item = new DailyNewsDigestItem();
        item.digest = digest;
        item.news = news;
        item.stock = stock;
        item.status = Status.ACTIVE;
        return item;
    }

    // 연관관계 편의 메서드
    public void assignDigest(DailyNewsDigest digest) {
        this.digest = digest;
        digest.getItems().add(this);
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}