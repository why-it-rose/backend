package com.whyitrose.domain.news;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "news",
        uniqueConstraints = {
                // DDL: UNIQUE KEY uq_news_url (url(255)) — prefix 인덱스는 Flyway로 관리
                @UniqueConstraint(name = "uq_news_url", columnNames = {"url"})
        },
        indexes = {
                @Index(name = "idx_news_published_at", columnList = "published_at"),
                @Index(name = "idx_news_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "source", length = 100, nullable = false)
    private String source;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 원문 링크 — DDL: VARCHAR(1000)
    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // 기사 발행시각
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @OneToMany(mappedBy = "news", fetch = FetchType.LAZY)
    private List<NewsStock> newsStocks = new ArrayList<>();

    public static News create(String source, String title, String content, String url,
                              String thumbnailUrl, LocalDateTime publishedAt) {
        News news = new News();
        news.source = source;
        news.title = title;
        news.content = content;
        news.url = url;
        news.thumbnailUrl = thumbnailUrl;
        news.publishedAt = publishedAt;
        news.status = Status.ACTIVE;
        return news;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}