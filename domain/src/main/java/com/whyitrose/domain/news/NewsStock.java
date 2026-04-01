package com.whyitrose.domain.news;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "news_stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_news_stocks", columnNames = {"news_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_news_stocks_stock", columnList = "stock_id, news_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsStock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static NewsStock create(News news, Stock stock) {
        NewsStock newsStock = new NewsStock();
        newsStock.news = news;
        newsStock.stock = stock;
        newsStock.status = Status.ACTIVE;
        return newsStock;
    }

    // 연관관계 편의 메서드
    public void assignNews(News news) {
        this.news = news;
        news.getNewsStocks().add(this);
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}